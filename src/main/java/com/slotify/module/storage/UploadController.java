package com.slotify.module.storage;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.salon.dto.ImageUploadResponse;
import com.slotify.module.salon.dto.PresignRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Pre-signed image upload URLs for the admin panel and the apps. */
@Tag(name = "Uploads", description = "Direct-to-storage image uploads")
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

  private final StorageService storageService;

  @Operation(summary = "Get a pre-signed URL to upload an image")
  @PostMapping("/presign")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<ImageUploadResponse> presign(@Valid @RequestBody PresignRequest request) {
    return ApiResponse.ok(
        storageService.presignImageUpload(
            request.folder(), request.fileName(), request.contentType()));
  }
}
