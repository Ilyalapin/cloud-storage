package com.cloud_storage.controller;

import com.cloud_storage.common.UserPrincipal;
import com.cloud_storage.common.util.PrefixGenerationUtil;
import com.cloud_storage.dto.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;

public abstract class BaseController {
    protected ObjectReadDto getRootFolder(HttpSession session) {
        return (ObjectReadDto) session.getAttribute("rootFolder");
    }


    protected void submitToModel(Model model, UserPrincipal userPrincipal, ObjectReadDto rootFolder) {
        model.addAttribute("userInfo", userPrincipal.getRole() + ": " + userPrincipal.getUsername());
        model.addAttribute("objectCreateDto", new ObjectDto());
        model.addAttribute("folderDeleteDto", new ObjectDeleteDto());
        model.addAttribute("renameDto", new ObjectRenameDto());
        model.addAttribute("path", PrefixGenerationUtil.generatePathBackForBreadCrumbs(""));
        model.addAttribute("breadCrumbs", PrefixGenerationUtil.generatePathForBreadCrumbs("", rootFolder));
        model.addAttribute("objectUploadDto", new ObjectUploadDto());
    }
}

