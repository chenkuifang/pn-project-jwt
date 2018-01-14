package com.example.demo.interceptor;

import java.util.Objects;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.example.demo.common.Constants;
import com.example.demo.common.util.TokenUtils;
import com.example.demo.common.util.WebContextUtils;

import io.jsonwebtoken.Claims;

/**
 * 登陆拦截器
 * <p>
 * 需要注意的是：该拦截器不会拦截静态资源
 * </p>
 *
 * @author QuiFar
 * @version V1.0
 */
public class LoginInterceptor extends HandlerInterceptorAdapter {

	// private static final Logger Logger =
	// LoggerFactory.getLogger(LoginInterceptor.class);

	/**
	 * controller 执行之前调用,返回true 往下执行,否则不往下执行
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		// 请求限制
		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			throw new ServletException("corsFilter.onlyHttp");
		}

		// 非拦截路径
		String path = request.getServletPath();
		if (path.matches(Constants.NO_INTERCEPTOR_PATH)) {
			return true;
		}

		String token = TokenUtils.getTokenFromRequest(request);
		Claims claims = TokenUtils.parseToken(token);
		if (!TokenUtils.validateToken(claims, token)) {
			response.sendRedirect(request.getContextPath() + "/login.html");
			return false;
		}

		// 这样维护一个本地对象，获取session方便，但每个线程都要维护该对象,但还好HttpSession的实现类不是final的，这个利和弊由开发者决定
		WebContextUtils.setClaims(claims);

		return true;
	}

	/**
	 * controller 执行之后，且页面渲染之前调用
	 */
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		super.postHandle(request, response, handler, modelAndView);

	}

	/**
	 * 页面渲染之后调用，一般用于资源清理操作
	 */
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		if (!Objects.isNull(ex)) {
		}
		super.afterCompletion(request, response, handler, ex);
	}

}
