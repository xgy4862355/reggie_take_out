package com.xgy.reggie.filter;

import com.alibaba.fastjson.JSON;


import com.xgy.reggie.common.BaseContext;
import com.xgy.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否已经完成登录，未登录则不允许访问其他动态资源页面
 */

@WebFilter(filterName = "loginCheckFilter",urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {
    //创建一个路径匹配器，方便调用它的内置匹配方法进行路径匹配(支持通配符)
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        //向下转型拿到HttpServletRequest
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        //1、获取本次请求的URI
        String requestURI = request.getRequestURI();

        //日志打印拦截路径
        log.info("拦截到请求：{}",requestURI);

        //定义不需要处理的请求路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/user/sendMsg", //移动端发送短信
                "/user/login"    //移动端登录
        };

        //2、判断本次请求是否需要处理
        boolean check = check(urls, requestURI);

        //3、如果不需要处理，则直接放行
        if(check){
            log.info("本次请求：{}不需要处理",requestURI);
            filterChain.doFilter(request,response);
            return;
        }

        //4-1、否则需要处理，再判断当前是否为登录状态，如果已登录，放行   -----网页端
        if(request.getSession().getAttribute("employee") != null){
            log.info("用户 {} 已登录，放行",request.getSession().getAttribute("employee"));
            //long id = Thread.currentThread().getId();
            //log.info("线程id为："+id);

            Long empId = (Long) request.getSession().getAttribute("employee");
            //调用工具类将当前用户id共享给整个线程
            BaseContext.setCurrentId(empId);

            filterChain.doFilter(request,response);
            return;
        }

        //4-2、否则需要处理，再判断当前是否为登录状态，如果已登录，放行   -----移动端
        if(request.getSession().getAttribute("user") != null){
            log.info("用户 {} 已登录，放行",request.getSession().getAttribute("user"));
            //long id = Thread.currentThread().getId();
            //log.info("线程id为："+id);

            Long userId = (Long) request.getSession().getAttribute("user");
            //调用工具类将当前用户id共享给整个线程
            BaseContext.setCurrentId(userId);

            filterChain.doFilter(request,response);
            return;
        }

        //5、否则未登录，则返回登录结果，通过输出流方式向客户端页面响应数据(配合前端响应拦截器统一判断)
        log.info("用户未登录，不予放行");
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;

    }

    /**
     * 路径匹配，检查本次匹配是否需要放行
     * @param urls
     * @param requestURI
     * @return
     */
    public boolean check(String[] urls,String requestURI){
        for (String url : urls) {
            //调用内置方法进行匹配
            boolean match = PATH_MATCHER.match(url, requestURI);
            //如果匹配上了，说明此路径不需要过滤处理
            if(match){
                return true;
            }
        }
        return false;
    }
}
