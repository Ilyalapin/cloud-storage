package com.cloud_storage.integrationTest.minio;

import com.cloud_storage.dto.ObjectReadDto;
import com.cloud_storage.integrationTest.config.MinioServiceTestConfig;
import com.cloud_storage.service.MinioService;
import io.minio.MinioClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.bouncycastle.asn1.dvcs.DVCSObjectIdentifiers;
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

    @Container
    static MinIOContainer minio = new MinIOContainer(DockerImageName.parse("minio/minio:latest"))
            .withEnv("MINIO_ROOT_USER", "minioadmin62")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin62")
            .withCommand("server /data")
            .withExposedPorts(9000);


    @Test
    void createFolder() throws Exception {

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

        ObjectReadDto rootFolder = minioService.createFolder(rootFolderName, pathRootFolder);

        Assertions.assertNotNull(rootFolder);
        Assertions.assertEquals(rootFolder.getName(), rootFolderName);
        Assertions.assertEquals(rootFolder.getPath(), "/" + rootFolderName + "/");

        String name1 = "photo";
        String name2 = "video";
        String path = rootFolder.getPath();

        ObjectReadDto newFolder1 = minioService.createFolder(name1, path);
        ObjectReadDto newFolder2 = minioService.createFolder(name2, path);

        List<ObjectReadDto> objects = minioService.getObjects( rootFolder.getName());

        Assertions.assertNotNull(objects);
        Assertions.assertEquals(2, objects.size());

        Assertions.assertEquals(newFolder1.getName(), objects.get(0).getName());
        Assertions.assertEquals(newFolder2.getName(), objects.get(1).getName());

        Assertions.assertTrue(objects.get(0).isDir());
        Assertions.assertTrue(objects.get(1).isDir());
    }


    @Test
    void deleteObject() throws Exception {
        String rootFolderName = "user-162-files";
        String pathRootFolder = "/";
        minioService.createFolder(rootFolderName, pathRootFolder);

        String name = "test";
        String name1 = "1";
        String name2 = "2";
        String name3 = "3";

        String path = rootFolderName+"/";

        String path1 = path+name+"/";
        String path2 = path1+name2+"/";

        minioService.createFolder(name, path);
        List<ObjectReadDto> objects1 = minioService.getObjects( path);
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
    void deleteObjectMinio() throws Exception {
        String rootFolderName = "user-129-files";
        minioService.createFolder(rootFolderName, "/");

        String name1 = "test.txt";
        String name2 = "test/";

        String path = rootFolderName+"/";

        minioService.createFolder(name1, path);
        minioService.createFolder(name2, path);

        List<ObjectReadDto> objects1 = minioService.getObjects(path);
        Assertions.assertEquals(2, objects1.size());

        minioService.deleteObjectMinio(path+name1+"/");
        minioService.deleteObjectMinio(path+name2);
        List<ObjectReadDto> objects2 = minioService.getObjects(path);
        Assertions.assertEquals(0, objects2.size());
    }

    @Test
    void renameObject() throws Exception {
        String rootFolderName = "user-130-files";
        ObjectReadDto rootFolder = minioService.createFolder(rootFolderName, "/");

        String oldName = "test";
        String newName = "mama";

        String path = rootFolderName+"/";

        minioService.createFolder(oldName, path);

        minioService.renameObject(oldName, newName,path,rootFolder);
        List<ObjectReadDto> objects = minioService.getObjects(path);
        Assertions.assertEquals(1, objects.size());

        Assertions.assertEquals(newName, objects.get(0).getName());

    }
}


