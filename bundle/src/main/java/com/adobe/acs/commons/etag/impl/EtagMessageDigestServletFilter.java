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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.engine.EngineConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.etag.impl.EtagMessageDigestServletFilter.Config;
import com.adobe.acs.commons.util.BufferedServletOutput.ResponseWriteMethod;
import com.adobe.acs.commons.util.BufferedSlingHttpServletResponse;
import com.google.common.io.BaseEncoding;

/** Generates the ETag response header from a message digest of the response. This header is supposed to be cached also on the
 * dispatcher! */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, property = EngineConstants.SLING_FILTER_SCOPE + "="
        + EngineConstants.FILTER_SCOPE_REQUEST)
@Designate(ocd = Config.class)
public class EtagMessageDigestServletFilter implements Filter {

    private static final String WEAK_TAG_PREFIX = "W/";

    @ObjectClassDefinition(name = "ACS AEM Commons - Digest-based ETag Servlet Filter", description = "Sets an ETag response header based on a message digest from the response's content and optionally its' other headers. Enabling it increases the memory consumption as the full response need to be buffered before being sent to the client!")
    public @interface Config {
        @AttributeDefinition(name = "Enabled", description = "If this filter should not be active, rather try to delete this config. Only in cases where this cannot be easily accomplished uncheck this option to disable the filter.")
        boolean enabled() default true;

        @AttributeDefinition(name = "Message Digest Algorithm", description = "The message digest algorithm for calculating the ETag header. Must be one of the supported ones by the JRE (for Oracle JRE8 listed in https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#MessageDigest).")
        String messageDigestAlgorithm() default "MD5";

        @AttributeDefinition(name = "Overwrite existing ETag header", description = "If this is set a previously set ETag header will be disregarded and overwritten by this filter. Otherwise the original ETag is used.")
        boolean overwrite() default false;

        @AttributeDefinition(name = "Service Ranking", description = "Indication of where to place the filter in the filter chain. The higher the number the earlier in the filter chain. This value may span the whole range of integer values. Two filters with equal service.ranking property value (explicitly set or default value of zero) will be ordered according to their service.id service property as described in section 5.2.5, Service Properties, of the OSGi Core Specification R 4.2.")
        int service_ranking() default Integer.MAX_VALUE;

        @AttributeDefinition(name = "Pattern", description = "Restricts the filter to paths that match the supplied regular expression. Requires Sling Engine 2.4.0 or newer.")
        String sling_filter_pattern() default ".*";

        @AttributeDefinition(name = "Consider Response Headers", description = "If checked will also include the existing response headers for the digest calculation, i.e. different headers lead to different ETags. That is usually intended as you cannot send arbitrary response headers with a 304 response (https://tools.ietf.org/html/rfc7232#section-4.1).")
        boolean considerResponseHeaders() default true;

        @AttributeDefinition(name = "Ignored Response Headers", description = "The header names (case-insensitive) which should in no case be considered for the digest calculation as they are considered also for a 304 response (compare with https://tools.ietf.org/html/rfc7232#section-4.1).")
        String[] ignoredResponseHeaders() default { "Date", "Cache-Control", "Expires", "Vary" };

        @AttributeDefinition(name = "Salt", description = "The (optional) salt is also taken into account for the message digest calculation. It is necessary to change that value whenever the response content or the response headers are now modified differently in a proxy instance between client and AEM (e.g. Dispatcher sets additional headers).")
        String salt();

        @AttributeDefinition(name = "Enabled for output streams", description = "If set to 'true' this will also calculate the ETag for response output streams (binary output) and not only for response writers (text output). Enabling this option might lead to heavy memory demands as the full output stream is then buffered (i.e. kept in memory) before being delivered to the client. Especially if you deliver large assets like videos from AEM you should not enable this option.")
        boolean enabledForOutputStream() default false;

        @AttributeDefinition(name = "Add as HTML comment", description = "If set to 'true' this filter will also emit a HTML comment at the very end of each HTML document exposing the ETag. This may be helpful to debug issues with stale HTML cache entries in case the ETag header is not properly propagated.")
        boolean addAsHtmlComment() default false;
    }

    private static final Logger log = LoggerFactory.getLogger(EtagMessageDigestServletFilter.class);

    private Config configuration;
    private Collection<String> ignoredHeaderNames;

    @Activate
    public void activate(Config configuration) {
        this.configuration = configuration;
        if (configuration.ignoredResponseHeaders() != null && configuration.ignoredResponseHeaders().length > 0) {
            // turn to lower case
            ignoredHeaderNames = Arrays.asList(configuration.ignoredResponseHeaders()).stream().map(String::toLowerCase)
                    .collect(Collectors.toSet());
        } else {
            ignoredHeaderNames = Collections.emptySet();
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // no-op
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(response instanceof SlingHttpServletResponse) || !(request instanceof SlingHttpServletRequest)) {
            throw new IllegalStateException("Filter not properly registered as Sling Servlet Filter");
        }
        if (configuration.enabled()) {
            log.debug("ETag filter enabled");

            // we already checked that this is a HTTP servlet response before
            SlingHttpServletResponse slingHttpServletResponse = (SlingHttpServletResponse) response;
            SlingHttpServletRequest slingHttpServletRequest = (SlingHttpServletRequest) request;

            // is this a GET/HEAD?
            if (slingHttpServletRequest.getMethod().equals(HttpConstants.METHOD_HEAD)
                    || slingHttpServletRequest.getMethod().equals(HttpConstants.METHOD_GET)) {
                log.debug("Method is GET or HEAD, calculating ETag...");
                doFilterWithMessageDigest(slingHttpServletRequest, slingHttpServletResponse, chain);
                return;
            } else {
                log.debug("Method neither GET or HEAD: {}, no ETag necessary!", slingHttpServletRequest.getMethod());
            }
        } else {
            log.debug("ETag filter disabled");
        }
        chain.doFilter(request, response);
    }

    private void doFilterWithMessageDigest(SlingHttpServletRequest slingHttpServletRequest, SlingHttpServletResponse slingHttpServletResponse,  FilterChain chain) throws IOException, ServletException {
        ByteArrayOutputStream outputStream = configuration.enabledForOutputStream() ? new ByteArrayOutputStream() : null;
        try (BufferedSlingHttpServletResponse bufferedResponse = new BufferedSlingHttpServletResponse(slingHttpServletResponse,
                new StringWriter(), outputStream)) {
            chain.doFilter(slingHttpServletRequest, bufferedResponse);
            if (!configuration.overwrite() && slingHttpServletResponse.containsHeader(HttpConstants.HEADER_ETAG)) {
                log.debug("Do not overwrite existing ETag header with value '{}'",
                        slingHttpServletResponse.getHeader(HttpConstants.HEADER_ETAG));
                return;
            }
            // was the response buffered?
            if (!configuration.enabledForOutputStream()
                    && bufferedResponse.getBufferedServletOutput().getWriteMethod() == ResponseWriteMethod.OUTPUTSTREAM) {
                log.debug("Can not calculate message digest as response was written via output stream which was not buffered.");
                return;
            }
            if (slingHttpServletResponse.isCommitted()) {
                log.error("Can not send ETag header because response is already committed, try to give this filter a higher ranking!");
                return;
            }

            try {
                String digest = calculateDigestFromResponse(bufferedResponse);
                slingHttpServletRequest.getRequestProgressTracker().log("ETag from digest calculated with {0}: {1}",
                        configuration.messageDigestAlgorithm(), digest);
                slingHttpServletResponse.setHeader(HttpConstants.HEADER_ETAG, "\"" + digest + "\"");
                if (isUnmodified(slingHttpServletRequest.getHeaders(HttpHeaders.IF_NONE_MATCH), digest)) {
                    log.debug(
                            "Digest is equal to one of the given ETags in the If-None-Match request header, returning empty response with a 304");
                    bufferedResponse.resetBuffer();
                    slingHttpServletResponse.setStatus(HttpStatus.SC_NOT_MODIFIED);
                    return;
                }
                if (configuration.addAsHtmlComment()
                        && bufferedResponse.getBufferedServletOutput().getWriteMethod() == ResponseWriteMethod.WRITER
                        && slingHttpServletResponse.getContentType() != null
                        && slingHttpServletResponse.getContentType().startsWith("text/html")) {
                    bufferedResponse.getWriter().println(String.format("%n<!-- ETag: %s -->", digest));
                }
            } catch (NoSuchAlgorithmException e) {
                log.error("The algorithm configured for this servlet filter is invalid: " + configuration.messageDigestAlgorithm(), e);
            }
        }
    }

    /** Handles conditional requests like outlined in RFC7232.
     * 
     * @param slingHttpServletRequest
     * @param slingHttpServletResponse
     * @return {@code true} in case this was a conditional request and the response was not modified, otherwise {@code false}
     * @see <a href="// https://tools.ietf.org/html/rfc7232#section-3.2">RFC7232</a> */
    static boolean isUnmodified(Enumeration<String> ifNoneMatchETags, String responseETag) {
        if (ifNoneMatchETags == null) {
            log.debug("Can not access request headers or no NoneMatchETags header given!");
            return false;
        }
        while (ifNoneMatchETags.hasMoreElements()) {
            String ifNoneMatchETag = ifNoneMatchETags.nextElement();
            if (ifNoneMatchETag.equals("*")) {
                return true;
            }
            // strip weak tag prefix
            if (ifNoneMatchETag.startsWith(WEAK_TAG_PREFIX)) {
                ifNoneMatchETag = ifNoneMatchETag.substring(WEAK_TAG_PREFIX.length());
            }
            // remove double quotes (first and last value character)
            if (!ifNoneMatchETag.startsWith("\"") || !ifNoneMatchETag.endsWith("\"")) {
                // ignoring invalid etag
                log.debug("Ignoring invalid ETag not starting and ending with quotes: {}", ifNoneMatchETag);
                continue;
            }
            ifNoneMatchETag = ifNoneMatchETag.substring(1, ifNoneMatchETag.length() - 1);
            if (ifNoneMatchETag.equals(responseETag)) {
                return true;
            }
        }
        return false;
    }

    String calculateDigestFromResponse(BufferedSlingHttpServletResponse bufferedResponse)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest messageDigest = MessageDigest.getInstance(configuration.messageDigestAlgorithm());
        if (bufferedResponse.getBufferedServletOutput().getWriteMethod() == ResponseWriteMethod.OUTPUTSTREAM) {
            messageDigest.update(bufferedResponse.getBufferedServletOutput().getBufferedBytes());
        } else if (bufferedResponse.getBufferedServletOutput().getWriteMethod() == ResponseWriteMethod.WRITER) {
            String charsetName = bufferedResponse.getCharacterEncoding();
            if (charsetName == null) {
                charsetName = StandardCharsets.ISO_8859_1.name();
            }
            messageDigest.update(bufferedResponse.getBufferedServletOutput().getBufferedString().getBytes(charsetName));
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
