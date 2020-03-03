package com.adobe.acs.commons.granite.ui.components.include;

import org.apache.commons.collections.MapUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.engine.EngineConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import javax.annotation.CheckForNull;
import javax.servlet.*;
import java.io.IOException;

@Component(
        service = Filter.class,
        configurationPolicy = ConfigurationPolicy.OPTIONAL,
        property= {
                EngineConstants.SLING_FILTER_SCOPE+"="+ EngineConstants.FILTER_SCOPE_INCLUDE,
                "sling.filter.resourceTypes=" + IncludeDecoratorFilterImpl.RESOURCE_TYPE
        }
        
)
public class IncludeDecoratorFilterImpl implements Filter {

    static final String RESOURCE_TYPE = "acs-commons/granite/ui/components/include";
    static final String NAMESPACE = "namespace";
    static final String PARAMETERS = "parameters";
    public static final String PREFIX = "PREFIX_";
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {

   
        ValueMap parameters = ValueMap.EMPTY;

        if(servletRequest instanceof SlingHttpServletRequest){
            
            SlingHttpServletRequest request = (SlingHttpServletRequest) servletRequest;

            @CheckForNull Resource parameterResource = request.getResource().getChild(PARAMETERS);
            if(parameterResource != null){
                parameters = parameterResource.getValueMap();
            }

            Object existingNamespace = servletRequest.getAttribute(NAMESPACE);
            boolean hasExistingNamespace = existingNamespace != null;
            boolean hasNamespaceInParameters = parameters.containsKey(NAMESPACE);


            if(MapUtils.isNotEmpty(parameters)){
                parameters.forEach((key, object) -> {
                    servletRequest.setAttribute(PREFIX + key, object);
                });
            }

            if(hasNamespaceInParameters && hasExistingNamespace){
                servletRequest.setAttribute(NAMESPACE, existingNamespace + "/" + parameters.get(NAMESPACE).toString());
            }else if(hasNamespaceInParameters){
                servletRequest.setAttribute(NAMESPACE, parameters.get(NAMESPACE).toString());
            }

            chain.doFilter(servletRequest, servletResponse);

            if(MapUtils.isNotEmpty(parameters)){
                parameters.forEach((key, object) -> {
                    servletRequest.removeAttribute(PREFIX + key);
                });
            }

            if(existingNamespace != null){
                servletRequest.setAttribute(NAMESPACE, existingNamespace);
            }else{
                servletRequest.removeAttribute(NAMESPACE);
            }

            return;
            
        }
    
        chain.doFilter(servletRequest, servletResponse);
    }
    
    
    @Override
    public void destroy() {

    }
}
