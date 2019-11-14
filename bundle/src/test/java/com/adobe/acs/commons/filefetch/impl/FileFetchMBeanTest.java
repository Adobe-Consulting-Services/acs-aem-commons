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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collections;

import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.OpenDataException;

import org.apache.sling.api.resource.LoginException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.acs.commons.filefetch.FileFetchConfiguration;
import com.adobe.acs.commons.filefetch.FileFetcher;
import com.day.cq.replication.ReplicationException;

import io.wcm.testing.mock.aem.junit.AemContext;

public class FileFetchMBeanTest {

  @Rule
  public final AemContext context = new AemContext();
  private FileFetcher fileFetch;

  @Before
  public void init() throws LoginException {
    fileFetch = Mockito.mock(FileFetcher.class);
    Mockito.when(fileFetch.isLastJobSucceeded()).thenReturn(true);
    Mockito.when(fileFetch.getLastException()).thenReturn(null);

    Mockito.when(fileFetch.getConfig()).thenReturn(new FileFetchConfiguration() {

      @Override
      public Class<? extends Annotation> annotationType() {
        return null;
      }

      @Override
      public String damPath() {
        return "/content/dam";
      }

      @Override
      public String[] headers() {
        return new String[0];
      }

      @Override
      public String mimeType() {
        return "application/json";
      }

      @Override
      public String remoteUrl() {
        return "https://www.altavista.com";
      }

      @Override
      public String scheduler_expression() {
        return "* * * 8 * * * ";
      }

      @Override
      public int[] validResponseCodes() {
        return new int[] { 200 };
      }

      @Override
      public int timeout() {
        return 200;
      }
    });
  }

  @Test
  public void testMBean() throws IOException, ReplicationException, NotCompliantMBeanException, OpenDataException {
    FileFetchMBeanImpl mbean = new FileFetchMBeanImpl();
    mbean.setFetchers(Collections.singletonList(fileFetch));

    assertTrue(mbean.allSucceeded());
    assertNotNull(mbean.getFetchers());
    mbean.fetch(0);
  }

}
