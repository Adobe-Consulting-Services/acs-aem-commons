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
package com.adobe.acs.commons.filefetch.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.HttpURLConnection;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.acs.commons.filefetch.FileFetchConfiguration;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

import io.wcm.testing.mock.aem.junit.AemContext;

public class FileFetchImplTest {

  @Rule
  public final AemContext context = new AemContext();
  private FileFetcherImpl fileFetch;

  @Before
  public void init() throws LoginException {
    fileFetch = new FileFetcherImpl() {
      protected HttpURLConnection openConnection() throws IOException {

        HttpURLConnection huc = Mockito.mock(HttpURLConnection.class);
        if ("https://www.danklco.com/me.png".equals(config.remoteUrl())) {
          Mockito.when(huc.getResponseCode()).thenReturn(200);
          Mockito.when(huc.getInputStream()).thenReturn(new ByteArrayInputStream("Hello World".getBytes()));
        } else if ("https://www.perficientdigital.com/logo.png".equals(config.remoteUrl())) {
          Mockito.when(huc.getResponseCode()).thenReturn(304);
        } else {
          Mockito.when(huc.getResponseCode()).thenReturn(400);
        }
        Mockito.when(huc.getHeaderField("Last-Modified")).thenReturn("1970-01-01");
        return huc;
      }
    };

    ResourceResolverFactory factory = Mockito.mock(ResourceResolverFactory.class);
    Mockito.when(factory.getServiceResourceResolver(Mockito.any())).thenReturn(context.resourceResolver());
    fileFetch.setFactory(factory);

    fileFetch.setReplicator(Mockito.mock(Replicator.class));
  }

  @Test
  public void testFetch() throws IOException, ReplicationException {
    fileFetch.activate(new FileFetchConfiguration() {

      @Override
      public Class<? extends Annotation> annotationType() {
        return null;
      }

      @Override
      public String damPath() {
        return "/content/dam/an-asset.png";
      }

      @Override
      public String[] headers() {
        return new String[] { "Hi=123" };
      }

      @Override
      public String mimeType() {
        return "image/png";
      }

      @Override
      public String remoteUrl() {
        return "https://www.danklco.com/me.png";
      }

      @Override
      public String scheduler_expression() {
        return "* * * * *";
      }

      @Override
      public int[] validResponseCodes() {
        return new int[] { 200 };
      }

      @Override
      public int timeout() {
        return 5000;
      }

    });

    assertNull(fileFetch.getLastException());
    assertTrue(fileFetch.isLastJobSucceeded());
    assertEquals("1970-01-01",fileFetch.getLastModified());

    fileFetch.updateFile();

    assertNull(fileFetch.getLastException());
    assertTrue(fileFetch.isLastJobSucceeded());
  }
  

  @Test
  public void testFetchNoUpdate() throws IOException, ReplicationException {
    fileFetch.activate(new FileFetchConfiguration() {

      @Override
      public Class<? extends Annotation> annotationType() {
        return null;
      }

      @Override
      public String damPath() {
        return "/content/dam/an-asset.png";
      }

      @Override
      public String[] headers() {
        return new String[] { "Hi=123" };
      }

      @Override
      public String mimeType() {
        return "image/png";
      }

      @Override
      public String remoteUrl() {
        return "https://www.perficientdigital.com/logo.png";
      }

      @Override
      public String scheduler_expression() {
        return "* * * * *";
      }

      @Override
      public int[] validResponseCodes() {
        return new int[] { 200 };
      }

      @Override
      public int timeout() {
        return 5000;
      }


    });

    assertNull(fileFetch.getLastException());
    assertTrue(fileFetch.isLastJobSucceeded());
    assertEquals(null,fileFetch.getLastModified());
  }

  @Test
  public void testBadUrl() throws IOException, ReplicationException {

    fileFetch.activate(new FileFetchConfiguration() {

      @Override
      public Class<? extends Annotation> annotationType() {
        return null;
      }

      @Override
      public String damPath() {
        return "/content/dam/an-asset.png";
      }

      @Override
      public String[] headers() {
        return new String[] { "Hi=123" };
      }

      @Override
      public String mimeType() {
        return "image/png";
      }

      @Override
      public String remoteUrl() {
        return "https://www.adobe.com/logo.png";
      }

      @Override
      public String scheduler_expression() {
        return "* * * * *";
      }

      @Override
      public int[] validResponseCodes() {
        return new int[] { 200 };
      }

      @Override
      public int timeout() {
        return 5000;
      }

    });

    assertNotNull(fileFetch.getLastException());
    assertFalse(fileFetch.isLastJobSucceeded());

  }

}
