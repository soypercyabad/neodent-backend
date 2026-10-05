package com.neodent.storage;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class R2StorageService implements StorageService {

    @Value("${storage.r2.endpoint}")
    private String endpoint;

    @Value("${storage.r2.access-key}")
    private String accessKey;

    @Value("${storage.r2.secret-key}")
    private String secretKey;

    @Value("${storage.r2.bucket}")
    private String bucket;

    private S3Client s3Client;

    @PostConstruct
    void inicializar() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        S3Configuration configuracionS3 = S3Configuration.builder().pathStyleAccessEnabled(true).chunkedEncodingEnabled(false).build();
        s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(configuracionS3)
                .build();
    }

    @Override
    public void guardar(String clave, byte[] contenido, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(clave).contentType(contentType).contentLength((long) contenido.length).build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(contenido));
        } catch (S3Exception ex) {
            throw new IllegalStateException("No se pudo guardar el archivo en Cloudflare R2.", ex);
        }
    }

    @Override
    public byte[] obtener(String clave) {
        GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(clave).build();

        try {
            return s3Client.getObjectAsBytes(request).asByteArray();
        } catch (NoSuchKeyException ex) {
            throw new IllegalStateException("El archivo solicitado no existe.", ex);

        } catch (S3Exception ex) {
            throw new IllegalStateException("No se pudo obtener el archivo desde Cloudflare R2.", ex);
        }
    }

    @Override
    public void eliminar(String clave) {

        DeleteObjectRequest request = DeleteObjectRequest.builder().bucket(bucket).key(clave).build();

        try {
            s3Client.deleteObject(request);
        } catch (S3Exception ex) {
            throw new IllegalStateException("No se pudo eliminar el archivo de Cloudflare R2.", ex);
        }
    }

    @Override
    public boolean existe(String clave) {

        HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucket).key(clave).build();

        try {
            s3Client.headObject(request);
            return true;
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) { return false; }
            throw new IllegalStateException("No se pudo verificar el archivo en Cloudflare R2.", ex);
        }
    }
}