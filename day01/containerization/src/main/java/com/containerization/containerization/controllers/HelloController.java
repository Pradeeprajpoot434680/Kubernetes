package com.containerization.containerization.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/v1")
public class HelloController {
    @GetMapping("/hello")
    public String getMethodName() {
        return "Hello from Spring boot";
    }
    
}
