/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.sitemaps.impl;

import static com.adobe.acs.commons.sitemaps.impl.SiteMapConstants.SERVLET_PATH;
import static com.adobe.acs.commons.sitemaps.impl.SiteMapConstants.SERVLET_REQUEST_METHOD;
import static com.adobe.acs.commons.sitemaps.impl.SiteMapConstants.SERVLET_URL_EXTENSION;
import static com.adobe.acs.commons.sitemaps.impl.SiteMapConstants.SITEMAP_REQUEST_URL;

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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

@Component(label = "ACS AEM Commons - Sitemap Redirection Filter", description = "ACS AEM Commons -  Sitemap redirection filter" , policy = ConfigurationPolicy.REQUIRE)
@Service(Filter.class)
@Properties({
        @Property(name = "filter.scope", value = "REQUEST", propertyPrivate = true),
        @Property(name = "filter.order", intValue = -5000, propertyPrivate = true)
})
public class SiteMapFilter implements Filter {

    @Override
    public void destroy() {
        
    }

    
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain chain) throws IOException, ServletException {
      HttpServletRequest request = (HttpServletRequest) servletRequest;
      if(isSiteMapRequested(request)){
         HttpServletResponse response = (HttpServletResponse) servletResponse;
         RequestDispatcher dispatcher = request.getRequestDispatcher(getSiteMapServletURL());
         dispatcher.forward(request, response);
      }else{
          chain.doFilter(servletRequest , servletResponse);
      }
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        
    }

    private boolean isSiteMapRequested(HttpServletRequest request){
        return (SERVLET_REQUEST_METHOD.equals(request.getMethod())&&request.getRequestURI().equals(SITEMAP_REQUEST_URL));
    }
    private String getSiteMapServletURL(){
        return SERVLET_PATH+"."+SERVLET_URL_EXTENSION;
    }
}
