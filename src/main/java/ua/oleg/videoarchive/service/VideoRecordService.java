package ua.oleg.videoarchive.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ua.oleg.videoarchive.model.VideoFile;
import ua.oleg.videoarchive.model.VideoRecord;
import ua.oleg.videoarchive.model.WorkArea;
import ua.oleg.videoarchive.repository.VideoRecordRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

@Service
public class VideoRecordService {
    private final VideoRecordRepository repository;
    private final VideoStorageService storage;

    /**
     * Створює сервіс записів відеоархіву та отримує MongoDB-репозиторій записів і сервіс фізичного зберігання.
     * @param repository репозиторій MongoDB
     * @param storage параметр методу
     */
    public VideoRecordService(VideoRecordRepository repository, VideoStorageService storage) {
        this.repository = repository;
        this.storage = storage;
    }

    /**
     * Виконує пошук за полями provider, company, title і date у пам’яті після завантаження записів із MongoDB; текстові поля порівнюються без урахування регістру, результат сортується за датою.
     * @param provider фільтр за відправником файлів
     * @param company фільтр за назвою фірми
     * @param title фільтр за назвою запису
     * @param date фільтр за датою
     * @return результат роботи методу (List<VideoRecord>)
     */
    public List<VideoRecord> search(String provider, String company, String title, LocalDate date) {
        Stream<VideoRecord> stream = repository.findAll().stream();

        if (provider != null && !provider.isBlank())
            stream = stream.filter(r -> contains(r.getFileProvider(), provider));
        if (company != null && !company.isBlank())
            stream = stream.filter(r -> contains(r.getCompanyName(), company));
        if (title != null && !title.isBlank())
            stream = stream.filter(r -> contains(r.getTitle(), title));
        if (date != null)
            stream = stream.filter(r -> date.equals(r.getRecordDate()));

        return stream.sorted(Comparator.comparing(VideoRecord::getRecordDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * Перевіряє, чи містить значення шуканий підрядок без урахування регістру.
     * @param value значення, яке необходимо обработать
     * @param search параметр методу
     * @return результат роботи методу (boolean)
     */
    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT)
                .contains(search.toLowerCase(Locale.ROOT));
    }

    /**
     * Підготовляє список videos, якщо він відсутній, і зберігає VideoRecord через MongoDB repository.
     * @param record об’єкт VideoRecord для сохранения
     * @return результат роботи методу (VideoRecord)
     */
    public VideoRecord save(VideoRecord record) {
        if (record.getVideos() == null) record.setVideos(new ArrayList<>());
        return repository.save(record);
    }

    /**
     * Знаходить VideoRecord за ID або викидає NoSuchElementException, якщо запис відсутній.
     * @param id ідентифікатор об’єкта
     * @return результат роботи методу (VideoRecord)
     */
    public VideoRecord find(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Record not found"));
    }

    /**
     * Для кожного непорожнього MultipartFile зберігає фізичний файл через VideoStorageService, додає отриманий VideoFile до запису та зберігає оновлений запис у MongoDB.
     * @param recordId ідентифікатор запису VideoRecord
     * @param files масив завантажуваних відеофайлів
     * @param workArea робоча зона, у каталог яку записується файл
     * @throws IOException якщо операція введення-виведення не може быть выполнена
     */
    public void addVideos(String recordId, MultipartFile[] files, WorkArea workArea) throws IOException {
        VideoRecord record = find(recordId);
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    record.getVideos().add(storage.store(file, workArea));
                }
            }
        }
        repository.save(record);
    }

    /**
     * Знаходить конкретний VideoFile всередині зазначеної VideoRecord за videoId.
     * @param recordId ідентифікатор запису VideoRecord
     * @param videoId ідентифікатор VideoFile всередині запису
     * @return результат роботи методу (VideoFile)
     */
    public VideoFile findVideo(String recordId, String videoId) {
        return find(recordId).getVideos().stream()
                .filter(v -> videoId.equals(v.getId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Video not found"));
    }

    /**
     * Видаляє фізичний файл через VideoStorageService, видаляє його метадані зі списку запису та зберігає запис у MongoDB.
     * @param recordId ідентифікатор запису VideoRecord
     * @param videoId ідентифікатор VideoFile всередині запису
     * @throws IOException якщо операція введення-виведення не може быть выполнена
     */
    public void deleteVideo(String recordId, String videoId) throws IOException {
        VideoRecord record = find(recordId);
        VideoFile video = findVideo(recordId, videoId);
        storage.delete(video);
        record.getVideos().removeIf(v -> videoId.equals(v.getId()));
        repository.save(record);
    }

    /**
     * Видаляє всі фізичні відеофайли запису, після чого видаляє сам документ VideoRecord із MongoDB.
     * @param recordId ідентифікатор запису VideoRecord
     * @throws IOException якщо операція введення-виведення не може быть выполнена
     */
    public void deleteRecord(String recordId) throws IOException {
        VideoRecord record = find(recordId);
        for (VideoFile video : record.getVideos()) {
            storage.delete(video);
        }
        repository.delete(record);
    }
}
