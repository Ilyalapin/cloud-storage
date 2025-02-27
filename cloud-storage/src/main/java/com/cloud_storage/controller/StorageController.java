package com.cloud_storage.controller;

import com.cloud_storage.common.UserPrincipal;
import com.cloud_storage.common.exception.InvalidParameterException;
import com.cloud_storage.common.exception.MinioException;
import com.cloud_storage.common.util.PrefixGenerationUtil;
import com.cloud_storage.dto.FolderDeleteDto;
import com.cloud_storage.dto.ObjectCreateDto;
import com.cloud_storage.dto.ObjectReadDto;
import com.cloud_storage.dto.RenameDto;
import com.cloud_storage.service.MinioService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        List<ObjectReadDto> objects = minioService.getObjects(PrefixGenerationUtil.generatePath(path, rootFolder));

        model.addAttribute("userInfo", userPrincipal.getRole() + ": " + userPrincipal.getUsername());
        model.addAttribute("objectCreateDto", new ObjectCreateDto("", PrefixGenerationUtil.generatePath(path, rootFolder)));
        model.addAttribute("objects", objects);
        model.addAttribute("folderDeleteDto", new FolderDeleteDto());
        model.addAttribute("renameDto", new RenameDto());
        model.addAttribute("path", PrefixGenerationUtil.getBackPath(path));
        model.addAttribute("links", PrefixGenerationUtil.generateFromDirectory(path, rootFolder));
        session.setAttribute("rootFolder", rootFolder);

        return "storage";
    }


    @PostMapping
    public String createFolder(@RequestParam(required = false) String path,
                               @ModelAttribute("objectCreateDto") ObjectCreateDto objectCreateDto,
                               RedirectAttributes redirectAttributes) {
        try {
            log.info("Creating new folder, folderName: {}, path: {}", objectCreateDto.getName(), path);

            objectCreateDto.setPath(path);
            ObjectReadDto newFolder = minioService.createFolder(objectCreateDto.getName(), objectCreateDto.getPath());

            log.info("Folder created: {}", newFolder);
        } catch (InvalidParameterException e) {
            log.error(e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error creating folder. " + e.getMessage());
        } catch (MinioException e) {
            log.error("Error creating folder: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/storage?path=" + objectCreateDto.getPath();
    }


    @DeleteMapping("/deleteFolder")
    public String deleteFolder(@ModelAttribute("folderDeleteDto") FolderDeleteDto folderDeleteDto,
                               RedirectAttributes redirectAttributes) {
        try {
            log.info("Attempting to delete folder: {}", folderDeleteDto.getFolderName());
            minioService.deleteObject(folderDeleteDto.getPath() + folderDeleteDto.getFolderName() + "/");

            log.info("Folder with name:{} deleted successfully", folderDeleteDto.getFolderName());
        } catch (Exception e) {
            log.error("Error deleting folder: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/storage?path=" + folderDeleteDto.getPath();
    }


    @PatchMapping("/rename")
    public String rename(@ModelAttribute("renameDto") RenameDto renameDto,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        try {
            ObjectReadDto rootFolder = (ObjectReadDto) session.getAttribute("rootFolder");
            minioService.renameObject(renameDto.getOldName(), renameDto.getNewName(), renameDto.getPath(), rootFolder);

        } catch (InvalidParameterException e) {
            log.error(e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error renaming folder. " + e.getMessage());
        } catch (MinioException e) {
            log.error("Error renaming folder: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/storage?path=" + renameDto.getPath();
    }
}
