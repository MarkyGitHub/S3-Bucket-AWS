package com.contargo.s3sync.s3;

/**
 * REST endpoints to browse and download objects stored in S3.
 */
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/s3")
public class S3Controller {

    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @GetMapping("/files")
    /**
     * Lists all objects available in the configured S3 bucket.
     */
    public List<S3ObjectMetadata> listFiles() {
        return s3Service.listAllObjects();
    }

    @GetMapping(value = "/files", params = "key")
    /**
     * Streams the content of the given object key as a file download.
     *
     * @param key the object key within the bucket
     */
    public ResponseEntity<byte[]> downloadFile(@RequestParam("key") String key) {
        // key is already decoded here
        S3ObjectContent object = s3Service.getObject(key);

        MediaType mediaType = resolveMediaType(object.contentType());

        String filename = Paths.get(key).getFileName().toString();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(Objects.requireNonNull(mediaType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(object.data());
    }

    /**
     * Parses the content type or falls back to octet-stream when
     * invalid/unknown.
     */
    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
