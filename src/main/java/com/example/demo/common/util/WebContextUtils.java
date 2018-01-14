package com.example.demo.common.util;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;

import com.example.demo.common.Constants;
import com.example.demo.common.WebContext;

import io.jsonwebtoken.Claims;

/**
 * 系统上下文工具类
 *
 * @author QuiFar
 * @version V1.0
 */
public class WebContextUtils {

	// 保存授权信息
	private static ThreadLocal<Claims> threadLocal = new ThreadLocal<>();

	private WebContextUtils() {
	}

	/**
	 * 设置claims,此方法在LoginInterceptor中调用赋值
	 *
	 * @param claims
	 * @see #preHandle(HttpServletRequest request, HttpServletResponse response,
	 *      Object handler)
	 */
	public static void setClaims(Claims claims) {
		WebContextUtils.threadLocal.set(claims);
	}

	/***
	 * 返回Claims
	 *
	 * @return
	 */
	public static Claims getClaims() {
		Assert.notNull(threadLocal, "threadLocal is must not be null");
		return threadLocal.get();
	}

	/**
	 * 清空Claims对象
	 */
	public static void clear() {
		threadLocal.remove();
	}

	/**
	 * 获取当前用户信息
	 */
	@SuppressWarnings("unchecked")
	public static WebContext getCurrentUser() {
		Object object = getClaims().get(Constants.WEB_CONTEXT_MAP);
		Map<String, Object> map = (Map<String, Object>) object;

		WebContext webContext = new WebContext();
		webContext.setIp(map.get("ip").toString());
		webContext.setRoleId(StringUtils.parseInt(map.get("roleId")));
		webContext.setUserId(StringUtils.parseInt(map.get("userId")));
		webContext.setUserName(map.get("userName").toString());
		webContext.setUserNike(map.get("userNike").toString());
		return webContext;
	}

	/**
	 * 获取当前用户Id
	 */
	public static int getCurrentUserId() {
		return getCurrentUser().getUserId();
	}

	/**
	 * 获取当前用户名
	 */
	public static String getCurrentUserName() {
		return getCurrentUser().getUserName();
	}

	/**
	 * 获取当前用户角色Id
	 */
	public static int getCurrentRoleId() {
		return getCurrentUser().getRoleId();
	}

	/**
	 * 获取当前用户角色IP
	 */
	public static String getRemoteAddr() {
		return getCurrentUser().getIp();
	}
}
