package com.cloud_storage.controller;

import com.cloud_storage.dto.LoginDto;
import com.cloud_storage.dto.UserCreateDto;
import com.cloud_storage.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/registration")
public class RegistrationController {
    private final UserService userService;


    @GetMapping
    public String registrationPage() {
        return "registration";
    }


    @PostMapping
    public String create(@ModelAttribute("user") LoginDto loginDto, HttpServletRequest httpServletRequest) throws ServletException {

        userService.create(loginDto);
        httpServletRequest.login(loginDto.getUsername(),loginDto.getPassword());

        return "redirect:/user-page";
    }
}
