package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否完成登录
 *  在启动类 添加注解@ServletComponentScan  才可以加载过滤器
 */
@WebFilter(filterName = "loginCheckFilter",urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {

    //路径匹配器  支持通配符匹配
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * 实现Filter接口,登录检查过滤器
     * @param servletRequest
     * @param servletResponse
     * @param filterChain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        //1.获取本次请求的URI
        String requestURI = request.getRequestURI();
        log.info("请求路径为:  {}",requestURI);
        //定义不需要处理的路径
        String[] uris = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",  //静态资源
                "/front/**"
                ,"/common/upload"
                ,"/common/download"
                ,"/user/sendMsg" // 移动端发送短信
                ,"/user/login" //移动端登录

        };

        //2.判断此次请求是否需要处理
        boolean check = check(requestURI, uris);
        if (check){
            log.info("请求不需要处理: {}",requestURI);
            //3.如果不需要处理直接放行
            filterChain.doFilter(request,response);
            return; //原因是后边的不要再执行了
        }

        //4-1.判断登录状态,如果已登录直接放行
        if (request.getSession().getAttribute("employee") != null){
            log.info("用户已经登录,id: {}",request.getSession().getAttribute("employee"));

            //threadLocal 存储登录id值
            Long empId = (Long) request.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);

            filterChain.doFilter(request,response);
            return;
        }

        //4-2.判断登录状态,如果已登录直接放行
        if (request.getSession().getAttribute("user") != null){
            log.info("用户已经登录,id: {}",request.getSession().getAttribute("user"));

            //threadLocal 存储登录id值
            Long userId = (Long) request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);

            filterChain.doFilter(request,response);
            return;
        }

        //5.如果未登录则返回未登录结果  通过输出流方式向客户端页面反应
        //NOTLOGIN 前端判断码  注意转为JSon 格式
        log.info("未登录");
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;

//        log.info("拦截到请求: {}",request.getRequestURI()); //{}  为占位符  注意获取的是UR"I"
//        filterChain.doFilter(request,response);
    }

    /**
     * 匹配判断
     * @param requestURI
     * @param uris
     * @return
     */
    public boolean check(String requestURI,String[] uris){
        for (String uri : uris) {
            boolean match = PATH_MATCHER.match(uri, requestURI);
            if (match) {
                return true;
            }
        }
        return false;
    }

}
