package com.cloud_storage.service;

import com.cloud_storage.common.exception.InvalidParameterException;
import com.cloud_storage.common.exception.MinioException;
import com.cloud_storage.common.util.PrefixGenerationUtil;
import com.cloud_storage.common.util.ValidationUtil;
import com.cloud_storage.dto.ObjectReadDto;
import com.cloud_storage.dto.RenameDto;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class MinioService {
    private final MinioClient minioClient;
    private static final String BUCKET_NAME = "user-files";

    @PostConstruct
    private void init() {
        try {
            createBucketIfNotExists();
        } catch (Exception e) {
            log.error("Error in initialization MinioService: {}", e.getMessage());
        }
    }


    public void createBucketIfNotExists() throws Exception {
        try {
            boolean isExist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build());
            if (!isExist) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());
                log.info("Bucket:{} created successfully", BUCKET_NAME);
            } else {
                log.info("Bucket:{} already exist", BUCKET_NAME);
            }
        } catch (RuntimeException e) {
            throw new MinioException("Error to create a bucket", e);
        }
    }


    public ObjectReadDto createRootFolder(String folderName, String path) throws MinioException {
        try {
            log.trace("Creating folder in bucket: {}, folderName: {}", BUCKET_NAME, folderName);
            if (!isExist(folderName)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(folderName + "/")
                        .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                        .build());
                log.info("Folder:{} created successfully", folderName);
            }
            return new ObjectReadDto(
                    folderName,
                    true,
                    path + folderName
            );
        } catch (Exception e) {
            throw new MinioException("Error to create a folder", e);
        }
    }


    public ObjectReadDto createFolder(String folderName, String path) throws MinioException {
        try {
            ValidationUtil.validate(folderName);
        } catch (RuntimeException e) {
            throw new InvalidParameterException(e.getMessage());
        }
        try {
            log.info("Creating folder with name: {}", folderName);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(path + folderName + "/")
                    .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                    .build());
            log.trace("Folder:{} created successfully", folderName);
            return new ObjectReadDto(
                    folderName,
                    true,
                    path + folderName + "/",
                    getSize(path + folderName + "/")
            );
        } catch (Exception e) {
            throw new MinioException("Error to create a folder", e);
        }
    }


    private Iterable<Result<Item>> findObjects(String path) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(BUCKET_NAME)
                        .startAfter(path)
                        .prefix(path)
                        .build());
    }


    public List<ObjectReadDto> getObjects(String path) throws MinioException {
        Iterable<Result<Item>> items = findObjects(path);

        List<ObjectReadDto> result = new ArrayList<>();
        for (Result<Item> item : items) {
            try {
                ObjectReadDto object = new ObjectReadDto(
                        PrefixGenerationUtil.generateFolderNameForView(item.get().objectName()),
                        item.get().isDir(),
                        path,
                        item.get().size());
                result.add(object);
            } catch (Exception e) {
                throw new MinioException("There is an error while receiving the files, try again later", e);
            }
        }
        return result;
    }


    private boolean isExist(String folderName) throws MinioException {
        try {
            log.trace("Checking if folder exists in bucket: {}, folderName: {}", BUCKET_NAME, folderName);
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(folderName + "/")
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            if (e.getMessage().contains("Object does not exist")) {
                log.trace("Folder does not exist. Proceeding to create it.");
                return false;
            } else {
                throw new MinioException("Error while checking folder existence", e);
            }
        } catch (Exception e) {
            throw new MinioException("Unexpected error while checking folder existence", e);
        }
    }


    public void deleteObject(String path) throws MinioException {
        Iterable<Result<Item>> objects = getAllByPath(path);
        if (!objects.iterator().hasNext()) {
            delete(path);
        } else {
            for (Result<Item> item : objects) {
                try {
                    deleteAllByPath(item.get().objectName());
                    delete(item.get().objectName());
                } catch (Exception e) {
                    throw new MinioException("Error while receiving the files", e);
                }
            }
            delete(path);
        }
    }


    private void deleteAllByPath(String path) throws MinioException {
        try {
            Iterable<Result<Item>> objects = getAllByPath(path);

            for (Result<Item> result : objects) {
                Item item = result.get();

                if (!item.isDir()) {
                    delete(item.objectName());
                    log.info("Deleted object: {}", item.objectName());
                } else {
                    deleteAllByPath(item.objectName());
                }
            }
            delete(path);
            log.info("Deleted folder: {}", path);
        } catch (Exception e) {
            throw new MinioException("Error while deleting folder: ", e);
        }
    }


    private void delete(String fullName) throws MinioException {
        try {
            if (!fullName.endsWith("/")) {
                fullName += "/";
            }
            RemoveObjectArgs args = RemoveObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(fullName)
                    .build();
            minioClient.removeObject(args);
            log.info("Folder:{} deleted successfully", fullName);
        } catch (Exception e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }


    private Iterable<Result<Item>> getAllByPath(String path) throws MinioException {
        try {
            return minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(BUCKET_NAME)
                    .prefix(path)
                    .startAfter(path)
                    .build());
        } catch (Exception e) {
            throw new MinioException("Error while listing objects in folder: " + path, e);
        }
    }


    public void renameObject(RenameDto renameDto, ObjectReadDto rootFolder) throws MinioException {
        try {
            ValidationUtil.validate(renameDto.getNewName());
        } catch (RuntimeException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        renameDto.setPath(PrefixGenerationUtil.generateIfPathIsEmpty(renameDto.getPath(), rootFolder));
        copy(renameDto,rootFolder.getName());
        deleteObject(renameDto.getPath() + renameDto.getOldName() + "/");
    }
    private void copy(RenameDto renameDto,String path) throws MinioException {
        try {
            List<ObjectReadDto> objects = getObjects(renameDto.getPath()+renameDto.getOldName()+"/");
            log.warn("Objects found: {}", objects);

            if (!objects.iterator().hasNext()) {
                createFolder(renameDto.getNewName(), path);
            }
            else {

                    ObjectReadDto newFolder = createFolder(renameDto.getNewName(), path);

                for (ObjectReadDto object : objects) {
                    RenameDto childDto = new RenameDto(
                            object.getName(),
                            object.getName(),
                            object.getPath(),
                            String.valueOf(object.isDir())
                    );
                    ObjectReadDto folder = createFolder(object.getName(),newFolder.getPath());
                    copy(childDto, PrefixGenerationUtil.generateNewPathForCopyObject(folder.getPath()));
                }
            }

        } catch (Exception e) {
            throw new MinioException("Error while copying object", e);
        }
    }


    private void copyObject(RenameDto renameDto) throws MinioException {
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(renameDto.getNewName())
                    .source(CopySource.builder()
                            .bucket(BUCKET_NAME)
                            .object(renameDto.getPath() + renameDto.getOldName() + "/")
                            .build())
                    .build());
        } catch (Exception e) {
            throw new MinioException("Error while copying object", e);
        }
    }

    public ObjectReadDto createFile(String newName, String path) throws MinioException {
        try {
            ValidationUtil.validate(newName);
        } catch (RuntimeException e) {
            throw new InvalidParameterException(e.getMessage());
        }
        try {
            log.info("Creating file with name: {}", newName);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(path + newName)
                    .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                    .build());
            log.trace("File:{} created successfully", newName);
            return new ObjectReadDto(
                    newName,
                    false,
                    path + newName,
                    getSize(path + newName)
            );
        } catch (Exception e) {
            throw new MinioException("Error to create a file", e);
        }
    }


    public Long getSize(String path) throws MinioException {
        StatObjectResponse metadata = getMetadata(path);
        return metadata.size();
    }


    private StatObjectResponse getMetadata(String path) throws MinioException {
        try {
            StatObjectArgs args = StatObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(path)
                    .build();
            return minioClient.statObject(args);
        } catch (Exception e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /* Этот метод позволяет получить метаданные группы объектов,например размер файла или дату последних изменений.
     Возможно он мне не пригодится
     */
//     public Map<Path, StatObjectResponse> getMetadata(Iterable<Path> paths) {
//     return StreamSupport.stream(paths.spliterator(), false)
//     .map(path -> {
//     try {
//     StatObjectArgs args = StatObjectArgs.builder()
//     .bucket(configurationProperties.getBucket())
//     .object(path.toString())
//     .build();
//     return new HashMap.SimpleEntry<>(path, minioClient.statObject(args));
//     } catch (Exception e) {
//     throw new MinioFetchException("Error while parsing list of objects", e);
//     }
//     })
//     .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//     }


    /*
     * Метод getAndSave предназначен для загрузки объекта из хранилища MinIO и сохранения его на локальной файловой системе.
     */
    public void getAndSave(Path source, String fileName) throws MinioException {
        try {
            DownloadObjectArgs args = DownloadObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(source.toString())
                    .filename(fileName)
                    .build();
            minioClient.downloadObject(args);
        } catch (Exception e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

    /*
     * Метод предназначен для загрузки файла из локального хранилища в хранилище MinIO.
     * Скорее всего будет использован для загрузки файла
     */
    public void upload(Path source, File file) throws MinioException {
        try {
            UploadObjectArgs args = UploadObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(source.toString())
                    .filename(file.getAbsolutePath())
                    .build();
            minioClient.uploadObject(args);
        } catch (Exception e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

    /*
     * Метод предназначен для загрузки объекта в хранилище MinIO без указания типа контента загружаемого файла.
     * Скорее всего будет использован для загрузки папок
     */
    public void upload(Path source, InputStream file) throws MinioException {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(source.toString())
                    .stream(file, file.available(), -1)
                    .build();
            minioClient.putObject(args);
        } catch (Exception e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

    /*
     * Метод предназначен для загрузки объекта в хранилище MinIO с указанием типа контента загружаемого файла.
     * Скорее всего будет использован для загрузки файлов
     */
    public void upload(Path source, InputStream file, String contentType) throws MinioException {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(source.toString())
                    .stream(file, file.available(), -1)
                    .contentType(contentType)
                    .build();

            minioClient.putObject(args);
        } catch (Exception e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

    /*
     * Этот метод нужен для чтения файла
     */
    public InputStream get(Path path) throws MinioException {
        try {
            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(path.toString())
                    .build();
            return minioClient.getObject(args);
        } catch (Exception e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

}
