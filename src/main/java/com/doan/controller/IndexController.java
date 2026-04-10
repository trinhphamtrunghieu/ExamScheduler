package com.doan.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController implements ErrorController {

	// Handle root path and serve index.html
	@GetMapping("/")
	public String home() {
		return "forward:/index.html";
	}

	// Handle all unknown routes (React Router support)
	@RequestMapping({"/add-course", "/add-student", "/students", "/subjects", "/registrations"})
	public String forwardToIndex() {
		return "forward:/index.html";
	}

	// Handle errors
	@RequestMapping("/error")
	public String handleError() {
		return "forward:/index.html";
	}
}
