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
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BadMd5VersionedClientLibsFilterTest {

  public BadMd5VersionedClientLibsFilterTest() throws UnsupportedEncodingException {
  }

  private final byte[] BYTES = "test".getBytes("UTF-8");
  private final java.io.InputStream INPUTSTREAM = new ByteArrayInputStream(BYTES);
  private final String INPUTSTREAM_MD5 = DigestUtils.md5Hex(BYTES);

  @Mock
  private HtmlLibraryManager htmlLibraryManager;

  @InjectMocks
  private BadMd5VersionedClientLibsFilter sut;

  @Mock
  private SlingHttpServletRequest slingRequest;

  @Mock
  private SlingHttpServletResponse slingResponse;

  @Mock
  private GenericCacheMBean versionedClientLibsTransformerFactory;

  @Mock
  private FilterChain filterChain;

  @Test
  public void init() throws Exception {
    sut.init(null);
  }

  @Test
  public void doFilter_nonJSCSS() throws Exception {
    when(slingRequest.getRequestURI()).thenReturn("/some_other/uri.html");

    sut.doFilter(slingRequest, slingResponse, filterChain);

    verifyNothingHappened();
  }

  @Test
  public void doFilter_notFoundInCache_md5Match() throws Exception {
    when(slingRequest.getRequestURI()).thenReturn("/etc/clientlibs/some.min." + INPUTSTREAM_MD5 + ".js");
    when(versionedClientLibsTransformerFactory.getCacheEntry("/etc/clientlibs/some.js")).thenReturn("Invalid cache key parameter.");

    HtmlLibrary library = mock(HtmlLibrary.class);
    when(library.getInputStream()).thenReturn(
      INPUTSTREAM
    );
    when(library.getLibraryPath()).thenReturn("/etc/clientlibs/some.js");
    when(htmlLibraryManager.getLibrary(slingRequest)).thenReturn(library);

    sut.doFilter(slingRequest, slingResponse, filterChain);

    verifyNo404();
  }

  @Test
  public void doFilter_notFoundInCache_md5MisMatch() throws Exception {
    when(slingRequest.getRequestURI()).thenReturn("/etc/clientlibs/some.min.foobar.js");
    when(versionedClientLibsTransformerFactory.getCacheEntry("/etc/clientlibs/some.js")).thenReturn("Invalid cache key parameter.");

    HtmlLibrary library = mock(HtmlLibrary.class);
    when(library.getInputStream()).thenReturn(
      INPUTSTREAM
    );
    when(library.getLibraryPath()).thenReturn("/etc/clientlibs/some.js");
    when(htmlLibraryManager.getLibrary(slingRequest)).thenReturn(library);

    sut.doFilter(slingRequest, slingResponse, filterChain);

    verify404();
  }

  @Test
  public void doFilter_notFoundInCache_NoClientLib() throws Exception {
    when(slingRequest.getRequestURI()).thenReturn("/etc/clientlibs/some.min.foobar.js");
    when(versionedClientLibsTransformerFactory.getCacheEntry("/etc/clientlibs/some.js")).thenReturn("Invalid cache key parameter.");

    when(htmlLibraryManager.getLibrary(slingRequest)).thenReturn(null);

    sut.doFilter(slingRequest, slingResponse, filterChain);

    verify404();
  }

  @Test
  public void doFilter_foundInCache_md5Match() throws Exception {
    when(slingRequest.getRequestURI()).thenReturn("/etc/clientlibs/some.min." + INPUTSTREAM_MD5 + ".js");
    when(versionedClientLibsTransformerFactory.getCacheEntry("/etc/clientlibs/some.js")).thenReturn(INPUTSTREAM_MD5);

    sut.doFilter(slingRequest, slingResponse, filterChain);

    verifyZeroInteractions(htmlLibraryManager);
    verifyNo404();
  }

  @Test
  public void doFilter_foundInCache_md5MisMatch() throws Exception {
    when(slingRequest.getRequestURI()).thenReturn("/etc/clientlibs/some.min.foobar.js");
    when(versionedClientLibsTransformerFactory.getCacheEntry("/etc/clientlibs/some.js")).thenReturn(INPUTSTREAM_MD5);

    sut.doFilter(slingRequest, slingResponse, filterChain);

    verify404();
  }

  @Test
  public void destroy() throws Exception {
    sut.destroy();
  }

  private void verifyNothingHappened() throws IOException, ServletException {
    verifyZeroInteractions(htmlLibraryManager);
    verifyNo404();
  }

  private void verifyNo404() throws IOException, ServletException {
    verify(filterChain).doFilter(slingRequest, slingResponse);
    verify(slingResponse, never()).sendError(anyInt());
  }

  private void verify404() throws IOException, ServletException {
    verify(filterChain, never()).doFilter(slingRequest, slingResponse);
    verify(slingResponse).sendError(HttpServletResponse.SC_NOT_FOUND);
  }

}
