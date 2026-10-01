package ua.oleg.videoarchive.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ua.oleg.videoarchive.model.VideoFile;
import ua.oleg.videoarchive.model.WorkArea;
import ua.oleg.videoarchive.repository.WorkAreaRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

@Service
public class VideoStorageService {

    /**
     * Старий загальний каталог.
     * Використовується лише для старих VideoFile,
     * які не мають workAreaId і storagePatch.
     */
    private final Path legacyRoot;

    private final WorkAreaRepository workAreas;

    /**
     * Створює сервіс фізичного зберігання відео. Нормалізує legacy-каталог із `video.storage.path`, створює його та отримує WorkAreaRepository для визначення шляхів.
     * @param storagePath параметр методу
     * @param workAreas параметр методу
     * @throws IOException якщо операція введення-виведення не може быть выполнена
     */
    public VideoStorageService(
            @Value("${video.storage.path}") String storagePath,
            WorkAreaRepository workAreas) throws IOException {

        this.legacyRoot = Paths.get(storagePath)
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(legacyRoot);

        this.workAreas = workAreas;
    }

    /**
     * Зберігає видео у patch вибраного WorkArea.
     *.
     * Для звичайного користувача WorkArea визначається
     * контролером за поточним користувачем.
     *.
     * Для адміністратора передається вибрана ним WorkArea.
     */
    public VideoFile store(
            MultipartFile file,
            WorkArea workArea) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Пустий відеофайл");
        }

        if (workArea == null) {
            throw new IllegalArgumentException("Поле «Робоча зона» є обов’язковим.");
        }

        /*
         * Получаем каталог WorkArea и при необходимости
         * создаём его.
         */
        Path root = requireRoot(workArea);

        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null
                        ? "video"
                        : file.getOriginalFilename()
        );

        /*
         * Получаем расширение исходного файла.
         */
        String extension = "";

        int dot = original.lastIndexOf('.');

        if (dot >= 0) {
            extension = original.substring(dot);
        }

        /*
         * Физическое имя файла генерируем сами.
         */
        String stored = UUID.randomUUID() + extension;

        Path destination = safeResolve(root, stored);

        /*
         * Записываем файл.
         */
        try (InputStream in = file.getInputStream()) {
            Files.copy(
                    in,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }

        /*
         * В VideoFile сохраняем:
         *
         * workAreaId  - район, которому принадлежит видео
         * storagePatch - patch, который использовался
         * relativePath - имя файла относительно patch
         *
         * Благодаря storagePatch изменение patch
         * района в будущем не сломает старые видео.
         */
        return new VideoFile(
                UUID.randomUUID().toString(),
                null,
                original,
                stored,
                workArea.getId(),
                root.toString(),
                stored,
                Files.size(destination),
                Instant.now()
        );
    }

    /**
     * Повертає фізичний шлях відео.
     *
     * Нові відео використовують storagePatch,
     * який було збережено у момент завантаження.
     *
     * Тому зміна WorkArea.patch впливає
     * лише на НОВІ відео.
     *
     * Старі відео продовжують використовувати старий patch.
     *
     * Старі VideoFile без workAreaId і storagePatch
     * використовують video.storage.path.
     */
    public Path resolve(VideoFile video) {

        if (video == null) {
            throw new IllegalArgumentException("Video is null");
        }

        if (video.getRelativePath() == null
                || video.getRelativePath().isBlank()) {

            throw new IllegalArgumentException("Video path is empty");
        }

        Path root = legacyRoot;

        /*
         * В первую очередь используем storagePatch.
         *
         * Это важно:
         * если администратор изменил patch района,
         * старое видео должно остаться доступным
         * по старому физическому пути.
         */
        if (video.getStoragePatch() != null
                && !video.getStoragePatch().isBlank()) {

            root = getRootByPatch(video.getStoragePatch());

        } else if (video.getWorkAreaId() != null
                && !video.getWorkAreaId().isBlank()) {

            /*
             * Совместимость с промежуточными данными:
             *
             * workAreaId уже есть,
             * но storagePatch ещё отсутствует.
             *
             * Здесь НЕ вызываем requireRoot(),
             * потому что resolve() не должен создавать каталог
             * и не должен выбрасывать checked IOException.
             */
            WorkArea workArea = workAreas.findById(video.getWorkAreaId())
                    .orElseThrow(() -> new IllegalStateException(
                            "WorkArea not found: "
                                    + video.getWorkAreaId()
                    ));

            root = getRoot(workArea);
        }

        return safeResolve(root, video.getRelativePath());
    }

    /**
     * Видаляє фізичний відеофайл.
     */
    public void delete(VideoFile video) throws IOException {
        Files.deleteIfExists(resolve(video));
    }

    /**
     * Використовується ChunkUploadService.
     *
     * Повертає patch вибраного WorkArea
     * і створює каталог, якщо його ще немає.
     */
    public Path rootFor(WorkArea workArea) throws IOException {
        return requireRoot(workArea);
    }

    /**
     * Перевіряє WorkArea, отримує его patch
     * і створює каталог.
     *
     * Цей метод використовується під час ЗАПИСУ файлу.
     */
    private Path requireRoot(WorkArea workArea) throws IOException {

        Path root = getRoot(workArea);

        Files.createDirectories(root);

        return root;
    }

    /**
     * Отримує шлях із WorkArea.
     *
     * Цей метод НЕ створює каталог.
     *
     * Тому його можна безпечно використовувати
     /**
     * Визначає фізичний шлях VideoFile. Для нових файлів використовує збережений storagePatch, для проміжних даних — workAreaId, а для старих файлів — legacyRoot.
     * @param workArea робоча зона, у каталог яку записується файл
     * @return результат роботи методу (всередині)
     *
     * внутри resolve().
     */
    private Path getRoot(WorkArea workArea) {

        if (workArea == null) {
            throw new IllegalArgumentException("WorkArea is required");
        }

        if (workArea.getId() == null
                || workArea.getId().isBlank()) {

            throw new IllegalArgumentException(
                    "WorkArea id is required"
            );
        }

        if (workArea.getPatch() == null
                || workArea.getPatch().isBlank()) {

            throw new IllegalStateException(
                    "Для района '"
                            + workArea.getName()
                            + "' не задан patch"
            );
        }

        return getRootByPatch(workArea.getPatch());
    }

    /**
     * Перетворює patch на нормалізований абсолютний Path.
     */
    private Path getRootByPatch(String patch) {

        if (patch == null || patch.isBlank()) {
            throw new IllegalArgumentException(
                    "Storage patch is empty"
            );
        }

        try {
            return Paths.get(patch)
                    .toAbsolutePath()
                    .normalize();

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Некорректный patch: " + patch,
                    e
            );
        }
    }

    /**
     * Безпечно формує шлях до файлу.
     *
     * relativePath не може вивести нас
     * за межі root.
     */
    private Path safeResolve(
            Path root,
            String relativePath) {

        Path normalizedRoot = root
                .toAbsolutePath()
                .normalize();

        Path path = normalizedRoot
                .resolve(relativePath)
                .normalize();

        if (!path.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException(
                    "Invalid stored path"
            );
        }

        return path;
    }
}
