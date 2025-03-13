package com.cloud_storage.integrationTest.minio;

import com.cloud_storage.dto.ObjectReadDto;
import com.cloud_storage.dto.RenameDto;
import com.cloud_storage.integrationTest.config.MinioServiceTestConfig;
import com.cloud_storage.service.MinioService;
import io.minio.MinioClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

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
        String folderName = "test";
        String path = "user-1234-files/";
        ObjectReadDto newFolder = minioService.createFolder(folderName, path);

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

        ObjectReadDto newFolder1 = minioService.createFolder(name1, rootFolder.getPath());
        ObjectReadDto newFolder2 = minioService.createFolder(name2, rootFolder.getPath());

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
        String name = "test";
        String name1 = "1";
        String name2 = "2";
        String name3 = "3";
        String path = "user-162-files/";
        String path1 = path + name + "/";
        String path2 = path1 + name2 + "/";

        minioService.createFolder(name, path);
        List<ObjectReadDto> objects1 = minioService.getObjects(path);
        Assertions.assertEquals(1, objects1.size());

        minioService.createFolder(name1, path1);
        minioService.createFolder(name2, path1);
        List<ObjectReadDto> objects2 = minioService.getObjects(path1);
        Assertions.assertEquals(2, objects2.size());

        minioService.createFolder(name3, path2);

        minioService.deleteObject(path1);
        List<ObjectReadDto> objects3 = minioService.getObjects(path);
        Assertions.assertEquals(0, objects3.size());
    }


    @Test
    void getSize() throws Exception {
        String folderName = "test";
        String path = "user-1020-files/";

        ObjectReadDto newFolder = minioService.createFolder(folderName, path);

        Assertions.assertEquals(0, newFolder.getSize());
        minioService.deleteObject(path);
    }


    @Test
    void renameObjectSuccessfully() throws Exception {
        String rootFolderName = "user-143-files";
        ObjectReadDto rootFolder = minioService.createRootFolder(rootFolderName, "/");

        String oldName = "test1";
        String newName = "test2";
        String path = rootFolderName + "/";

        ObjectReadDto folder1 = minioService.createFolder(oldName, path);
        ObjectReadDto folder2 = minioService.createFolder("1", folder1.getPath());
        minioService.createFolder("2", folder2.getPath());

        RenameDto renameDto = new RenameDto(
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
}


