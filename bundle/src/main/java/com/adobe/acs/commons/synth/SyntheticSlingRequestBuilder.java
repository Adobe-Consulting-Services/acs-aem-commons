/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

package com.adobe.acs.commons.synth;


import com.adobe.acs.commons.synth.impl.support.SyntheticSlingHttpServletRequest;
import com.adobe.acs.commons.synth.impl.support.CookieSupport;
import com.adobe.acs.commons.synth.impl.support.HeaderSupport;
import com.adobe.acs.commons.synth.impl.support.SyntheticRequestPathInfo;
import com.day.cq.wcm.api.WCMMode;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class SyntheticSlingRequestBuilder {

    private byte[] contentByteArray;
    private int remotePort;
    private String remoteUser;
    private String remoteAddress;

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

    public SyntheticSlingRequestBuilder withContent(byte[] byteArray){
        this.contentByteArray = byteArray;
        return this;
    }

    public SyntheticSlingRequestBuilder withPayload(byte[] content, String contentType) throws IOException {
        this.contentByteArray = content;
        this.contentType = contentType;
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

    public SyntheticSlingRequestBuilder withRemoteUser(String user){
        this.remoteUser = user;
        return this;
    }

    public SyntheticSlingRequestBuilder withRemoteAddress(String remoteAddress){
        this.remoteAddress = remoteAddress;
        return this;
    }

    public SyntheticSlingRequestBuilder withRemotePort(int port){
        this.remotePort = port;
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

        attributeMap.forEach(syntheticRequest::setAttribute);

        if(MapUtils.isNotEmpty(parameterMap)){
            syntheticRequest.setParameterMap(parameterMap);
        }else if(StringUtils.isNotBlank(queryString)){
            syntheticRequest.setQueryString(queryString);
        }else{
            syntheticRequest.setQueryString(StringUtils.EMPTY);
        }

        syntheticRequest.setLocale(locale != null ? locale : Locale.ENGLISH);
        syntheticRequest.setContent(contentByteArray);
        syntheticRequest.setMethod(method.name());
        syntheticRequest.setCharacterEncoding(characterEncoding);
        syntheticRequest.setContentType(contentType);
        syntheticRequest.setServerPort(serverPort);
        syntheticRequest.setScheme(scheme);
        syntheticRequest.setServerName(serverName);
        syntheticRequest.setRemotePort(remotePort);
        syntheticRequest.setRemoteUser(remoteUser);
        syntheticRequest.setRemoteAddr(remoteAddress);


        if(dispatcherFactory != null){
            syntheticRequest.setRequestDispatcherFactory(dispatcherFactory);
        }

        //bind wcm mode
        wcmMode.toRequest(syntheticRequest);

        return syntheticRequest;

    }

    
    
}
