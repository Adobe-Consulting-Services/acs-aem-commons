package com.adobe.acs.commons.synth;


import com.adobe.acs.commons.synth.impl.support.CookieSupport;
import com.adobe.acs.commons.synth.impl.support.HeaderSupport;
import com.adobe.acs.commons.synth.impl.support.SyntheticRequestPathInfo;
import com.adobe.acs.commons.synth.impl.support.SyntheticSlingHttpServletRequest;
import com.day.cq.wcm.api.WCMMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.servlet.http.Cookie;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class SyntheticSlingRequestBuilder {

    enum Method {
        GET,
        POST,
        PUT,
        DELETE,
        HEAD,
        OPTIONS,
        TRACE;
    }

    private static final String DEFAULT_EXTENSION = "html";


    private final ResourceResolver resourceResolver;
    private final Resource currentResource;
    private final CookieSupport cookieSupport = new CookieSupport();
    private final HeaderSupport headerSupport = new HeaderSupport();
    private final Map<String, Object> attributeMap = new HashMap<>();
    private final Map<String, String[]> parameterMap = new LinkedHashMap<>();

    private String queryString = StringUtils.EMPTY;
    private String contentType;
    private int serverPort;
    private String scheme;
    private String serverName;
    private String characterEncoding;
    private String resourcePath;
    private String selectorString;
    private String suffix;
    private String extension;
    private Locale locale;
    private WCMMode wcmMode = WCMMode.DISABLED;
    private Method method = Method.GET;

    private final SyntheticRequestDispatcherFactory dispatcherFactory;

    /**
     * Constructors are not public, we want people to use the factory instead of the constructor.
     * @see SyntheticSlingRequestBuilderFactory
     * @param dispatcherFactory
     * @param resourceResolver
     * @param currentResourcePath
     */
    SyntheticSlingRequestBuilder(SyntheticRequestDispatcherFactory dispatcherFactory, ResourceResolver resourceResolver, String currentResourcePath){
        this.dispatcherFactory = dispatcherFactory;
        this.resourceResolver = resourceResolver;
        this.currentResource = resourceResolver.getResource(currentResourcePath);
    }

    SyntheticSlingRequestBuilder(SyntheticRequestDispatcherFactory dispatcherFactory, ResourceResolver resourceResolver, Resource currentResource){
        this.dispatcherFactory = dispatcherFactory;
        this.resourceResolver = resourceResolver;
        this.currentResource = currentResource;
    }


    public SyntheticSlingRequestBuilder withLocale(Locale locale){
        this.locale = locale;
        return this;
    }


    public SyntheticSlingRequestBuilder withMethod(Method method){
        this.method = method;
        return this;
    }


    public SyntheticSlingRequestBuilder withWCMMode(WCMMode wcmMode){
        this.wcmMode = wcmMode;
        return this;
    }


    public SyntheticSlingRequestBuilder withResourcePath(String resourcePath){
        this.resourcePath = resourcePath;
        return this;
    }


    public SyntheticSlingRequestBuilder withQueryString(String queryString){
        this.queryString = queryString;
        return this;
    }


    public SyntheticSlingRequestBuilder withSelectorString(String selectorString){
        this.selectorString = selectorString;
        return this;
    }


    public SyntheticSlingRequestBuilder withSuffix(String suffix){
        this.suffix = suffix;
        return this;
    }


    public SyntheticSlingRequestBuilder withExtension(String extension){
        this.extension = extension;
        return this;
    }


    public SyntheticSlingRequestBuilder withHeader(String name, String value){
        this.headerSupport.addHeader(name,value);
        return this;
    }



    public SyntheticSlingRequestBuilder withAttribute(String name, Object value){
        this.attributeMap.put(name,value);
        return this;
    }


    public SyntheticSlingRequestBuilder withAttributes(Map<String, Object> attributeMap){
        this.attributeMap.putAll(attributeMap);
        return this;
    }


    public SyntheticSlingRequestBuilder withDateHeader(String name, long value){
        this.headerSupport.addDateHeader(name,value);
        return this;
    }


    public SyntheticSlingRequestBuilder withIntHeader(String name, int value){
        this.headerSupport.addIntHeader(name,value);
        return this;
    }


    public SyntheticSlingRequestBuilder withCookie(Cookie cookie){
        this.cookieSupport.addCookie(cookie);
        return this;
    }


    public SyntheticSlingRequestBuilder withContentType(String value){
        this.contentType = value;
        return this;
    }


    public SyntheticSlingRequestBuilder withScheme(String value){
        this.scheme = value;
        return this;
    }


    public SyntheticSlingRequestBuilder withServerName(String value){
        this.serverName = value;
        return this;
    }


    public SyntheticSlingRequestBuilder withCharacterEncoding(String value){
        this.characterEncoding = value;
        return this;
    }


    public SyntheticSlingRequestBuilder withServerPort(int port){
        this.serverPort = port;
        return this;
    }



    public SyntheticSlingRequestBuilder withParameter(String key, String[] value){
        this.parameterMap.put(key,value);
        return this;
    }


    public SyntheticSlingRequestBuilder withParameters(Map<String, String[]> parameters){
        this.parameterMap.putAll(parameters);
        return this;
    }


    public SyntheticSlingRequestBuilder withPathInfoCopied(RequestPathInfo requestPathInfo){
        this.resourcePath = requestPathInfo.getResourcePath();
        this.selectorString = requestPathInfo.getSelectorString();
        this.suffix = requestPathInfo.getSuffix();
        this.extension = requestPathInfo.getExtension();
        return this;
    }



    public SlingHttpServletRequest build(){

        SyntheticRequestPathInfo syntheticRequestPathInfo = new SyntheticRequestPathInfo(resourceResolver);

        syntheticRequestPathInfo.setResourcePath(resourcePath != null ? resourcePath : currentResource.getPath());
        syntheticRequestPathInfo.setSelectorString(selectorString);
        syntheticRequestPathInfo.setSuffix(suffix);
        syntheticRequestPathInfo.setExtension(extension != null? extension : DEFAULT_EXTENSION);

        SyntheticSlingHttpServletRequest syntheticRequest = new SyntheticSlingHttpServletRequest(resourceResolver, currentResource, syntheticRequestPathInfo, cookieSupport, headerSupport );
        syntheticRequest.setQueryString(queryString);

        attributeMap.forEach(syntheticRequest::setAttribute);
        syntheticRequest.setParameterMap(parameterMap);

        syntheticRequest.setLocale(locale != null ? locale : Locale.ENGLISH);
        syntheticRequest.setMethod(method.name());
        syntheticRequest.setCharacterEncoding(characterEncoding);
        syntheticRequest.setContentType(contentType);
        syntheticRequest.setServerPort(serverPort);
        syntheticRequest.setScheme(scheme);
        syntheticRequest.setServerName(serverName);

        if(dispatcherFactory != null){
            syntheticRequest.setRequestDispatcherFactory(dispatcherFactory);
        }

        //bind wcm mode
        wcmMode.toRequest(syntheticRequest);

        return syntheticRequest;

    }

    
    
}
