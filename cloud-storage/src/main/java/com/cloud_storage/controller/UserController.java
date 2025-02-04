package com.cloud_storage.controller;

import com.cloud_storage.common.UserPrincipal;
import com.cloud_storage.dto.LoginDto;
import com.cloud_storage.dto.UserReadDto;
import com.cloud_storage.service.MinioService;
import com.cloud_storage.service.UserService;
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
            UserReadDto user = userService.save(loginDto);

            minioService.createUserFolder(String.valueOf(user.getId()));

            httpServletRequest.login(loginDto.getUsername(), loginDto.getPassword());
            return "redirect:/storage/user-page";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "/sign-up";
        }
    }


    @DeleteMapping
    public String delete(@AuthenticationPrincipal UserPrincipal userPrincipal,
                         HttpSession session) {
        minioService.deleteUserFolder(String.valueOf(userPrincipal.getId()));

        userService.delete(userPrincipal.getUsername());
        session.invalidate();

        return "redirect:/storage/guest-page";
    }
//    @DeleteMapping
//    public String delete(@AuthenticationPrincipal UserDetails userDetails,
//                         HttpSession session) {
//        userService.delete(userDetails.getUsername());
//        session.invalidate();
//
//        return "redirect:/storage/guest-page";
//    }
}
