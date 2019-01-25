/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.etag.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.servlets.annotations.SlingServletFilter;
import org.apache.sling.servlets.annotations.SlingServletFilterScope;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.etag.impl.EtagMessageDigestServletFilter.Config;
import com.adobe.acs.commons.util.BufferedServletResponse.ResponseWriteMethod;
import com.adobe.acs.commons.util.BufferedSlingHttpServletResponse;
import com.google.common.io.BaseEncoding;

/** Generates the ETag response header from a message digest of the response. This header is supposed to be cached also on the dispatcher! */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@SlingServletFilter(scope = SlingServletFilterScope.REQUEST)
@Designate(ocd = Config.class)
public class EtagMessageDigestServletFilter implements Filter {

    @ObjectClassDefinition(name = "ACS AEM Commons - Digest-based ETag Servlet Filter", description = "Sets an ETag response header based on a message digest from the response's content and optionally its other headers. Enabling it increases the memory consumption as the full response need to be buffered before being sent to the client!")
    @interface Config {
        @AttributeDefinition(description = "If this filter should not be active, rather try to delete this config. Only in cases where this cannot be easily accomplished uncheck this option to disable the filter.")
        boolean enabled() default true;

        @AttributeDefinition(name = "Message Digest Algorithm", description = "The message digest algorithm for calculating the ETag header. Must be one of the supported ones by the JRE (for Oracle JRE8 listed in https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#MessageDigest).")
        String messageDigestAlgorithm() default "MD5";

        @AttributeDefinition(name = "Overwrite existing ETag header", description = "If this is set a previously set ETag header will be disregarded and overwritten by this filter. Otherwise the original ETag is used.")
        boolean overwrite() default false;

        @AttributeDefinition(name = "Service Ranking", description = "Indication of where to place the filter in the filter chain. The higher the number the earlier in the filter chain. This value may span the whole range of integer values. Two filters with equal service.ranking property value (explicitly set or default value of zero) will be ordered according to their service.id service property as described in section 5.2.5, Service Properties, of the OSGi Core Specification R 4.2.")
        int service_ranking() default Integer.MAX_VALUE;

        @AttributeDefinition(name = "Pattern", description = "Restricts the filter to paths that match the supplied regular expression. Requires Sling Engine 2.4.0 or newer.")
        String sling_filter_pattern() default ".*";

        @AttributeDefinition(name = "Consider Response Headers", description = "If checked will also include the existing response headers for the digest calculation, i.e. different headers lead to different ETags. That is usually intended as you cannot send arbitrary response headers with a 304 response (https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.5).")
        boolean considerResponseHeaders() default true;

        @AttributeDefinition(name = "Ignored Response Headers", description = "The header names (case-insensitive) which should in no case be considered for the digest calculation as they are considered also for a 304 response (compare with https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.5).")
        String[] ignoredResponseHeaders() default {"Date", "Cache-Control", "Expires",  "Vary"};
        
        @AttributeDefinition(name = "Salt", description = "The (optional) salt is also taken into account for the message digest calculation. It is necessary to change that value whenever the response content or the response headers are now modified differently in a proxy instance between client and AEM (e.g. Dispatcher sets additional headers).")
        String salt();
    }

    private static final Logger log = LoggerFactory.getLogger(EtagMessageDigestServletFilter.class);

    private Config configuration;
    private Collection<String> ignoredHeaderNames;

    @Activate
    public void activate(Config configuration) {
        this.configuration = configuration;
        if (configuration.ignoredResponseHeaders() != null && configuration.ignoredResponseHeaders().length > 0) {
            ignoredHeaderNames = Arrays.asList(configuration.ignoredResponseHeaders()).stream().map(String::toLowerCase).collect(Collectors.toSet());
            // turn to lower case
        } else {
            ignoredHeaderNames = Collections.emptySet();
        };
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
       // no-op
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(response instanceof SlingHttpServletResponse)) {
            throw new IllegalStateException("Filter not properly registered as Sling Servlet Filter");
        }
        if (!configuration.enabled()) {
            log.debug("ETag filter not enabled");
            chain.doFilter(request, response);
        } else {
            try {
                // we already checked that this is a HTTP servlet response before
                SlingHttpServletResponse slingHttpServletResponse = (SlingHttpServletResponse)response;
                try (BufferedSlingHttpServletResponse bufferedResponse = new BufferedSlingHttpServletResponse((SlingHttpServletResponse) response)) {
                    chain.doFilter(request, bufferedResponse);
                    if (!configuration.overwrite() && slingHttpServletResponse.containsHeader(HttpConstants.HEADER_ETAG)) {
                        log.debug("Do not overwrite existing ETag header with value '{}'", slingHttpServletResponse.getHeader(HttpConstants.HEADER_ETAG));
                    } else {
                        if (slingHttpServletResponse.isCommitted()) {
                            log.error("Can not send ETag header because response is already committed, try to give this filter a higher ranking!");
                        } else {
                          String digest = calculateDigestFromResponse(bufferedResponse);
                          slingHttpServletResponse.setHeader(HttpConstants.HEADER_ETAG, digest);
                        }
                    }
                }
            } catch (NoSuchAlgorithmException e) {
                log.error("The algorithm configured for this servlet filter is invalid: " + configuration.messageDigestAlgorithm(), e);
            }
        }
    }

    private String calculateDigestFromResponse(BufferedSlingHttpServletResponse bufferedResponse) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest messageDigest;
        messageDigest = MessageDigest.getInstance(configuration.messageDigestAlgorithm());
        if (bufferedResponse.getWriteMethod() == ResponseWriteMethod.OUTPUTSTREAM) {
            messageDigest.update(bufferedResponse.getBufferedBytes());
        } else if (bufferedResponse.getWriteMethod() == ResponseWriteMethod.WRITER) {
            messageDigest.update(bufferedResponse.getBufferedString().getBytes(bufferedResponse.getCharacterEncoding()));
        }
   
        // consider header values as well?
        if (configuration.considerResponseHeaders()) {
            for (String name : bufferedResponse.getHeaderNames()) {
                String lowerCaseName = name.toLowerCase();
                if (!ignoredHeaderNames.contains(lowerCaseName)) {
                    String header = lowerCaseName + ":" + StringUtils.join(bufferedResponse.getHeaders(name), ',');
                    messageDigest.update(header.getBytes(StandardCharsets.US_ASCII));
                    log.debug("Considering header {} for the digest calculation", header);
                }
            }
        }
        if (!StringUtils.isEmpty(configuration.salt())) {
            log.debug("Considering salt {} for the digest calculation", configuration.salt());
            messageDigest.update(configuration.salt().getBytes(StandardCharsets.UTF_8));
        }
        byte[] digest = messageDigest.digest();
        String hexDigest = BaseEncoding.base16().lowerCase().encode(digest);
        log.debug("ETag based on {} digest of the response is {}", messageDigest.getAlgorithm(), hexDigest);
        return hexDigest;
    }

    @Override
    public void destroy() {
        // no-op
    }

}
