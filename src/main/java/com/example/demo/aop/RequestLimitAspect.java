package com.example.demo.aop;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.demo.annotation.RequestLimit;
import com.example.demo.exception.RequestLimitException;

/**
 * @author Administrator @time： @Discription：
 */
@Aspect
@Component
public class RequestLimitAspect {
	private static final Logger logger = LoggerFactory.getLogger("RequestLimitAspect");
	// private Map<String, Integer> redisTemplate = new HashMap<>();
	private Map<String, Integer> redisTemplate = new ConcurrentHashMap<>();

	@Before("within(@org.springframework.stereotype.Controller *) && @annotation(limit)")
	public void requestLimit(final JoinPoint joinPoint, RequestLimit limit) throws RequestLimitException {

		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = attributes.getRequest();
		// MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		// Method method = signature.getMethod();

		String ip = request.getLocalAddr();
		String url = request.getRequestURL().toString();
		String key = "req_limit_".concat(url).concat(ip);
		Integer requestTimes = redisTemplate.get(key);
		AtomicInteger value;
		if (requestTimes == null) {
			value = new AtomicInteger(1);
			redisTemplate.put(key, value.get());
		} else {
			value = new AtomicInteger(requestTimes);
			redisTemplate.put(key, value.addAndGet(1));
		}
		
		// 最新的值判断
		int count = redisTemplate.get(key);
		if (count > 0) {
			// 创建一个定时器
			Timer timer = new Timer();
			TimerTask timerTask = new TimerTask() {
				@Override
				public void run() {
					redisTemplate.remove(key);
				}
			};

			// 在規定的時間后清除key
			timer.schedule(timerTask, limit.time());
		}
		if (count > limit.count()) {
			logger.info("用户IP[" + ip + "]访问地址[" + url + "]超过了限定的次数[" + limit.count() + "]");
			throw new RequestLimitException();
		}

	}
}