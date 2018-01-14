package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.common.Constants;
import com.example.demo.common.JsonResult;
import com.example.demo.common.util.JsonResultUtils;
import com.example.demo.common.util.MDUtils;
import com.example.demo.common.util.TokenUtils;
import com.example.demo.common.util.WebContextUtils;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;

import io.jsonwebtoken.Claims;

/**
 * 登陆控制类
 *
 * @author QuiFar
 * @version V1.0
 */
@Controller
public class LoginController {

	@Autowired
	private StringRedisTemplate template;
	@Autowired
	private UserService UserService;
	// @Value("${pn.redis.expirationTime}")
	// private long expirationTime;

	/**
	 * 获取登陆初始化页面
	 *
	 * @return
	 */
	@GetMapping("/login")
	public String login() {
		return "login";
	}

	/**
	 * 登陆校验
	 *
	 * @param userName
	 * @param password
	 * @return code 100 成功，101 失败
	 */
	@PostMapping("/loginPost")
	@ResponseBody
	public JsonResult loginPost(HttpServletRequest request, @RequestParam("userName") String userName,
			@RequestParam("password") final String password) {

		String code = Constants.FAIL_CODE;
		String msg = Constants.USER_NAME_OR_PASSWORD_ERROR;
		User user = UserService.getByUserName(userName);
		String token = "";
		if (user != null) {
			String md5Str = "";
			try {
				md5Str = MDUtils.encodeMD5(password);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (user.getPassword().equals(md5Str)) {
				code = Constants.SUCCESS_CODE;
				msg = Constants.SUCCESS_DESCRIPTION;

				Map<String, Object> webContext = new HashMap<>();
				webContext.put("userId", user.getId());
				webContext.put("userName", user.getUserName());
				webContext.put("userNike", user.getUserNike());
				webContext.put("roleId", user.getRoleId());
				webContext.put("ip", request.getRemoteAddr());

				token = TokenUtils.createToken(webContext, userName);

				Claims claims = TokenUtils.parseToken(token);
				WebContextUtils.setClaims(claims);

				// 保存有效token到redis中
				ValueOperations<String, String> ops = template.opsForValue();
				try {
					ops.set("user&" + user.getId(), token);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		System.err.println("loginToken:" + token);
		// 结果返回
		return JsonResultUtils.jsonResult(code, msg, token);
	}

	/**
	 * 登出
	 *
	 * @return
	 */
	@GetMapping("/loginOut")
	public String loginOut(@CookieValue("token") String token) {
		// 刷新token
		String newToken = TokenUtils.refreshToken(token);
		int userId = WebContextUtils.getCurrentUserId();
		// 保存有效token到redis中
		ValueOperations<String, String> ops = template.opsForValue();
		try {
			String aa =ops.get("user&" + userId);
			System.err.println("redis中旧的token:"+aa);
			ops.set("user&" + userId, newToken);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "redirect:/login";
	}

	/**
	 * 修改密码初始化视图
	 *
	 * @return
	 */
	@GetMapping("/changePwd")
	public String changePwd(Model model) {
		String userName = WebContextUtils.getCurrentUserName();
		model.addAttribute("userName", userName);
		model.addAttribute("result", "");
		return "user/changePwd";
	}

	/**
	 * 获取修改密码操作
	 *
	 * @param userName
	 *            用户名
	 * @param oldPwd
	 *            旧密码
	 * @param newPwd
	 *            新密码
	 * @param model
	 *            需要返回的数据
	 * @return
	 * @throws Exception
	 */
	@PostMapping("/changePwdPost")
	public String changePwdPost(@RequestParam String userName, @RequestParam String oldPwd, @RequestParam String newPwd,
			Model model) throws Exception {
		String result;

		// 根据用户名获取用户信息
		User user = UserService.getByUserName(userName);
		String encodePwd = MDUtils.encodeMD5(oldPwd);
		if (user != null && user.getPassword().equals(encodePwd)) {
			User newUser = new User();
			newUser.setId(user.getId());
			newUser.setPassword(MDUtils.encodeMD5(newPwd));

			// 修改密码
			int flag = UserService.update(newUser);
			result = flag >= 1 ? Constants.SUCCESS_DESCRIPTION : Constants.FAIL_DESCRIPTION;
		} else {
			result = Constants.USER_NAME_OR_PASSWORD_ERROR;
		}
		model.addAttribute("userName", userName);
		model.addAttribute("result", result);
		return "user/changePwd";
	}
}
