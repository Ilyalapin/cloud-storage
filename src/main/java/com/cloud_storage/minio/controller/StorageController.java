package com.cloud_storage.minio.controller;

import com.cloud_storage.common.exception.MinioException;
import com.cloud_storage.common.util.PrefixGenerationUtil;
import com.cloud_storage.common.util.ValidationUtil;
import com.cloud_storage.minio.dto.*;
import com.cloud_storage.minio.service.MinioService;
import com.cloud_storage.user.service.UserPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/storage")
@RequiredArgsConstructor
@Slf4j
public class StorageController {
    private final MinioService minioService;

    @GetMapping("/guest-page")
    public String showGuestPage() {
        return "home";
    }


    @GetMapping
    public String showContentsOfFolder(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                       @RequestParam(value = "path", required = false, defaultValue = "") String path,
                                       Model model,
                                       HttpSession session) throws Exception {
        ObjectReadDto rootFolder = minioService.createRootFolder("user-" + userPrincipal.getId() + "-files/", "/");
        int idFromPath = PrefixGenerationUtil.getUserIdFromPath(path,rootFolder);
        ValidationUtil.validate(userPrincipal.getId(),idFromPath);

        List<ObjectReadDto> objects = minioService.getObjects(PrefixGenerationUtil.generateIfPathIsEmpty(path, rootFolder));

        model.addAttribute("objects", objects);
        model.addAttribute("searchDto", new SearchDto());
        model.addAttribute("userInfo", userPrincipal.getRole() + ": " + userPrincipal.getUsername());
        model.addAttribute("objectCreateDto", new ObjectDto("", PrefixGenerationUtil.generateIfPathIsEmpty(path, rootFolder)));
        model.addAttribute("folderDeleteDto", new ObjectDeleteDto());
        model.addAttribute("renameDto", new ObjectRenameDto());
        model.addAttribute("path", PrefixGenerationUtil.generatePathBackForBreadCrumbs(path));
        model.addAttribute("breadCrumbs", PrefixGenerationUtil.generatePathForBreadCrumbs(path, rootFolder));
        model.addAttribute("objectUploadDto", new ObjectUploadDto());
        session.setAttribute("rootFolder", rootFolder);
        return "storage";
    }


    @PostMapping
    public String createFolder(@ModelAttribute("objectCreateDto") ObjectDto objectDto,
                               HttpSession session) throws Exception {

        log.info("Creating new folder, folderName: {}, path: {}", objectDto.getName(), objectDto.getPath());
        ObjectReadDto newFolder = minioService.createFolder(objectDto.getName(), objectDto.getPath(), getRootFolder(session));
        log.info("Folder created: {}", newFolder);
        return "redirect:/storage?path=" + objectDto.getPath();
    }


    @DeleteMapping("/delete")
    public String deleteFolder(@ModelAttribute("objectDeleteDto") ObjectDeleteDto objectDeleteDto) throws MinioException {
        log.info("Attempting to delete folder: {}", objectDeleteDto.getObjectName());
        minioService.deleteObject(objectDeleteDto.getPath() + objectDeleteDto.getObjectName());
        log.info("Folder with name:{} deleted successfully", objectDeleteDto.getObjectName());
        return "redirect:/storage?path=" + objectDeleteDto.getPath();
    }


    @PatchMapping("/rename")
    public String rename(@ModelAttribute("renameDto") ObjectRenameDto renameDto, HttpSession session) throws MinioException {
        log.info("Received renameDto: {}", renameDto);
        minioService.renameObject(renameDto, getRootFolder(session));
        return "redirect:/storage?path=" + renameDto.getPath();
    }


    @PostMapping("/uploadFile")
    public String uploadFile(@ModelAttribute("objectUploadDto") ObjectUploadDto fileUpload, HttpSession session) {
        minioService.uploadFile(fileUpload, getRootFolder(session));
        return "redirect:/storage?path=" + fileUpload.getPath();
    }


    @PostMapping("/uploadFolder")
    public String uploadFolder(@ModelAttribute("objectUploadDto") ObjectUploadDto folderUpload, HttpSession session) {
        minioService.uploadFolder(folderUpload, getRootFolder(session));
        return "redirect:/storage?path=" + folderUpload.getPath();
    }


    @GetMapping("/downloadFile")
    public ResponseEntity<ByteArrayResource> downloadFile(@ModelAttribute("objectDto") ObjectDto objectDto, HttpSession session) {
        log.info("Received objectDto: {}", objectDto);
        ByteArrayResource file = minioService.downloadFile(objectDto, getRootFolder(session));

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + objectDto.getName())
                .body(file);
    }


    @GetMapping("/search")
    public String showSearchResults(@ModelAttribute("searchName") String searchName,
                                    @ModelAttribute("searchDto") SearchDto searchDto,
                                    HttpSession session,
                                    Model model,
                                    @AuthenticationPrincipal UserPrincipal userPrincipal) throws MinioException {
        ObjectReadDto rootFolder = getRootFolder(session);

        List<SearchDto> searchResults = minioService.findByName(searchDto, getRootFolder(session));

        model.addAttribute("objects", searchResults);
        model.addAttribute("userInfo", userPrincipal.getRole() + ": " + userPrincipal.getUsername());
        model.addAttribute("objectCreateDto", new ObjectDto());
        model.addAttribute("folderDeleteDto", new ObjectDeleteDto());
        model.addAttribute("renameDto", new ObjectRenameDto());
        model.addAttribute("path", PrefixGenerationUtil.generatePathBackForBreadCrumbs(""));
        model.addAttribute("breadCrumbs", PrefixGenerationUtil.generatePathForBreadCrumbs("", rootFolder));
        model.addAttribute("objectUploadDto", new ObjectUploadDto());
        return "storage";
    }


    @GetMapping("/downloadFolder")
    public void downloadFolder(@ModelAttribute("objectDto") ObjectDto objectDto,
                               HttpSession session,
                               HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Content-Disposition", "attachment; filename=" + objectDto.getName() + ".zip");
        minioService.downloadFolder(objectDto, response.getOutputStream(), getRootFolder(session));
    }


    private ObjectReadDto getRootFolder(HttpSession session) {
        return (ObjectReadDto) session.getAttribute("rootFolder");
    }
}
