package com.cloud_storage.service;

import com.cloud_storage.common.exception.FileOperationException;
import com.cloud_storage.common.exception.InvalidParameterException;
import com.cloud_storage.common.exception.MinioException;
import com.cloud_storage.common.exception.NotFoundException;
import com.cloud_storage.common.util.FileSizeConverter;
import com.cloud_storage.common.util.PrefixGenerationUtil;
import com.cloud_storage.common.util.ValidationUtil;
import com.cloud_storage.dto.FileUploadDto;
import com.cloud_storage.dto.ObjectDto;
import com.cloud_storage.dto.ObjectReadDto;
import com.cloud_storage.dto.RenameDto;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
                    "0"
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
        List<ObjectReadDto> objects = new ArrayList<>();
        try {
            getItems(path, objects);
        } catch (Exception e) {
            throw new MinioException("Error while receiving the files: ", e);
        }
        return objects;
    }


    private void getItems(String path, List<ObjectReadDto> objects) throws MinioException {
        Iterable<Result<Item>> items = findObjects(path);
        long totalSize = 0;
        try {
            for (Result<Item> result : items) {
                Item item = result.get();
                if (item.isDir()) {
                    List<ObjectReadDto> subObjects = new ArrayList<>();
                    getItems(item.objectName(), subObjects);

                    for (ObjectReadDto subObject : subObjects) {
                        totalSize += FileSizeConverter.convertToBytes(subObject.getSize());
                    }
                    ObjectReadDto directoryObject = new ObjectReadDto(
                            PrefixGenerationUtil.generateFolderName(item.objectName()),
                            true,
                            path,
                            FileSizeConverter.convert(totalSize));
                    objects.add(directoryObject);
                } else {
                    ObjectReadDto object = new ObjectReadDto(
                            PrefixGenerationUtil.generateFolderName(item.objectName()),
                            false,
                            path,
                            FileSizeConverter.convert(item.size()));
                    objects.add(object);
                }
            }
        } catch (Exception e) {
            throw new MinioException("Error while receiving the files: ", e);
        }
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
            try {
                for (Result<Item> result : objects) {
                    Item item = result.get();

                    deleteAllByPath(item.objectName());
                    delete(item.objectName());
                }
            } catch (Exception e) {
                throw new MinioException("Error while receiving the files", e);
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

        if (renameDto.getIsDir().equals(String.valueOf(true))) {
            copyFolder(renameDto, rootFolder.getName());
            deleteObject(renameDto.getPath() + renameDto.getOldName() + "/");
        } else {
            copyFile(renameDto, rootFolder.getName());
            deleteObject(renameDto.getPath() + renameDto.getOldName());
        }
    }


    private void copyFolder(RenameDto renameDto, String path) throws MinioException {
        try {
            List<ObjectReadDto> objects = getObjects(renameDto.getPath() + renameDto.getOldName() + "/");
            log.warn("Objects found: {}", objects);

            if (!objects.iterator().hasNext()) {
                createFolder(renameDto.getNewName(), path);
            } else {
                ObjectReadDto newFolder = createFolder(renameDto.getNewName(), path);

                for (ObjectReadDto object : objects) {
                    RenameDto childDto = new RenameDto(
                            object.getName(),
                            object.getName(),
                            object.getPath(),
                            String.valueOf(object.isDir())
                    );

                    if (object.isDir()) {
                        ObjectReadDto folder = createFolder(object.getName(), newFolder.getPath());
                        copyFolder(childDto, PrefixGenerationUtil.generateNewPathForCopyObject(folder.getPath()));
                    } else {
                        copyFile(childDto, newFolder.getPath());
                    }
                }
            }
        } catch (Exception e) {
            throw new MinioException("Error while copying object", e);
        }
    }


    private void copyFile(RenameDto renameDto, String path) throws MinioException {
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(path + renameDto.getNewName())
                    .source(CopySource.builder()
                            .bucket(BUCKET_NAME)
                            .object(renameDto.getPath() + renameDto.getOldName())
                            .build())
                    .build());
        } catch (Exception e) {
            throw new MinioException("Error while copying object", e);
        }
    }


    public void uploadFile(FileUploadDto fileUploadDto) throws MinioException, IOException {
            List<MultipartFile> files = fileUploadDto.getFiles();
            for (MultipartFile file : files) {
                putObject(
                        fileUploadDto.getPath() + file.getOriginalFilename(),
                        file.getContentType(),
                        file.getInputStream());
            }
    }

    private void putObject(String objectName, String contentType, InputStream inputStream) throws MinioException {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectName)
                            .stream(
                                    inputStream, -1, 3545)
                            .contentType(contentType)
                            .build());
        } catch (RuntimeException e) {
            throw new InvalidParameterException("Error uploading object. To upload files more than 100 Mb, make a paid subscription.");
        }
        catch (Exception e) {
            throw new MinioException("Error in placing an object in storage", e);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*
     * Метод getAndSave предназначен для загрузки объекта из хранилища MinIO и сохранения его на локальной файловой системе.
     */
    public void getAndSave(String path, String objectName) throws MinioException {

        List<ObjectReadDto> objects = getObjects(path + objectName + "/");
        if (objects.isEmpty()) {
            throw new NotFoundException("No files for downloading were found. This folder is empty.");
        }
        try {
            DownloadObjectArgs args = DownloadObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(path)
                    .filename(objectName)
                    .build();
            minioClient.downloadObject(args);
        } catch (Exception e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }


    public ByteArrayResource downloadFile(ObjectDto objectDto) {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(BUCKET_NAME)
                .object(objectDto.getPath() + objectDto.getName())
                .build();
        try (GetObjectResponse object = minioClient.getObject(getObjectArgs)) {
            return new ByteArrayResource(object.readAllBytes());
        } catch (Exception e) {
            throw new FileOperationException("There is an error while downloading the file, try again later");
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
}
