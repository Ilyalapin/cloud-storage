package com.cloud_storage.controller;

import com.cloud_storage.common.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/storage")
public class StorageController {

    @GetMapping("/guest-page")
    public String guestPage() {
        return "home";
    }


    @GetMapping("/user-page")
    public String userPage(@AuthenticationPrincipal UserPrincipal userPrincipal,
                           Model model) {
        String userInfo = userPrincipal.getRole() + ": " + userPrincipal.getUsername();

        /** здесь по идее юзер должен сразу видеть содержимое директории user-62-files/
         * скорее всего как-то так: List<Item> items = minioService.list(Path.of("user-62-files/"));
         * и потом как-то передаваться в модел: model.addAttribute("items", items);
         */
        model.addAttribute("userInfo", userInfo);
        return "user-page";
    }
}
