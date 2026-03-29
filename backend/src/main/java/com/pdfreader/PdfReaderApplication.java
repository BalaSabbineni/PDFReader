package com.pdfreader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@SpringBootApplication
@RestController
public class PdfReaderApplication {
    public static void main(String[] args) {
        SpringApplication.run(PdfReaderApplication.class, args);
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello!... Welcome to the PDF Reader Application!";
    }
    

}
