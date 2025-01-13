package com.cloud_storage.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/sign-in")
public class SignInController {

    @GetMapping
    public String signInPage(@RequestParam(value = "error", required = false) String error,
                             Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password.");
        }
        return "sign-in";
    }
}
