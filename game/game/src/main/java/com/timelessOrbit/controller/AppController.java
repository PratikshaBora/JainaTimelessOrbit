package com.timelessOrbit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AppController {

	@RequestMapping({
		 "/", "/login", "/home", "/lobby", "/room/*", "/winner"
	})
	public String index()
	{
		return "index";
	}
}
