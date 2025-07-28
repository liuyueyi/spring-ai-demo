package com.git.hui.offer;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Base64;

/**
 * 需要开启异步支持
 *
 * @author YiHui
 * @date 2025/7/28
 */
@WebFilter(asyncSupported = true)
public class ReqFilter implements Filter {
    public static final String TOKEN = "yihuihui-blog";

    public static final String USER = "yihui";
    public static final String PWD = "12345678";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 打印请求日志
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        String url = req.getRequestURI();
        String params = req.getQueryString();
        System.out.println("请求 " + url + " params=" + params);

        String auth = req.getHeader("Authorization");
        if (url.equals("/sse") || url.equals("/mcp/messages")) {
            if (auth == null) {
                throw new RuntimeException("认证头格式错误");
            }

            if (auth.startsWith("Bearer ")) {
                // 令牌方式
                // "Authorization: Bearer <有效的访问令牌>"
                String token = auth.substring(7); // "Bearer " 长度为7
                if (!TOKEN.equals(token)) {
                    throw new RuntimeException("token error");
                }
                System.out.println("token鉴权通过!");
            } else if (auth.startsWith("Basic ")) {
                // 标准Basic Auth格式解析 Authorization: Basic eWlodWk6MTIzNDU2Nzg=
                // Base64编码的 "username:password
                String encodedCredentials = auth.substring(6); // "Basic " 长度为6
                String decodedCredentials = new String(Base64.getDecoder().decode(encodedCredentials));
                String[] credentials = decodedCredentials.split(":", 2);
                String username = credentials[0];
                String password = credentials[1];
                if (!USER.equals(username) || !PWD.equals(password)) {
                    throw new RuntimeException("用户名密码错误");
                }
                System.out.println("basic auth 鉴权通过!");
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
