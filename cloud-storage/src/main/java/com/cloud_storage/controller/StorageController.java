package com.cloud_storage.controller;

import com.cloud_storage.common.UserPrincipal;
import com.cloud_storage.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/storage")
@RequiredArgsConstructor
public class StorageController {
    private final UserService userService;


    @GetMapping("/guest-page")
    public String guestPage() {
        return "home";
    }


    @GetMapping("/user-page")
    public String userPage(@AuthenticationPrincipal UserPrincipal userPrincipal,
                           Model model) {

        String userInfo = userPrincipal.getAuthorities().toString()
                .replace("[", "")
                .replace("]", "") + ": " + userPrincipal.getUsername();

        model.addAttribute("userInfo", userInfo);
        return "user-page";
    }
//    @GetMapping("/user-page")
//    public String userPage(@AuthenticationPrincipal UserDetails userDetails,
//                           Model model) {
//
//        String userInfo = userDetails.getAuthorities().toString()
//                .replace("[", "")
//                .replace("]", "") + ": " + userDetails.getUsername();
//
//        model.addAttribute("userInfo", userInfo);
//        return "user-page";
//    }
}
