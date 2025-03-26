package com.cloud_storage.user.controller;

import com.cloud_storage.user.service.UserPrincipal;
import com.cloud_storage.user.dto.LoginDto;
import com.cloud_storage.minio.service.MinioService;
import com.cloud_storage.user.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final MinioService minioService;

    @GetMapping("/sign-in")
    public String signInPage(@RequestParam(value = "error", required = false) String error,
                             Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password.");
        }
        return "sign-in";
    }


    @GetMapping("/sign-up")
    public String signUpPage() {
        return "sign-up";
    }


    @PostMapping
    public String create(@ModelAttribute("user") LoginDto loginDto,
                         HttpServletRequest httpServletRequest,
                         Model model) throws ServletException {
        try {
            userService.save(loginDto);

            httpServletRequest.login(loginDto.getUsername(), loginDto.getPassword());

            return "redirect:/storage";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "/sign-up";
        }
    }


    @DeleteMapping
    public String delete(@AuthenticationPrincipal UserPrincipal userPrincipal,
                         HttpSession session,
                         Model model) {
        String rootFolder = "user-" + userPrincipal.getId() + "-files/";
        try {
            minioService.deleteObject(rootFolder);

            userService.delete(userPrincipal.getUsername());
            session.invalidate();

            return "redirect:/storage/guest-page";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/storage/guest-page";
        }
    }
}
