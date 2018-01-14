package com.example.demo.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description:CORS 跨域请求，支持get，post 安全性高，参考
 *                   org.apache.catalina.filters.CorsFilter
 * @author QuiFar
 * @date 2017年10月25日 下午4:45:00
 * @version V1.0
 */
//@Component
public class MyCorsFilter implements Filter {
	private static final Logger log = LoggerFactory.getLogger(MyCorsFilter.class);
	/**
	 * 默认请求支持访问方法
	 */
	public static final String DEFAULT_ALLOWED_HTTP_METHODS = "GET,POST,HEAD,OPTIONS,DELETE";
	/**
	 * 请求头用户token
	 */
	public static final String REQUEST_HEADER_USER_TOKEN = "UserToken";
	/**
	 * 预检请求
	 */
	public static final String REQUEST_HEADER_ORIGIN = "Origin";

	/**
	 * 内容类型
	 */
	public static final String REQUEST_HEADER_CONTENT_TYPE = "Content-Type";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {

		if (!(req instanceof HttpServletRequest) || !(res instanceof HttpServletResponse)) {
			throw new ServletException("corsFilter.onlyHttp");
		}

		HttpServletRequest request = (HttpServletRequest) req;

		// 响应请求头设置
		HttpServletResponse response = (HttpServletResponse) res;
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", DEFAULT_ALLOWED_HTTP_METHODS);
		response.setHeader("Access-Control-Max-Age", "3600");

		// 重点
		StringBuilder sbBuilder = new StringBuilder();
		sbBuilder.append(REQUEST_HEADER_ORIGIN).append(",");
		sbBuilder.append(REQUEST_HEADER_CONTENT_TYPE).append(",");
		sbBuilder.append(REQUEST_HEADER_USER_TOKEN);
		response.setHeader("Access-Control-Allow-Headers", sbBuilder.toString());

		log.info("token：" + request.getHeader(REQUEST_HEADER_USER_TOKEN));
		// 由于需要传token,header里面包含自定义字段，为非简单请求，
		// 浏览器是会先发一次options请求，如果请求通过，则继续发送正式的post请求，而如果不通过则返回以上错误
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			response.setStatus(HttpServletResponse.SC_OK);
			return;
		}
		log.info("响应状态：" + response.getStatus());
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
	}

}
