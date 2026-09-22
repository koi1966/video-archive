package ua.oleg.videoarchive.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ua.oleg.videoarchive.model.VideoFile;
import ua.oleg.videoarchive.model.VideoRecord;
import ua.oleg.videoarchive.service.VideoRecordService;
import ua.oleg.videoarchive.service.VideoStorageService;
import ua.oleg.videoarchive.service.ChunkUploadService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

@Controller
@RequestMapping("/records")
public class VideoRecordController {
    private final VideoRecordService service;
    private final VideoStorageService storage;
    private final ChunkUploadService chunkUploadService;

    public VideoRecordController(VideoRecordService service, VideoStorageService storage, ChunkUploadService chunkUploadService) {
        this.service = service;
        this.storage = storage;
        this.chunkUploadService = chunkUploadService;
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
    public String details(@PathVariable String id, Model model) {
        model.addAttribute("record", service.find(id));
        return "record-details";
    }


    @PostMapping("/{id}/videos/chunk")
    @ResponseBody
    public java.util.Map<String, Object> uploadChunk(
            @PathVariable String id,
            @RequestParam String uploadId,
            @RequestParam String fileName,
            @RequestParam long totalSize,
            @RequestParam int chunkNumber,
            @RequestParam int totalChunks,
            @RequestParam("chunk") MultipartFile chunk) throws IOException {
        boolean completed = chunkUploadService.writeChunk(
                id, uploadId, fileName, totalSize, chunkNumber, totalChunks, chunk);
        return java.util.Map.of(
                "completed", completed,
                "chunkNumber", chunkNumber,
                "totalChunks", totalChunks);
    }

    @PostMapping("/{id}/videos")
    public String upload(@PathVariable String id, @RequestParam("files") MultipartFile[] files)
            throws IOException {
        service.addVideos(id, files);
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

    @PostMapping("/{recordId}/videos/{videoId}/delete")
    public String deleteVideo(@PathVariable String recordId, @PathVariable String videoId)
            throws IOException {
        service.deleteVideo(recordId, videoId);
        return "redirect:/records/" + recordId;
    }

    @PostMapping("/{id}/delete")
    public String deleteRecord(@PathVariable String id) throws IOException {
        service.deleteRecord(id);
        return "redirect:/records";
    }
}
