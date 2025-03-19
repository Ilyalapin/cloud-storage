package com.cloud_storage.controller;

import com.cloud_storage.common.UserPrincipal;
import com.cloud_storage.common.exception.InvalidParameterException;
import com.cloud_storage.common.exception.MinioException;
import com.cloud_storage.common.util.PrefixGenerationUtil;
import com.cloud_storage.dto.*;
import com.cloud_storage.service.MinioService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

        List<ObjectReadDto> objects = minioService.getObjects(PrefixGenerationUtil.generateIfPathIsEmpty(path, rootFolder));

        model.addAttribute("userInfo", userPrincipal.getRole() + ": " + userPrincipal.getUsername());
        model.addAttribute("objectCreateDto", new ObjectDto("", PrefixGenerationUtil.generateIfPathIsEmpty(path, rootFolder)));
        model.addAttribute("objects", objects);
        model.addAttribute("folderDeleteDto", new ObjectDeleteDto());
        model.addAttribute("renameDto", new RenameDto());
        model.addAttribute("path", PrefixGenerationUtil.generatePathBackForBreadCrumbs(path));
        model.addAttribute("breadCrumbs", PrefixGenerationUtil.generatePathForBreadCrumbs(path, rootFolder));
        model.addAttribute("fileUploadDto", new FileUploadDto(PrefixGenerationUtil.generateIfPathIsEmpty(path, rootFolder), null));
        session.setAttribute("rootFolder", rootFolder);

        return "storage";
    }


    @PostMapping
    public String createFolder(@RequestParam(required = false) String path,
                               @ModelAttribute("objectCreateDto") ObjectDto objectDto,
                               RedirectAttributes redirectAttributes) {
        try {
            log.info("Creating new folder, folderName: {}, path: {}", objectDto.getName(), path);

            objectDto.setPath(path);
            ObjectReadDto newFolder = minioService.createFolder(objectDto.getName(), objectDto.getPath());

            log.info("Folder created: {}", newFolder);
        } catch (InvalidParameterException e) {
            log.error(e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error creating folder. " + e.getMessage());
        } catch (MinioException e) {
            log.error("Error creating folder: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/storage?path=" + objectDto.getPath();
    }


    @DeleteMapping("/delete")
    public String deleteFolder(@ModelAttribute("objectDeleteDto") ObjectDeleteDto objectDeleteDto,
                               RedirectAttributes redirectAttributes) {
        try {
            log.info("Attempting to delete folder: {}", objectDeleteDto.getObjectName());

            minioService.deleteObject(objectDeleteDto.getPath() + objectDeleteDto.getObjectName());

            log.info("Folder with name:{} deleted successfully", objectDeleteDto.getObjectName());
        } catch (Exception e) {
            log.error("Error deleting folder: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/storage?path=" + objectDeleteDto.getPath();
    }


    @PatchMapping("/rename")
    public String rename(@ModelAttribute("renameDto") RenameDto renameDto,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        log.info("Received renameDto: {}", renameDto);
        try {
            ObjectReadDto rootFolder = (ObjectReadDto) session.getAttribute("rootFolder");

            minioService.renameObject(renameDto, rootFolder);
        } catch (InvalidParameterException e) {
            log.error(e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error renaming folder. " + e.getMessage());
        } catch (MinioException e) {
            log.error("Error renaming folder: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/storage?path=" + renameDto.getPath();
    }


    @PostMapping("/uploadFile")
    public String uploadFile(@ModelAttribute("fileUploadDto") FileUploadDto fileUploadDto,
                             @RequestParam(required = false) String path,
                             RedirectAttributes redirectAttributes) {
        fileUploadDto.setPath(path);
        try {
            minioService.uploadFile(fileUploadDto);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/storage?path=" + fileUploadDto.getPath();
    }


    @GetMapping("/downloadFile")
    public ResponseEntity<ByteArrayResource> readFile(@ModelAttribute("objectDto") ObjectDto objectDto,
                                                      RedirectAttributes redirectAttributes){
        log.info("Received objectDto: {}", objectDto);
        ByteArrayResource file = null;
        try {
            file  = minioService.downloadFile(objectDto);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + objectDto.getName())
                .body(file);
    }
}
