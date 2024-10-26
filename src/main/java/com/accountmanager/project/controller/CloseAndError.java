package com.accountmanager.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CloseAndError {
    @GetMapping("/close")
    public String closeWindow(){
        return "close";
    }
    @GetMapping("/fbError")
    public String fbError(){
        return "fbError";
    }
    @GetMapping("/xError")
    public String xError(){
        return "xError";
    }

    @GetMapping("/error")
    public String error(){
        return "error";
    }
}

