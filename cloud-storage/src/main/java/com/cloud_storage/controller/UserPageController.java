package com.cloud_storage.controller;

import com.cloud_storage.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/user-page")
@RequiredArgsConstructor
public class UserPageController {
    private final UserService userService;


    @GetMapping
    public String userPage(@AuthenticationPrincipal UserDetails userDetails,
                           Model model) {

        String userInfo = userDetails.getAuthorities().toString()
                .replace("[", "")
                .replace("]", "") + ": " + userDetails.getUsername();

        model.addAttribute("userInfo", userInfo);
        return "user-page";
    }


    @PostMapping("/delete")
    public String delete(@AuthenticationPrincipal UserDetails userDetails,
                         HttpSession session) {
        userService.delete(userDetails.getUsername());
        session.invalidate();

        return "redirect:/home";
    }

}
