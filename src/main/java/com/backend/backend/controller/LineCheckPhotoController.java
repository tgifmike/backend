package com.backend.backend.controller;

import com.backend.backend.entity.LineCheckItemEntity;
import com.backend.backend.entity.LineCheckPhotoEntity;
import com.backend.backend.enums.PhotoType;
import com.backend.backend.enums.ResponseType;
import com.backend.backend.repositories.LineCheckItemRepository;
import com.backend.backend.repositories.LineCheckPhotoRepository;
import com.backend.backend.repositories.LineCheckCriterionResponseRepository;
import com.backend.backend.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.backend.backend.dto.PhotoResponse;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/line-check-items")
public class LineCheckPhotoController {

    private final S3Service s3Service;
    private final LineCheckItemRepository lineCheckItemRepository;
    private final LineCheckPhotoRepository lineCheckPhotoRepository;
    private final LineCheckCriterionResponseRepository criterionResponseRepository;

    public LineCheckPhotoController(
            S3Service s3Service,
            LineCheckItemRepository lineCheckItemRepository,
            LineCheckPhotoRepository lineCheckPhotoRepository,
            LineCheckCriterionResponseRepository criterionResponseRepository
    ) {
        this.s3Service = s3Service;
        this.lineCheckItemRepository = lineCheckItemRepository;
        this.lineCheckPhotoRepository = lineCheckPhotoRepository;
        this.criterionResponseRepository = criterionResponseRepository;
    }

    @PostMapping("/{lineCheckItemId}/photos")
    public ResponseEntity<?> uploadPhoto(
            @PathVariable UUID lineCheckItemId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("photoType") PhotoType photoType,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "criterionResponseId", required = false)
            UUID criterionResponseId
    ) throws IOException {

        // =====================================================
        // 1. Find line check item
        // =====================================================


        System.out.println("================================");
        System.out.println("UPLOAD PHOTO DEBUG");
        System.out.println("file object = " + file);
        System.out.println("file name = " + file.getOriginalFilename());
        System.out.println("file size = " + file.getSize());
        System.out.println("file empty = " + file.isEmpty());
        System.out.println("content type = " + file.getContentType());
        System.out.println("photo type = " + photoType);
        System.out.println("================================");

        LineCheckItemEntity lineCheckItem =
                lineCheckItemRepository.findById(lineCheckItemId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Line check item not found: "
                                                + lineCheckItemId
                                )
                        );

        if (photoType == PhotoType.CRITERION && criterionResponseId == null) {
            return ResponseEntity.badRequest()
                    .body("criterionResponseId is required for a criterion photo");
        }

        if (criterionResponseId != null && photoType != PhotoType.CRITERION) {
            return ResponseEntity.badRequest()
                    .body("photoType must be CRITERION when criterionResponseId is provided");
        }

        if (criterionResponseId != null) {
            var criterionResponse = criterionResponseRepository
                    .findById(criterionResponseId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Criterion response not found"
                    ));

            if (!lineCheckItemId.equals(criterionResponse.getLineCheckItem().getId())) {
                return ResponseEntity.badRequest()
                        .body("Criterion response does not belong to the line check item");
            }

            if (criterionResponse.getResponseType() != ResponseType.PHOTO) {
                return ResponseEntity.badRequest()
                        .body("Criterion response is not configured for a photo");
            }
        }

        // =====================================================
        // 2. Validate file
        // =====================================================

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("File is empty");
        }

        if (file.getContentType() == null ||
                !file.getContentType().startsWith("image/")) {

            return ResponseEntity.badRequest()
                    .body("File must be an image");
        }

        // =====================================================
        // 3. Upload image to S3
        // =====================================================

        String s3Key =
                s3Service.uploadImage(
                        file.getBytes(),
                        file.getContentType(),
                        file.getOriginalFilename(),
                        lineCheckItemId,
                        photoType.name()
                );

        // =====================================================
        // 4. Create database record
        // =====================================================

        LineCheckPhotoEntity photo =
                new LineCheckPhotoEntity();

        photo.setLineCheckItem(lineCheckItem);

        photo.setS3Key(s3Key);

        photo.setOriginalFileName(
                file.getOriginalFilename()
        );

        photo.setContentType(
                file.getContentType()
        );

        photo.setPhotoType(photoType);

        photo.setNotes(notes);
        photo.setCriterionResponseId(criterionResponseId);

        // =====================================================
        // 5. Save database record
        // =====================================================

        LineCheckPhotoEntity saved =
                lineCheckPhotoRepository.save(photo);

        // =====================================================
        // 6. Return database record
        // =====================================================

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{lineCheckItemId}/photos")
    public ResponseEntity<?> getPhotos(
            @PathVariable UUID lineCheckItemId
    ) {

        // =====================================================
        // 1. Make sure line check item exists
        // =====================================================

        lineCheckItemRepository.findById(lineCheckItemId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Line check item not found: "
                                        + lineCheckItemId
                        )
                );

        // =====================================================
        // 2. Get photos from database
        // =====================================================

        var photos =
                lineCheckPhotoRepository
                        .findByLineCheckItemId(lineCheckItemId);

        // =====================================================
        // 3. Generate temporary S3 URLs
        // =====================================================

        var response =
                photos.stream()
                        .map(photo -> {

                            String url =
                                    s3Service.generatePresignedUrl(
                                            photo.getS3Key()
                                    );

                            return new PhotoResponse(
                                    photo.getId(),
                                    photo.getS3Key(),
                                    photo.getOriginalFileName(),
                                    photo.getContentType(),
                                    photo.getPhotoType(),
                                    photo.getNotes(),
                                    photo.getCriterionResponseId(),
                                    photo.getCreatedAt(),
                                    photo.getCreatedBy(),
                                    url
                            );
                        })
                        .toList();

        // =====================================================
        // 4. Return photos
        // =====================================================

        return ResponseEntity.ok(response);
    }
}
