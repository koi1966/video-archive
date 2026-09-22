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

    public VideoStreamController(VideoRecordService records, VideoStorageService storage) {
        this.records = records; this.storage = storage;
    }

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
    private ResponseEntity<InputStreamResource> unsatisfiable(long total) {
        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                .header(HttpHeaders.CONTENT_RANGE,"bytes */"+total).build();
    }
    private void skipFully(InputStream in,long count)throws IOException{
        long left=count; while(left>0){long n=in.skip(left); if(n>0)left-=n; else if(in.read()==-1)throw new IOException("Cannot seek"); else left--;}
    }
    private static class BoundedInputStream extends InputStream{
        private final InputStream delegate; private long remaining;
        BoundedInputStream(InputStream d,long r){delegate=d;remaining=r;}
        public int read()throws IOException{if(remaining==0)return -1;int v=delegate.read();if(v>=0)remaining--;return v;}
        public int read(byte[] b,int o,int l)throws IOException{if(remaining==0)return -1;int max=(int)Math.min(l,remaining);int n=delegate.read(b,o,max);if(n>0)remaining-=n;return n;}
        public void close()throws IOException{delegate.close();}
    }
}
