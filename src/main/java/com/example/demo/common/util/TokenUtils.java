package com.example.demo.common.util;

import java.io.Serializable;
import java.security.Key;
import java.util.Date;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.example.demo.common.Constants;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * @Description: Jwt工具类token包括：1. header (base64后的,一般做声明作用)2.payload (base64后的)
 *               3.secret(第三部分是签证，包含私钥，必须保存在服务器)
 *               http://blog.csdn.net/a82793510/article/details/53509427
 * @author QuiFar
 * @date 2017年9月26日 下午6:05:10
 * @version V1.0
 */
@Component
public final class TokenUtils implements Serializable {
	private TokenUtils() {
	}

	private static final long serialVersionUID = -8834545248681200597L;

	/**
	 * 私钥
	 */
	private final static String SESCRET_STRING = "quifar";
	/**
	 * 过期时间(毫秒)60秒
	 */
	private final static long EXP_TIME = 20 * 60 * 1000;

	/**
	 * 签发者
	 */
	private final static String ISSUER = "http://www.phonepn.com";

	/**
	 * token名称
	 */
	private final static String TOKEN_NAME = "token";

	/**
	 * 解密JWT
	 * 
	 * @param token
	 *            接收客户端的token
	 * @param base64Security
	 *            私钥
	 * @return 载荷,如果解密不成功返回null
	 */
	public static Claims parseToken(String token) {
		if (token == null) {
			return null;
		}
		try {
			// 私钥base64加密
			final byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(SESCRET_STRING);
			Claims claims = Jwts.parser().setSigningKey(apiKeySecretBytes).parseClaimsJws(token).getBody();
			return claims;
		} catch (Exception ex) {
			return null;
		}
	}

	/**
	 * 创建JWT
	 * 
	 * @param webContextMap
	 *            载荷(包括一些不敏感的信息)
	 * @param audience
	 *            接收jwt者
	 * @return
	 */
	public static String createToken(Map<String, Object> webContextMap, String audience) {
		// 1.声明加密的算法 通常直接使用 HA256
		final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

		// 2.生成签名私钥(对私钥进行base64加密,那么解密同样需要相同的加密方式)
		final byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(SESCRET_STRING);
		final Key secret = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

		// 添加构成JWT的参数
		JwtBuilder builder = Jwts.builder().setHeaderParam("typ", "JWT")// header1.声明类型，2.声明加密的算法
				// .setHeaderParam("alg", "HS256")
				.claim(Constants.WEB_CONTEXT_MAP, webContextMap)// payload 载荷
				.setIssuer(ISSUER) // 发行者、签发者
				.setAudience(audience)// 接收jwt的一方
				.signWith(signatureAlgorithm, secret);

		// 添加Token过期时间
		if (EXP_TIME >= 0) {
			long nowMillis = System.currentTimeMillis();
			Date now = new Date(nowMillis);
			long expMillis = nowMillis + EXP_TIME;
			Date exp = new Date(expMillis);
			builder.setExpiration(exp).setNotBefore(now);// 把之前的token移除
		}

		// 生成JWT
		return builder.compact();
	}

	/**
	 * 刷新token
	 * 
	 * @param token
	 * @return
	 */
	public static String refreshToken(String token) {

		// 1.声明加密的算法 通常直接使用 HA256
		final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
		final Date createdDate = new Date();
		final Date expirationDate = calculateExpirationDate(createdDate);
		// 2.生成签名私钥(对私钥进行base64加密,那么解密同样需要相同的加密方式)
		final byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(SESCRET_STRING);
		final Key secret = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

		final Claims claims = parseToken(token);
		// System.err.println("旧tokeng过期时间:" + claims.getExpiration());
		claims.setIssuedAt(createdDate);
		claims.setExpiration(expirationDate);// 移除之前的token

		String newToken = Jwts.builder().setClaims(claims).signWith(signatureAlgorithm, secret)
				.setNotBefore(createdDate).compact();
		return newToken;
	}

	/**
	 * 从请求头中获取token
	 * <p>
	 * 基于简单请求，所以把token直接放在cookie中，当然也可以直接在请求头中加入token
	 * <p>
	 * 这样一来就是一个非简单请求，需要option请求的允许
	 * 
	 * @param request
	 *            http 请求
	 * @return token
	 */
	public static String getTokenFromRequest(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null && cookies.length > 0)
			for (Cookie c : cookies)
				if (TOKEN_NAME.equals(c.getName()))
					return c.getValue();

		return null;
	}

	/**
	 * 校验token是否有效,结合redis做黑名单
	 * 
	 * @param token
	 *            请求的token
	 * @return true 为有效；false 为无效
	 */
	public static Boolean validateToken(String token) {
		Claims claims = parseToken(token);
		if (claims == null) {
			return false;
		}
		// 签发者
		String issuer = claims.getIssuer();

		int userId = WebContextUtils.getCurrentUserId();

		// 获取有效的token
		String key = "user:" + userId;

		// String validToken = ops.get("user:" + userId);
		String validToken = JedisUtils.get(key);

		final Date expiration = claims.getExpiration();
		return (expiration.after(new Date()) && ISSUER.equals(issuer) && token.equals(validToken));
	}

	/**
	 * 校验token是否有效
	 * 
	 * @param claims
	 *            请求的token用户载荷
	 * @param token
	 *            请求的token
	 * @return true 为有效；false 为无效
	 */
	public static Boolean validateToken(Claims claims, String token) {
		if (claims == null) {
			return false;
		}
		// 签发者
		String issuer = claims.getIssuer();

		String userId = getUserIdFromClamis(claims);
		System.err.println("验证开始读取redis的token...................！！");
		// 从redis中获取有效的token
		// ValueOperations<String, String> ops = template.opsForValue();
		String key = "user:".concat(userId);
		// String validToken = ops.get(key);

		String validToken = JedisUtils.get(key);

		System.err.println("验证的时候读取到的token:" + validToken);
		final Date expiration = claims.getExpiration();
		return (expiration.after(new Date()) && ISSUER.equals(issuer) && token.equals(validToken));
	}

	/**
	 * 从载荷中获取用户id
	 * 
	 * @param claims
	 * @return
	 */
	@SuppressWarnings({ "unchecked" })
	private static String getUserIdFromClamis(Claims claims) {
		Object object = claims.get(Constants.WEB_CONTEXT_MAP);
		Map<String, Object> map = (Map<String, Object>) object;
		return String.valueOf(map.get("userId"));
	}

	/**
	 * 判断token是否过期
	 * 
	 * @param token
	 * @return
	 */
	@SuppressWarnings("unused")
	private static Boolean isTokenExpired(String token) {
		Claims clamis = parseToken(token);
		Assert.notNull(clamis, "clamis is must not be null");
		final Date expiration = clamis.getExpiration();

		// 签发者
		String issuer = clamis.getIssuer();
		int userId = WebContextUtils.getCurrentUserId();
		// 获取有效的token
		StringRedisTemplate template = new StringRedisTemplate();
		ValueOperations<String, String> ops = template.opsForValue();
		String validToken = ops.get("userId&" + userId);
		return (expiration.after(new Date()) && ISSUER.equals(issuer) && token.equals(validToken));
	}

//	/**
//	 * 获取token的过期时间
//	 * 
//	 * @param token
//	 * @return
//	 */
//	private static Date getExpirationDateFromToken(String token) {
//		Claims clamis = parseToken(token);
//		Assert.notNull(clamis, "clamis is must not be null");
//		return clamis.getExpiration();
//	}

	private static Date calculateExpirationDate(Date createdDate) {
		long nowMillis = System.currentTimeMillis();
		long expMillis = nowMillis + EXP_TIME;
		return new Date(expMillis);
	}
}
