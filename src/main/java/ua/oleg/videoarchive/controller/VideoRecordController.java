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

    @GetMapping("/new")
    public String newRecord(Model model) {
        VideoRecord record = new VideoRecord();
        record.setRecordDate(LocalDate.now());
        model.addAttribute("record", record);
        return "record-form";
    }

    @PostMapping
    public String save(@ModelAttribute VideoRecord record) {
        service.save(record);
        return "redirect:/records";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable String id, Model model) {
        model.addAttribute("record", service.find(id));
        return "record-form";
    }

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

    @GetMapping("/{id}")
    public String details(@PathVariable String id, Model model, Authentication authentication) {
        model.addAttribute("record", service.find(id));
        if (isAdmin(authentication)) {
            model.addAttribute("workAreas", workAreas.findAll());
        }
        return "record-details";
    }

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

    @PostMapping("/{id}/videos")
    public String upload(@PathVariable String id,
                         @RequestParam("files") MultipartFile[] files,
                         @RequestParam(required = false) String workAreaId,
                         Authentication authentication) throws IOException {
        WorkArea workArea = resolveUploadWorkArea(authentication, workAreaId);
        service.addVideos(id, files, workArea);
        return "redirect:/records/" + id;
    }

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

    /** Только ADMIN, потому что удаление записи также удаляет ее физические видеофайлы. */
    @PostMapping("/{id}/delete")
    public String deleteRecord(@PathVariable String id) throws IOException {
        service.deleteRecord(id);
        return "redirect:/records";
    }

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

    private boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}