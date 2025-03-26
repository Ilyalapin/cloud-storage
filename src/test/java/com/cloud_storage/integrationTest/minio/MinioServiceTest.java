package com.cloud_storage.integrationTest.minio;

import com.cloud_storage.dto.ObjectRenameDto;
import com.cloud_storage.dto.ObjectUploadDto;
import com.cloud_storage.dto.ObjectReadDto;
import com.cloud_storage.integrationTest.config.MinioServiceTestConfig;
import com.cloud_storage.service.MinioService;
import io.minio.MinioClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

@Testcontainers
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MinioServiceTestConfig.class)
public class MinioServiceTest {

    @Autowired
    private MinioService minioService;

    @Autowired
    MinioClient minioClient;
    private MinIOContainer minio;


    @BeforeEach
    public void setUp() {
        minio = new MinIOContainer(DockerImageName.parse("minio/minio:latest"))
                .withEnv("MINIO_ROOT_USER", "minioadmin62")
                .withEnv("MINIO_ROOT_PASSWORD", "minioadmin62")
                .withCommand("server /data")
                .withExposedPorts(9000);
        minio.start();
    }


    @AfterEach
    public void tearDown() {
        if (minio != null) {
            minio.stop();
        }
    }


    @Test
    void createFolderSuccessfully() throws Exception {
        ObjectReadDto rootFolder = new ObjectReadDto("user-1234-files",true,"/user-1234-files/");
        String folderName = "test";
        String path = "user-1234-files/";
        ObjectReadDto newFolder = minioService.createFolder(folderName, path, rootFolder);

        Assertions.assertNotNull(newFolder);
        Assertions.assertEquals(newFolder.getName(), folderName);
        Assertions.assertEquals(newFolder.getPath(), path + folderName + "/");
    }

    @Test
    void getObjects() throws Exception {
        String rootFolderName = "user-1002-files/";
        String pathRootFolder = "/";

        ObjectReadDto rootFolder = minioService.createRootFolder(rootFolderName, pathRootFolder);

        String name1 = "photo";
        String name2 = "video";

        ObjectReadDto newFolder1 = minioService.createFolder(name1, rootFolder.getPath(),rootFolder);
        ObjectReadDto newFolder2 = minioService.createFolder(name2, rootFolder.getPath(),rootFolder);

        List<ObjectReadDto> objects = minioService.getObjects(rootFolder.getName());

        Assertions.assertNotNull(objects);
        Assertions.assertEquals(2, objects.size());

        Assertions.assertEquals(newFolder1.getName(), objects.get(0).getName());
        Assertions.assertEquals(newFolder2.getName(), objects.get(1).getName());

        Assertions.assertTrue(objects.get(0).isDir());
        Assertions.assertTrue(objects.get(1).isDir());
    }


    @Test
    void deleteObjectSuccessfully() throws Exception {
        ObjectReadDto rootFolder = new ObjectReadDto("user-162-files",true,"/user-162-files/");
        String name = "test";
        String name1 = "1";
        String name2 = "2";
        String name3 = "3";
        String path = "user-162-files/";
        String path1 = path + name + "/";
        String path2 = path1 + name2 + "/";

        minioService.createFolder(name, path, rootFolder);
        List<ObjectReadDto> objects1 = minioService.getObjects(path);
        Assertions.assertEquals(1, objects1.size());

        minioService.createFolder(name1, path1,rootFolder);
        minioService.createFolder(name2, path1,rootFolder);
        List<ObjectReadDto> objects2 = minioService.getObjects(path1);
        Assertions.assertEquals(2, objects2.size());

        minioService.createFolder(name3, path2,rootFolder);

        minioService.deleteObject(path1);
        List<ObjectReadDto> objects3 = minioService.getObjects(path);
        Assertions.assertEquals(0, objects3.size());
    }


    @Test
    void renameObjectSuccessfully() throws Exception {
        String rootFolderName = "user-143-files";
        ObjectReadDto rootFolder = minioService.createRootFolder(rootFolderName, "/");

        String oldName = "test1";
        String newName = "test2";
        String path = rootFolderName + "/";

        ObjectReadDto folder1 = minioService.createFolder(oldName, path, rootFolder);
        ObjectReadDto folder2 = minioService.createFolder("1", folder1.getPath(), rootFolder);
        minioService.createFolder("2", folder2.getPath(), rootFolder);

        ObjectRenameDto renameDto = new ObjectRenameDto(
                oldName,
                newName,
                path,
                "true"
        );
        minioService.renameObject(renameDto, rootFolder);

        List<ObjectReadDto> objects = minioService.getObjects(path);
        Assertions.assertEquals(1, objects.size());
        Assertions.assertEquals(newName, objects.get(0).getName());
    }


    @Test
    void UploadFileSuccessfully() throws Exception {
        String rootFolderName = "user-111-files";
        ObjectReadDto rootFolder = minioService.createRootFolder(rootFolderName, "/");
        String path = "user-111-files/folder/";
        minioService.createFolder("folder","user-111-files/",rootFolder);

        List<MultipartFile> files = new ArrayList<>();

        String contentType = "text/plain";
        byte[] content = "Hello, world!".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);

        MultipartFile mockFile1 = Mockito.mock(MultipartFile.class);
        when(mockFile1.getOriginalFilename()).thenReturn("file1.txt");
        when(mockFile1.getContentType()).thenReturn(contentType);
        when(mockFile1.getInputStream()).thenReturn(inputStream);

        files.add(mockFile1);
        ObjectUploadDto testDto = new ObjectUploadDto(path,files);

        minioService.uploadFile(testDto, rootFolder);
        List<ObjectReadDto> objects = minioService.getObjects(path);

        Assertions.assertEquals(1, objects.size());
        Assertions.assertEquals(mockFile1.getOriginalFilename(), objects.get(0).getName());
        Assertions.assertEquals(objects.get(0).getSize(), "13,00 b");
    }
}


