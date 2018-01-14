package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.common.WebContext;
import com.example.demo.common.util.WebContextUtils;

/**
 * 首页控制类
 *
 * @author QuiFar
 */
@Controller
public class IndexController {

	/**
	 * 访问首页
	 *
	 * @param model
	 * @return
	 */
	@GetMapping("/index")
	public String index(Model model, @CookieValue("token") String token) {
		WebContext webContext = WebContextUtils.getCurrentUser();
		model.addAttribute("webContext", webContext);
		System.err.println("客户端传来的token:" + token);
		return "index";
	}

	/**
	 * 主页视图初始化
	 *
	 * @return
	 */
	@GetMapping("/main")
	public String main() {
		return "main";
	}
}
