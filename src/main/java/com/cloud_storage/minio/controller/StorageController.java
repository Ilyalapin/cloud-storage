package com.cloud_storage.minio.controller;

import com.cloud_storage.common.BaseController;
import com.cloud_storage.minio.dto.*;
import com.cloud_storage.minio.service.MinioService;
import com.cloud_storage.user.service.UserPrincipal;
import com.cloud_storage.common.exception.InvalidParameterException;
import com.cloud_storage.common.exception.MinioException;
import com.cloud_storage.common.util.PrefixGenerationUtil;
import com.cloud_storage.dto.minioDto.*;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/storage")
@RequiredArgsConstructor
@Slf4j
public class StorageController extends BaseController {
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

        model.addAttribute("objects", objects);
        model.addAttribute("searchDto", new SearchDto());
        submitToModel(model, userPrincipal, rootFolder);
        session.setAttribute("rootFolder", rootFolder);

        return "storage";
    }


    @PostMapping
    public String createFolder(@ModelAttribute("objectCreateDto") ObjectDto objectDto,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            log.info("Creating new folder, folderName: {}, path: {}", objectDto.getName(), objectDto.getPath());
            ObjectReadDto newFolder = minioService.createFolder(objectDto.getName(), objectDto.getPath(), getRootFolder(session));

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
    public String rename(@ModelAttribute("renameDto") ObjectRenameDto renameDto,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        log.info("Received renameDto: {}", renameDto);
        try {
            minioService.renameObject(renameDto, getRootFolder(session));
        } catch (InvalidParameterException e) {
            log.error(e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error renaming. " + e.getMessage());
        } catch (MinioException e) {
            log.error("Error renaming folder: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/storage?path=" + renameDto.getPath();
    }


    @PostMapping("/uploadFile")
    public String uploadFile(@ModelAttribute("objectUploadDto") ObjectUploadDto fileUpload,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            minioService.uploadFile(fileUpload, getRootFolder(session));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/storage?path=" + fileUpload.getPath();
    }


    @PostMapping("/uploadFolder")
    public String uploadFolder(@ModelAttribute("objectUploadDto") ObjectUploadDto folderUpload,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            minioService.uploadFolder(folderUpload, getRootFolder(session));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/storage?path=" + folderUpload.getPath();
    }


    @GetMapping("/downloadFile")
    public ResponseEntity<ByteArrayResource> downloadFile(@ModelAttribute("objectDto") ObjectDto objectDto,
                                                          HttpSession session,
                                                          RedirectAttributes redirectAttributes) {
        log.info("Received objectDto: {}", objectDto);
        ByteArrayResource file = null;
        try {
            file = minioService.downloadFile(objectDto, getRootFolder(session));

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + objectDto.getName())
                .body(file);

    }


    @GetMapping("/search")
    public String showSearchResults(@ModelAttribute("searchName") String searchName,
                                    @ModelAttribute("searchDto") SearchDto searchDto,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes,
                                    Model model,
                                    @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ObjectReadDto rootFolder = getRootFolder(session);
        try {
            List<SearchDto> searchResults = minioService.findByName(searchDto, getRootFolder(session));

            model.addAttribute("objects", searchResults);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        submitToModel(model, userPrincipal, rootFolder);

        return "storage";
    }


    @GetMapping("/downloadFolder")
    public void downloadFolder(@ModelAttribute("objectDto") ObjectDto objectDto,
                               RedirectAttributes redirectAttributes,
                               HttpSession session,
                               HttpServletResponse response) {

        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Content-Disposition", "attachment; filename=" + objectDto.getName()+".zip");
        try {
            minioService.downloadFolder(objectDto, response.getOutputStream(), getRootFolder(session));
        } catch (Exception e) {
            log.warn("Failed to download folder", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
    }
}
