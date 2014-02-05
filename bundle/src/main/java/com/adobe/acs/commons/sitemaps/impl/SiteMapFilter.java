package com.adobe.acs.commons.sitemaps.impl;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;
@SlingFilter(
        scope = SlingFilterScope.REQUEST,
        description = "ACS Sitemap redirection filter",
        order = -5000,
        metatype = false
        )
public class SiteMapFilter implements Filter {

    @Override
    public void destroy() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain chain) throws IOException, ServletException {
      HttpServletRequest request = (HttpServletRequest) servletRequest;
      if("GET".equals(request.getMethod())&&request.getRequestURI().equals("/sitemap.xml")){
         HttpServletResponse response = (HttpServletResponse) servletResponse;
         RequestDispatcher dispatcher = request.getRequestDispatcher("/bin/acs/sitemap.xml");
         dispatcher.forward(request, response);
      }else{
          chain.doFilter(servletRequest , servletResponse);
      }
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // TODO Auto-generated method stub
        
    }

}
