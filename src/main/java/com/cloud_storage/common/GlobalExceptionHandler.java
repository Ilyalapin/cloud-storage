package com.cloud_storage.common;

import com.cloud_storage.common.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistException.class)
    public String handleUserAlreadyExistException(UserAlreadyExistException e, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        return "redirect:/user/sign-up";
    }

    @ExceptionHandler(UserInvalidParameterException.class)
    public String handleInvalidParameterException(UserInvalidParameterException e, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        return "/user/sign-up";
    }


    @ExceptionHandler(MinioInvalidParameterException.class)
    public String handleMinioInvalidParameterException(MinioInvalidParameterException e, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        return "redirect:/storage";
    }


    @ExceptionHandler(FileOperationException.class)
    public String handleFileOperationException(FileOperationException e, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        return "redirect:/storage";
    }


    @ExceptionHandler(FolderOperationException.class)
    public String handleFolderOperationException(FolderOperationException e, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        return "redirect:/storage";
    }


    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<String> handleUserNotFoundException() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    }


    @ExceptionHandler(FileNotFoundException.class)
    public String handleFileNotFoundException(FileNotFoundException e, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        return "redirect:/storage";
    }


    @ExceptionHandler(MinioException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<String> handleMinioException() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Error access to storage");
    }


    @ExceptionHandler(UserIdConflictException.class)
    public ResponseEntity<String> handleUserIdConflictException() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Access error for a given ID");
    }
}
