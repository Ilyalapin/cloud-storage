package com.cloud_storage.minio.service;

import com.cloud_storage.common.exception.*;
import com.cloud_storage.common.util.FileSizeConverter;
import com.cloud_storage.common.util.MapingUtil;
import com.cloud_storage.common.util.PrefixGenerationUtil;
import com.cloud_storage.common.util.ValidationUtil;
import com.cloud_storage.minio.dto.*;
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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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


    public ObjectReadDto createFolder(String folderName, String path, ObjectReadDto rootFolder) throws Exception {
        ValidationUtil.validate(folderName);
        try {
            path = PrefixGenerationUtil.generateIfPathIsEmpty(path, rootFolder);

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


    public List<SearchDto> findByName(SearchDto searchDto, ObjectReadDto rootFolder) throws MinioException {
        String searchName = searchDto.getName();

        List<ObjectReadDto> allObjects = getObjects(rootFolder.getName());
        List<SearchDto> foundObjects = new ArrayList<>();
        try {
            for (ObjectReadDto object : allObjects) {
                if (object.getName().toLowerCase().contains(searchName.toLowerCase())) {
                    SearchDto searchObject = MapingUtil.convertToSearchDto(object);
                    searchObject.setSearchResult(true);
                    foundObjects.add(searchObject);
                }
                if (object.isDir()) {
                    findChildObjects(searchName, object, foundObjects);
                }
            }
        } catch (Exception e) {
            throw new FileNotFoundException("Search by name: " + searchName + " did not give results.");
        }
        return foundObjects;
    }


    private void findChildObjects(String searchName, ObjectReadDto parentObject, List<SearchDto> foundObjects) throws MinioException {
        List<ObjectReadDto> childObjects = getObjects(parentObject.getPath() + parentObject.getName() + "/");
        for (ObjectReadDto childObject : childObjects) {
            if (childObject.getName().toLowerCase().contains(searchName.toLowerCase())) {
                SearchDto foundChildObject = MapingUtil.convertToSearchDto(childObject);
                foundChildObject.setSearchResult(true);
                foundObjects.add(foundChildObject);
            }
            if (childObject.isDir()) {
                findChildObjects(searchName, childObject, foundObjects);
            }
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
        try {
            if (!objects.iterator().hasNext()) {
                delete(path);
            } else {
                for (Result<Item> itemResult : objects) {
                    Item item = itemResult.get();
                    deleteAllByPath(item.objectName());
                    delete(item.objectName());
                }
                delete(path);
            }
        } catch (Exception e) {
            throw new MinioException("Error when removing: ", e);
        }
    }


    private void deleteAllByPath(String path) throws MinioException {
        Iterable<Result<Item>> objects = getAllByPath(path);
        objects.forEach(itemResult -> {
            try {
                Item item = itemResult.get();

                if (!item.isDir()) {
                    delete(item.objectName());
                    log.info("Deleted object: {}", item.objectName());
                } else {
                    deleteAllByPath(item.objectName());
                }
            } catch (Exception e) {
                throw new RuntimeException("Error while deleting folder: ", e);
            }
        });
        delete(path);
        log.info("Deleted folder: {}", path);
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


    public void renameObject(ObjectRenameDto renameDto, ObjectReadDto rootFolder) throws MinioException {
        ValidationUtil.validate(renameDto);
        renameDto.setPath(PrefixGenerationUtil.generateIfPathIsEmpty(renameDto.getPath(), rootFolder));

        if (renameDto.getIsDir().equals(String.valueOf(true))) {
            copyFolder(renameDto, renameDto.getPath(), rootFolder);
            deleteObject(renameDto.getPath() + renameDto.getOldName() + "/");
        } else {
            copyFile(renameDto, renameDto.getPath());
            deleteObject(renameDto.getPath() + renameDto.getOldName());
        }
    }


    private void copyFolder(ObjectRenameDto renameDto, String path, ObjectReadDto rootFolder) throws MinioException {
        try {
            List<ObjectReadDto> objects = getObjects(renameDto.getPath() + renameDto.getOldName() + "/");
            log.warn("Objects found: {}", objects);

            if (!objects.iterator().hasNext()) {
                createFolder(renameDto.getNewName(), path, rootFolder);
            } else {
                ObjectReadDto newFolder = createFolder(renameDto.getNewName(), path, rootFolder);

                for (ObjectReadDto object : objects) {
                    ObjectRenameDto childDto = new ObjectRenameDto(
                            object.getName(),
                            object.getName(),
                            object.getPath(),
                            String.valueOf(object.isDir())
                    );

                    if (object.isDir()) {
                        ObjectReadDto folder = createFolder(object.getName(), newFolder.getPath(), rootFolder);
                        copyFolder(childDto, PrefixGenerationUtil.generateNewPathForCopyObject(folder.getPath()), rootFolder);
                    } else {
                        copyFile(childDto, newFolder.getPath());
                    }
                }
            }
        } catch (Exception e) {
            throw new MinioException("Error while copying object", e);
        }
    }


    private void copyFile(ObjectRenameDto renameDto, String path) throws MinioException {
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


    public void uploadFile(ObjectUploadDto fileUploadDto, ObjectReadDto rootFolder) throws FileOperationException {
        try {
            fileUploadDto.setPath(PrefixGenerationUtil.generateIfPathIsEmpty(fileUploadDto.getPath(), rootFolder));
            List<MultipartFile> files = fileUploadDto.getFiles();
            for (MultipartFile file : files) {
                putObject(
                        fileUploadDto.getPath() + file.getOriginalFilename(),
                        file.getContentType(),
                        file.getInputStream());
            }
        } catch (Exception e) {
            throw new FileOperationException("Error while uploading file");
        }
    }

    private void putObject(String objectName, String contentType, InputStream inputStream) throws MinioException {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectName)
                            .stream(
                                    inputStream, -1, 104857600)
                            .contentType(contentType)
                            .build());
        } catch (RuntimeException e) {
            throw new UserInvalidParameterException("Error uploading object. To upload files more than 100 Mb, make a paid subscription.");
        } catch (Exception e) {
            throw new MinioException("Error in placing an object in storage", e);
        }
    }


    public void uploadFolder(ObjectUploadDto objectUploadDto, ObjectReadDto rootFolder) {
        objectUploadDto.setPath(PrefixGenerationUtil.generateIfPathIsEmpty(objectUploadDto.getPath(), rootFolder));

        List<MultipartFile> files = objectUploadDto.getFiles();
        try {
            List<SnowballObject> objects = getSnowBallObjects(files, objectUploadDto.getPath());

            minioClient.uploadSnowballObjects(UploadSnowballObjectsArgs.builder()
                    .bucket(BUCKET_NAME)
                    .objects(objects)
                    .build());
        } catch (Exception e) {
            throw new FolderOperationException("There is an error while uploading the folder, try again later");
        }
    }


    private List<SnowballObject> getSnowBallObjects(List<MultipartFile> files, String path) throws IOException {
        List<SnowballObject> objects = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
                continue;
            }
            SnowballObject snowballObject = new SnowballObject(
                    path + file.getOriginalFilename(),
                    file.getInputStream(),
                    file.getSize(),
                    null
            );
            objects.add(snowballObject);
        }
        return objects;
    }


    public void downloadFolder(ObjectDto objectDto, OutputStream target, ObjectReadDto rootFolder) throws Exception {

        objectDto.setPath(PrefixGenerationUtil.generateIfPathIsEmpty(objectDto.getPath(), rootFolder));
        List<ObjectReadDto> objects = getObjects(objectDto.getPath() + objectDto.getName() + "/");

        try (ZipOutputStream zipOut = new ZipOutputStream(target)) {
            for (ObjectReadDto object : objects) {
                if (object.isDir()) {
                    addDirectoryToZip(zipOut, object);
                } else {
                    addFileToZip(zipOut, object.getName(), object);
                }
            }
        } catch (Exception e) {
            throw new FolderOperationException("There is an error while downloading the folder, try again later");
        }
    }


    private void addDirectoryToZip(ZipOutputStream zipOut, ObjectReadDto childObjectDto) throws Exception {
        List<ObjectReadDto> objects = getObjects(childObjectDto.getPath() + childObjectDto.getName() + "/");

        objects.forEach(object -> {
            try {
                if (object.isDir()) {
                    addDirectoryToZip(zipOut, object);
                } else {
                    String path = PrefixGenerationUtil.
                            removePrefixForZipDirectory(childObjectDto.getPath()) + childObjectDto.getName() + "/" + object.getName();
                    addFileToZip(zipOut, path, object);
                }
            } catch (Exception e) {
                throw new RuntimeException("There is an error while adding to zip", e);
            }
        });
    }


    private void addFileToZip(ZipOutputStream zipOut, String path, ObjectReadDto object) throws Exception {
        zipOut.putNextEntry(new ZipEntry(path));

        InputStream fileInputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(BUCKET_NAME)
                .object(object.getPath() + object.getName())
                .build());

        byte[] buffer = new byte[1024];
        int length;
        while ((length = fileInputStream.read(buffer)) >= 0) {
            zipOut.write(buffer, 0, length);
        }
        zipOut.closeEntry();
        fileInputStream.close();
    }


    public ByteArrayResource downloadFile(ObjectDto objectDto, ObjectReadDto rootFolder) {
        objectDto.setPath(PrefixGenerationUtil.generateIfPathIsEmpty(objectDto.getPath(), rootFolder));
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
}
