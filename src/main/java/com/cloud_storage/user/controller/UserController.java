package com.cloud_storage.user.controller;

import com.cloud_storage.common.exception.MinioException;
import com.cloud_storage.minio.service.MinioService;
import com.cloud_storage.user.dto.LoginDto;
import com.cloud_storage.user.service.UserPrincipal;
import com.cloud_storage.user.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    public String create(@ModelAttribute("user") LoginDto loginDto,HttpServletRequest httpServletRequest) throws ServletException {
            userService.save(loginDto);
            httpServletRequest.login(loginDto.getUsername(), loginDto.getPassword());

            return "redirect:/storage";
    }


    @DeleteMapping
    public String delete(@AuthenticationPrincipal UserPrincipal userPrincipal,HttpSession session) throws MinioException {
        String rootFolder = "user-" + userPrincipal.getId() + "-files/";
            minioService.deleteObject(rootFolder);
            userService.delete(userPrincipal.getUsername());
            session.invalidate();

            return "redirect:/storage/guest-page";
    }
}
