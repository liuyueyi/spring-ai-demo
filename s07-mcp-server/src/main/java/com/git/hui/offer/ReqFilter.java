package com.git.hui.offer;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * 需要开启异步支持
 *
 * @author YiHui
 * @date 2025/7/28
 */
@WebFilter(asyncSupported = true)
public class ReqFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 打印请求日志
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        String url = req.getRequestURI();
        String params = req.getQueryString();
        System.out.println("请求 " + url + " params=" + params);
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
