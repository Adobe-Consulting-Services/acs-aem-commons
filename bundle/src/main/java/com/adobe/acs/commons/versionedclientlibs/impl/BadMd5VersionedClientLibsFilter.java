/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2017 Adobe
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
package com.adobe.acs.commons.versionedclientlibs.impl;

import com.adobe.acs.commons.util.impl.GenericCacheMBean;
import com.day.cq.widget.HtmlLibrary;
import com.day.cq.widget.HtmlLibraryManager;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@SlingFilter(
  label = "ACS Commons Versioned ClientLibs Bad MD5 filter",
  description = "Sends a HTTP 404 when the MD5 sum of the requested clientlib doesn't match the incoming request. This to avoid bad caching upstream.",
  generateComponent = true, // True if you want to leverage activate/deactivate or manage its OSGi life-cycle
  generateService = true, // True; required for Sling Filters
  order = 0, /* The smaller the number, the earlier in the Filter chain (can go negative);
  defaults to Integer.MAX_VALUE which push it at the end of the chain;
  check http://localhost:4502/system/console/status-slingfilter for values */
  scope = SlingFilterScope.REQUEST) // REQUEST, INCLUDE, FORWARD, ERROR, COMPONENT (REQUEST, INCLUDE, COMPONENT)
public class BadMd5VersionedClientLibsFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(BadMd5VersionedClientLibsFilter.class);

  @Reference
  private HtmlLibraryManager libraryManager;

  @Reference(target = "(pipeline.type=versioned-clientlibs)")
  private GenericCacheMBean versionedClientLibsTransformerFactory;

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    // do nothing
  }

  @Override
  public void doFilter(
    final ServletRequest request,
    final ServletResponse response,
    final FilterChain filterChain) throws IOException, ServletException {
    final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
    final SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;
    String uri = slingRequest.getRequestURI();
    if (uri.endsWith(".js") || uri.endsWith(".css")) {
      String cleanedUri = Md5UriUtil.cleanURI(uri);
      String md5FromCache = getCacheEntry(cleanedUri);
      if (md5FromCache == null) {
        // something went bad during JMX call, allow it to pass
        LOGGER.warn("Failed to fetch data from Versioned ClientLibs cache, allowing {} to pass", uri);
        filterChain.doFilter(request, response);
      } else if ("Invalid cache key parameter.".equals(md5FromCache)) {
        // we did not find the such a file in the cache
        // AEM could just be started and there is no version in the cache, we should calculate it here before continuing
        String md5FromLibrary = calculateMd5(slingRequest);
        doMatchWithMd5(filterChain, request, slingResponse, uri, cleanedUri, md5FromLibrary);
      } else {
        // the file is in the cache, compare the md5 from cache with the one in the request
        doMatchWithMd5(filterChain, request, slingResponse, uri, cleanedUri, md5FromCache);
      }
    } else {
      // this is not a javascript/css request
      filterChain.doFilter(request, response);
    }
  }

  @Nonnull
  private String calculateMd5(final SlingHttpServletRequest slingRequest) throws IOException {
    HtmlLibrary library = libraryManager.getLibrary(slingRequest);
    if (library == null) {
      LOGGER.debug("No ClientLib found for {}", slingRequest.getRequestURI());
      return "lib-not-found";
    } else {
      LOGGER.info("No match in cache, calculating MD5 for {}", library.getLibraryPath());
      return DigestUtils.md5Hex(library.getInputStream());
    }
  }

  private void doMatchWithMd5(
    final FilterChain filterChain, final ServletRequest request,
    final SlingHttpServletResponse slingResponse,
    final String uri,
    final String cleanedUri,
    final String expectedMd5) throws IOException, ServletException {
    String md5FromRequest = Md5UriUtil.getMD5FromURI(uri);
    if (expectedMd5.equalsIgnoreCase(md5FromRequest)) {
      // both are equal, allow it to pass
      LOGGER.debug("MD5 equals for '{}' in Versioned ClientLibs cache, allowing {} to pass", cleanedUri, uri);
      filterChain.doFilter(request, slingResponse);
    } else {
      // they differ, block it
      LOGGER.info("MD5 differs for '{}' in Versioned ClientLibs cache, sending {} for {}",
        new Object[]{
          cleanedUri,
          HttpServletResponse.SC_NOT_FOUND,
          uri
        }
      );
      slingResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @Nullable
  private String getCacheEntry(final String cleanedUri) {
    try {
      return versionedClientLibsTransformerFactory.getCacheEntry(cleanedUri);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public void destroy() {
    // do nothing
  }
}
