package ua.oleg.videoarchive.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ua.oleg.videoarchive.model.VideoFile;
import ua.oleg.videoarchive.service.VideoRecordService;
import ua.oleg.videoarchive.service.VideoStorageService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/stream")
public class VideoStreamController {
    private static final Pattern RANGE = Pattern.compile("bytes=(\\d*)-(\\d*)");
    private final VideoRecordService records;
    private final VideoStorageService storage;

    /**
     * Створює контролер потоковой выдачи видео і отримує сервіс записей і сервіс зберігання файлів.
     * @param records параметр методу
     * @param storage параметр методу
     */
    public VideoStreamController(VideoRecordService records, VideoStorageService storage) {
        this.records = records; this.storage = storage;
    }

    /**
     * Обробляє HTTP Range-запити браузера до великого відеофайлу. Обчислює початковий і кінцевий байт, открывает поток з нужной позиции і повертає HTTP 206 Partial Content з Content-Range і Accept-Ranges.
     * @param recordId ідентифікатор запису VideoRecord
     * @param videoId ідентифікатор VideoFile всередині запису
     * @param range параметр методу
     * @return результат роботи методу (ResponseEntity<InputStreamResource>)
     * @throws IOException якщо операція введення-виведення не може быть выполнена
     */
    @GetMapping("/{recordId}/{videoId}")
    public ResponseEntity<InputStreamResource> stream(@PathVariable String recordId,
            @PathVariable String videoId,
            @RequestHeader(value="Range", required=false) String range) throws IOException {
        VideoFile video = records.findVideo(recordId, videoId);
        Path path = storage.resolve(video);
        if (!Files.isRegularFile(path)) return ResponseEntity.notFound().build();
        long total = Files.size(path);
        MediaType type = MediaTypeFactory.getMediaType(video.getOriginalName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        if (range == null || range.isBlank()) {
            return ResponseEntity.ok().contentType(type).contentLength(total)
                    .header(HttpHeaders.ACCEPT_RANGES,"bytes")
                    .body(new InputStreamResource(Files.newInputStream(path)));
        }
        Matcher m = RANGE.matcher(range.trim());
        if (!m.matches()) return unsatisfiable(total);
        long start, end;
        try {
            if (m.group(1).isEmpty()) {
                long suffix = Long.parseLong(m.group(2));
                if (suffix <= 0) return unsatisfiable(total);
                start = Math.max(0, total-suffix); end=total-1;
            } else {
                start=Long.parseLong(m.group(1));
                end=m.group(2).isEmpty()?total-1:Long.parseLong(m.group(2));
                if (start<0 || start>=total || end<start) return unsatisfiable(total);
                end=Math.min(end,total-1);
            }
        } catch (NumberFormatException e) { return unsatisfiable(total); }
        long len=end-start+1;
        InputStream in=Files.newInputStream(path); skipFully(in,start);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).contentType(type).contentLength(len)
                .header(HttpHeaders.ACCEPT_RANGES,"bytes")
                .header(HttpHeaders.CONTENT_RANGE,"bytes "+start+"-"+end+"/"+total)
                .body(new InputStreamResource(new BoundedInputStream(in,len)));
    }
    /**
     * Формує HTTP 416, якщо запрошенный диапазон байтов невозможен для файлу.
     * @param total повний розмір файлу у байтах
     * @return результат роботи методу (ResponseEntity<InputStreamResource>)
     */
    private ResponseEntity<InputStreamResource> unsatisfiable(long total) {
        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                .header(HttpHeaders.CONTENT_RANGE,"bytes */"+total).build();
    }
    /**
     * Пропускает во входном потоке указанное кількість байтов, гарантируя, что переход к начальной позиции диапазона выполнен полностью.
     * @param in входной поток
     * @param count кількість байтов, яке необходимо пропустить
     * @throws IOException якщо операція введення-виведення не може быть выполнена
     */
    private void skipFully(InputStream in,long count)throws IOException{
        long left=count; while(left>0){long n=in.skip(left); if(n>0)left-=n; else if(in.read()==-1)throw new IOException("Cannot seek"); else left--;}
    }
    /**
     * Внутренний поток, який ограничивает чтение заданным количеством байтов.
     */
    private static class BoundedInputStream extends InputStream {
        private final InputStream delegate;
        private long remaining;

        /**
         * Створює ограниченный поток поверх вихідного InputStream.
         * @param d исходный поток
         * @param r максимальное кількість байтов, разрешённых к чтению
         */
        BoundedInputStream(InputStream d, long r) {
            delegate = d;
            remaining = r;
        }

        /**
         * Читает один байт, уменьшая оставшийся лимит після успешного чтения.
         * @return прочитанный байт або -1 під час достижении лимита/конца вихідного потока
         * @throws IOException якщо исходный поток не може выполнить чтение
         */
        public int read() throws IOException {
            if (remaining == 0) return -1;
            int v = delegate.read();
            if (v >= 0) remaining--;
            return v;
        }

        /**
         * Читает блок байтов, но не больше поточного лимита Range.
         * @param b буфер назначения
         * @param o начальное зміщення у буфере
         * @param l максимально допустимое кількість байтов
         * @return кількість прочитанных байтов або -1, якщо лимит исчерпан
         * @throws IOException якщо исходный поток не може выполнить чтение
         */
        public int read(byte[] b, int o, int l) throws IOException {
            if (remaining == 0) return -1;
            int max = (int) Math.min(l, remaining);
            int n = delegate.read(b, o, max);
            if (n > 0) remaining -= n;
            return n;
        }

        /**
         * Закрывает исходный поток.
         * @throws IOException якщо исходный поток не може быть закрыт
         */
        public void close() throws IOException {
            delegate.close();
        }
    }
}
