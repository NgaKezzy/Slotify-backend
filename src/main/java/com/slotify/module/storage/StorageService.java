package com.slotify.module.storage;

import com.slotify.config.AppProperties;
import com.slotify.module.salon.dto.ImageUploadResponse;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Image uploads via pre-signed S3 URLs (works with AWS S3 and MinIO).
 *
 * <p>The API never proxies file bytes: clients ask for a pre-signed PUT URL, upload directly to the
 * bucket and then save the returned public URL on the entity. Keys are {@code
 * <folder>/<yyyy>/<uuid>.<ext>}.
 */
@Slf4j
@Service
public class StorageService {

  private final S3Presigner presigner;
  private final AppProperties.Storage storage;
  private final Clock clock;

  public StorageService(AppProperties properties, Clock clock) {
    this.storage = properties.storage();
    this.clock = clock;
    this.presigner = buildPresigner(storage);
  }

  /** Creates a pre-signed PUT URL for a new image object. */
  public ImageUploadResponse presignImageUpload(
      String folder, String fileName, String contentType) {
    String key = buildKey(folder, fileName);
    PutObjectRequest put =
        PutObjectRequest.builder()
            .bucket(storage.bucket())
            .key(key)
            .contentType(contentType)
            .build();
    PresignedPutObjectRequest presigned =
        presigner.presignPutObject(
            PutObjectPresignRequest.builder()
                .signatureDuration(storage.presignTtl())
                .putObjectRequest(put)
                .build());
    String publicUrl = storage.publicBaseUrl().replaceAll("/$", "") + "/" + key;
    return new ImageUploadResponse(presigned.url().toString(), publicUrl, key);
  }

  private String buildKey(String folder, String fileName) {
    String extension = "jpg";
    int dot = fileName.lastIndexOf('.');
    if (dot >= 0 && dot < fileName.length() - 1) {
      extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
    int year = Instant.now(clock).atZone(java.time.ZoneOffset.UTC).getYear();
    return folder + "/" + year + "/" + UUID.randomUUID() + "." + extension;
  }

  private static S3Presigner buildPresigner(AppProperties.Storage storage) {
    boolean hasStaticKeys =
        storage.accessKey() != null
            && !storage.accessKey().isBlank()
            && storage.secretKey() != null
            && !storage.secretKey().isBlank();
    AwsCredentialsProvider credentials =
        hasStaticKeys
            ? StaticCredentialsProvider.create(
                AwsBasicCredentials.create(storage.accessKey(), storage.secretKey()))
            : DefaultCredentialsProvider.builder().build();

    S3Presigner.Builder builder =
        S3Presigner.builder()
            .region(Region.of(storage.region()))
            .credentialsProvider(credentials)
            // Path-style addressing is required by MinIO and harmless on AWS.
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
    if (storage.endpoint() != null && !storage.endpoint().isBlank()) {
      builder.endpointOverride(URI.create(storage.endpoint()));
    }
    return builder.build();
  }
}
