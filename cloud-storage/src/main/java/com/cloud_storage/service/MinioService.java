package com.cloud_storage.service;

import com.cloud_storage.common.exception.MinioException;
import com.cloud_storage.common.exception.MinioFetchException;
import io.minio.*;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {
    private final MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String bucketName;


    @PostConstruct
    public void init() throws Exception {
        createBucketIfNotExists();
    }


    public void createBucketIfNotExists() throws Exception {
        try {
            boolean isExist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!isExist) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket:{} created successfully", bucketName);
            } else {
                log.info("Bucket:{} already exist", bucketName);
            }
        } catch (RuntimeException e) {
            throw new MinioException("Error to create a bucket",e);
        }
    }


    public void createFolder(String folderName) throws MinioException {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName).object(folderName + "/")
                    .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                    .build());
            log.info("Folder:{} created successfully", folderName);
        } catch (Exception e) {
            throw new MinioException("Error to create a folder",e);
        }
    }

    /**
     * Метод удаляет все файлы в директории, указанной в префиксе.
     */
    public void deleteFolder(String folderName) throws MinioException {
        try {
            Iterable<Result<Item>> objects = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(folderName)
                    .build());

            for (Result<Item> result : objects) {
                Item item = result.get();
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(item.objectName())
                        .build());
                log.info("Deleted object: {}", item.objectName());
            }
            log.info("User folder {} deleted successfully", folderName);
        } catch (Exception e) {
            throw new MinioException("Error to delete a folder",e);
        }
    }

    /**
     * Метод предназначен для удаления одного объекта из хранилища MinIO по заданному пути.
     */
    public void deleteFile(Path source) throws MinioException {
        try {
            RemoveObjectArgs args = RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(source.toString())
                    .build();
            minioClient.removeObject(args);
        } catch (Exception e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Когда использовать:
     * • Если нужно получить только объекты на верхнем уровне в бакете и я не хочу, чтобы результат включал вложенные объекты или папки.
     * • Когда надо ограничить количество возвращаемых объектов для повышения производительности.

    public List<Item> list() {
        ListObjectsArgs args = ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix("")
                .recursive(false)
                .build();
        Iterable<Result<Item>> myObjects = minioClient.listObjects(args);
        return getItems(myObjects);
    }
     */
    /**
     * todo такой же метод,как и выше,но без префикса и рекурсива.
     * Когда использовать:
     * • Если нужно получить полный список всех объектов в бакете, включая все вложенные структуры и подкаталоги.
     * • Когда уверен, что хочу получить все данные без каких-либо ограничений.
     * public List<Item> fullList() {
     * ListObjectsArgs args = ListObjectsArgs.builder()
     * .bucket(bucketName)
     * .build();
     * Iterable<Result<Item>> myObjects = minioClient.listObjects(args);
     * return getItems(myObjects);
     * }
     */
    private List<Item> getItems(Iterable<Result<Item>> myObjects) {
        return StreamSupport
                .stream(myObjects.spliterator(), true)
                .map(itemResult -> {
                    try {
                        return itemResult.get();
                    } catch (Exception e) {
                        throw new MinioFetchException("Error while parsing list of objects", e);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Этот метод показывает содержимое по заданному в префиксе пути, например, List<Item> items = list(Path.of("user-62-files/"));
     * и ничего более. То есть если в заданной директории будет лежать папка фотографии и 2 файла, то этот метод покажет это все и
     * не покажет содержимое папки фотографии
     */
    public List<Item> list(Path path) {
        ListObjectsArgs args = ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(path.toString())
                .recursive(false)
                .build();
        Iterable<Result<Item>> myObjects = minioClient.listObjects(args);
        return getItems(myObjects);
    }

    /**
     Этот метод позволяет получить метаданные объекта, например размер файла или дату последних изменений.
     */
    public StatObjectResponse getMetadata(Path path) throws MinioException {
        try {
            StatObjectArgs args = StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path.toString())
                    .build();
            return minioClient.statObject(args);
        } catch (Exception e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }


    /** Этот метод позволяет получить метаданные группы объектов,например размер файла или дату последних изменений.
     Возможно он мне не пригодится

    public Map<Path, StatObjectResponse> getMetadata(Iterable<Path> paths) {
        return StreamSupport.stream(paths.spliterator(), false)
                .map(path -> {
                    try {
                        StatObjectArgs args = StatObjectArgs.builder()
                                .bucket(configurationProperties.getBucket())
                                .object(path.toString())
                                .build();
                        return new HashMap.SimpleEntry<>(path, minioClient.statObject(args));
                    } catch (Exception e) {
                        throw new MinioFetchException("Error while parsing list of objects", e);
                    }
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
     */


    /**
     * Метод getAndSave предназначен для загрузки объекта из хранилища MinIO и сохранения его на локальной файловой системе.
     */
    public void getAndSave(Path source, String fileName) throws MinioException {
        try {
            DownloadObjectArgs args = DownloadObjectArgs.builder()
                    .bucket(bucketName)
                    .object(source.toString())
                    .filename(fileName)
                    .build();
            minioClient.downloadObject(args);
        } catch (Exception e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Метод предназначен для загрузки файла из локального хранилища в хранилище MinIO.
     * Скорее всего будет использован для загрузки файла
     */
    public void upload(Path source, File file) throws MinioException {
        try {
            UploadObjectArgs args = UploadObjectArgs.builder()
                    .bucket(bucketName)
                    .object(source.toString())
                    .filename(file.getAbsolutePath())
                    .build();
            minioClient.uploadObject(args);
        } catch (Exception e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Метод предназначен для загрузки объекта в хранилище MinIO без указания типа контента загружаемого файла.
     * Скорее всего будет использован для загрузки папок
     */
    public void upload(Path source, InputStream file) throws MinioException {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(source.toString())
                    .stream(file, file.available(), -1)
                    .build();
            minioClient.putObject(args);
        } catch (Exception e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Метод предназначен для загрузки объекта в хранилище MinIO с указанием типа контента загружаемого файла.
     * Скорее всего будет использован для загрузки файлов
     */
    public void upload(Path source, InputStream file, String contentType) throws MinioException {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(source.toString())
                    .stream(file, file.available(), -1)
                    .contentType(contentType)
                    .build();

            minioClient.putObject(args);
        } catch (Exception e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     Этот метод нужен для чтения файла
     */
    public InputStream get(Path path) throws MinioException {
        try {
            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path.toString())
                    .build();
            return minioClient.getObject(args);
        } catch (Exception e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

}
