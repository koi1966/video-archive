package ua.oleg.videoarchive.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import ua.oleg.videoarchive.model.AppUser;
import ua.oleg.videoarchive.model.VideoFile;
import ua.oleg.videoarchive.model.VideoRecord;
import ua.oleg.videoarchive.model.WorkArea;
import ua.oleg.videoarchive.repository.AppUserRepository;
import ua.oleg.videoarchive.repository.WorkAreaRepository;
import ua.oleg.videoarchive.service.ChunkUploadService;
import ua.oleg.videoarchive.service.VideoRecordService;
import ua.oleg.videoarchive.service.VideoStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/records")
public class VideoRecordController {
    private final VideoRecordService service;
    private final VideoStorageService storage;
    private final ChunkUploadService chunkUploadService;
    private final AppUserRepository users;
    private final WorkAreaRepository workAreas;

    /**
     * Створює контролер відеоархіву і отримує сервіс роботи з записями, сервіс фізичного зберігання і сервіс chunk-завантаження.
     * @param service параметр методу
     * @param storage параметр методу
     * @param chunkUploadService параметр методу
     * @param users параметр методу
     * @param workAreas параметр методу
     */
    public VideoRecordController(VideoRecordService service,
                                 VideoStorageService storage,
                                 ChunkUploadService chunkUploadService,
                                 AppUserRepository users,
                                 WorkAreaRepository workAreas) {
        this.service = service;
        this.storage = storage;
        this.chunkUploadService = chunkUploadService;
        this.users = users;
        this.workAreas = workAreas;
    }

    /**
     * Обрабатывает поиск записей за відправником файлів, назвою фірми, заголовку і датою. Результаты і введённые фильтры передаются у шаблон `records`.
     * @param provider фільтр за відправником файлів
     * @param company фільтр за назвою фірми
     * @param title фільтр за назвою запису
     * @param date фільтр за датою
     * @param model Model для передачи даних у Thymeleaf-шаблон
     * @return результат роботи методу (String)
     */
    @GetMapping
    public String records(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) LocalDate date,
            Model model) {
        model.addAttribute("records", service.search(provider, company, title, date));
        model.addAttribute("provider", provider);
        model.addAttribute("company", company);
        model.addAttribute("title", title);
        model.addAttribute("date", date);
        return "records";
    }

    /**
     * Виконує операцію `GetMapping` у рамках класу `VideoRecordController`.
     * @param model Model для передачи даних у Thymeleaf-шаблон
     * @return результат роботи методу (@)
     */
    @GetMapping("/new")
    public String newRecord(Model model) {
        VideoRecord record = new VideoRecord();
        record.setRecordDate(LocalDate.now());
        model.addAttribute("record", record);
        return "record-form";
    }

    /**
     * Принимает заполненную VideoRecord із HTML-формы і зберігає её через VideoRecordService.
     * @param record об’єкт VideoRecord для сохранения
     * @return результат роботи методу (String)
     */
    @PostMapping
    public String save(@ModelAttribute VideoRecord record) {
        service.save(record);
        return "redirect:/records";
    }

    /**
     * Знаходить существующую запис за ID і открывает ту же форму у режиме редактирования.
     * @param id ідентифікатор об’єкта
     * @param model Model для передачи даних у Thymeleaf-шаблон
     * @return результат роботи методу (String)
     */
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable String id, Model model) {
        model.addAttribute("record", service.find(id));
        return "record-form";
    }

    /**
     * Обновляет лише поля описания существующей запису і зберігає вже наявний список видео, не удаляя связанные VideoFile.
     * @param id ідентифікатор об’єкта
     * @param form форма з зміненими даними запису
     * @return результат роботи методу (String)
     */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable String id, @ModelAttribute VideoRecord form) {
        VideoRecord existing = service.find(id);

        existing.setFileProvider(form.getFileProvider());
        existing.setRecordDate(form.getRecordDate());
        existing.setCompanyName(form.getCompanyName());
        existing.setTitle(form.getTitle());
        existing.setDescription(form.getDescription());

        // Existing videos are intentionally preserved.
        service.save(existing);
        return "redirect:/records/" + id;
    }

    /**
     * Відкриває картку конкретной запису і передає шаблону дані запису і, під час потреби, список робочих зон для администратора.
     * @param id ідентифікатор об’єкта
     * @param model Model для передачи даних у Thymeleaf-шаблон
     * @param authentication дані поточної автентифікації
     * @return результат роботи методу (String)
     */
    @GetMapping("/{id}")
    public String details(@PathVariable String id, Model model, Authentication authentication) {
        model.addAttribute("record", service.find(id));
        if (isAdmin(authentication)) {
            model.addAttribute("workAreas", workAreas.findAll());
        }
        return "record-details";
    }

    /**
     * Принимает один 8-МБ chunk великого відеофайлу, определяет робочу зону поточного користувача і передає chunk у ChunkUploadService. Повертає JSON з номером частини, общим количеством частин і признаком завершения.
     * @param id ідентифікатор об’єкта
     * @param uploadId унікальний ідентифікатор поточної chunk-завантаження
     * @param fileName исходное ім’я загружаемого файлу
     * @param totalSize повний розмір вихідного файлу у байтах
     * @param chunkNumber номер поточної частини, починаючи з нуля
     * @param totalChunks загальна кількість частин
     * @param chunk поточна частина великого файлу
     * @param workAreaId ідентифікатор робочої зони користувача
     * @param authentication дані поточної автентифікації
     * @return результат роботи методу (Object>)
     * @throws IOException якщо операція введення-виведення не може быть выполнена
     */
    @PostMapping("/{id}/videos/chunk")
    @ResponseBody
    public Map<String, Object> uploadChunk(
            @PathVariable String id,
            @RequestParam String uploadId,
            @RequestParam String fileName,
            @RequestParam long totalSize,
            @RequestParam int chunkNumber,
            @RequestParam int totalChunks,
            @RequestParam("chunk") MultipartFile chunk,
            @RequestParam(required = false) String workAreaId,
            Authentication authentication) throws IOException {

        WorkArea workArea = resolveUploadWorkArea(authentication, workAreaId);

        boolean completed = chunkUploadService.writeChunk(
                id, uploadId, fileName, totalSize, chunkNumber, totalChunks, chunk, workArea);

        return Map.of(
                "completed", completed,
                "chunkNumber", chunkNumber,
                "totalChunks", totalChunks);
    }

    /**
     * Принимает один або несколько обычных MultipartFile, определяет робочу зону і передає файлы у VideoRecordService для фізичного сохранения і добавления метаданных у запис.
     * @param id ідентифікатор об’єкта
     * @param files масив завантажуваних відеофайлів
     * @param workAreaId ідентифікатор робочої зони користувача
     * @param authentication дані поточної автентифікації
     * @return результат роботи методу (String)
     * @throws IOException якщо операція введення-виведення не може быть выполнена
     */
    @PostMapping("/{id}/videos")
    public String upload(@PathVariable String id,
                         @RequestParam("files") MultipartFile[] files,
                         @RequestParam(required = false) String workAreaId,
                         Authentication authentication) throws IOException {
        WorkArea workArea = resolveUploadWorkArea(authentication, workAreaId);
        service.addVideos(id, files, workArea);
        return "redirect:/records/" + id;
    }

    /**
     * Знаходить VideoFile, визначає його фізичний шлях і передає файл браузеру як вкладення для завантаження з исходным именем файлу.
     * @param recordId ідентифікатор запису VideoRecord
     * @param videoId ідентифікатор VideoFile всередині запису
     * @return результат роботи методу (ResponseEntity<InputStreamResource>)
     * @throws IOException якщо операція введення-виведення не може быть выполнена
     */
    @GetMapping("/{recordId}/videos/{videoId}/download")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable String recordId, @PathVariable String videoId) throws IOException {
        VideoFile video = service.findVideo(recordId, videoId);
        Path path = storage.resolve(video);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(video.getOriginalName())
                                .build().toString())
                .contentLength(Files.size(path))
                .body(new InputStreamResource(Files.newInputStream(path)));
    }

    /**
     * Знаходить VideoFile і отдаёт его вміст для перегляду. Визначає MIME-тип за исходному имени файлу.
     * @param recordId ідентифікатор запису VideoRecord
     * @param videoId ідентифікатор VideoFile всередині запису
     * @return результат роботи методу (ResponseEntity<InputStreamResource>)
     * @throws IOException якщо операція введення-виведення не може быть выполнена
     */
    @GetMapping("/{recordId}/videos/{videoId}/play")
    public ResponseEntity<InputStreamResource> play(
            @PathVariable String recordId, @PathVariable String videoId) throws IOException {
        VideoFile video = service.findVideo(recordId, videoId);
        Path path = storage.resolve(video);

        MediaType type = MediaTypeFactory.getMediaType(video.getOriginalName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(type)
                .contentLength(Files.size(path))
                .body(new InputStreamResource(Files.newInputStream(path)));
    }

    /** Только ADMIN: дополнительно защищено SecurityConfig. */
    @PostMapping("/{recordId}/videos/{videoId}/delete")
    public String deleteVideo(@PathVariable String recordId, @PathVariable String videoId)
            throws IOException {
        service.deleteVideo(recordId, videoId);
        return "redirect:/records/" + recordId;
    }

    /** Только ADMIN, потому что видалення запису також видаляє ее физические відеофайли. */
    @PostMapping("/{id}/delete")
    public String deleteRecord(@PathVariable String id) throws IOException {
        service.deleteRecord(id);
        return "redirect:/records";
    }

    /**
     * Визначає робочу зону, у которую разрешено записывать файл. ADMIN може выбрать робочу зону із формы, обычный USER отримує её із собственного профиля. Перевіряє существование зони і наявність patch.
     * @param authentication дані поточної автентифікації
     * @param requestedWorkAreaId ідентифікатор робочої зони, вибраної администратором
     * @return результат роботи методу (WorkArea)
     */
    private WorkArea resolveUploadWorkArea(Authentication authentication, String requestedWorkAreaId) {
        AppUser user = users.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Пользователь не найден"));

        boolean admin = "ADMIN".equalsIgnoreCase(user.getRole());
        String effectiveId = admin ? requestedWorkAreaId : user.getWorkAreaId();

        if (effectiveId == null || effectiveId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    admin
                            ? "Администратор должен выбрать район работы для загрузки видео"
                            : "Для пользователя не назначен район работы");
        }

        WorkArea workArea = workAreas.findById(effectiveId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Район работы не найден"));

        if (workArea.getPatch() == null || workArea.getPatch().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Для района '" + workArea.getName() + "' не задан patch");
        }

        return workArea;
    }

    /**
     * Перевіряє наявність у Authentication роли ROLE_ADMIN.
     * @param authentication дані поточної автентифікації
     * @return результат роботи методу (boolean)
     */
    private boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
