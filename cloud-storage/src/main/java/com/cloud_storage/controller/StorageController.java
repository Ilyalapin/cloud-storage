package com.cloud_storage.controller;

import com.cloud_storage.common.UserPrincipal;
import com.cloud_storage.common.exception.MinioException;
import com.cloud_storage.common.util.PrefixGenerationUtil;
import com.cloud_storage.dto.ObjectCreateDto;
import com.cloud_storage.dto.ObjectReadDto;
import com.cloud_storage.service.MinioService;
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
                               RedirectAttributes redirectAttributes,
                               Model model) throws Exception {

        String folderName = "user-" + userPrincipal.getId() + "-files/";

        log.info("Creating root folder in bucket, folderName: {}", folderName);
        ObjectReadDto rootFolder = minioService.createRootFolder(folderName, "/");

        List<ObjectReadDto> objects = minioService.getObjects(PrefixGenerationUtil.generatePath(path, rootFolder));

        model.addAttribute("userInfo", userPrincipal.getRole() + ": " + userPrincipal.getUsername());
        model.addAttribute("objectCreateDto", new ObjectCreateDto("", PrefixGenerationUtil.generatePath(path, rootFolder)));
        model.addAttribute("objects", objects);
        redirectAttributes.addFlashAttribute("rootFolder", rootFolder);

        return "storage";
    }


    @PostMapping
    public String createFolder(@RequestParam(required = false) String path,
                               @ModelAttribute("objectCreateDto") ObjectCreateDto objectCreateDto,
                               RedirectAttributes redirectAttributes) throws MinioException {
        log.info("Creating new folder, folderName: {}, path: {}", objectCreateDto.getName(), path);
        objectCreateDto.setPath(path);
        ObjectReadDto newFolder = minioService.createFolder(objectCreateDto.getName(), objectCreateDto.getPath());
        redirectAttributes.addFlashAttribute("newFolder", newFolder);

        return "redirect:/storage?path=" + objectCreateDto.getPath();
    }
}
