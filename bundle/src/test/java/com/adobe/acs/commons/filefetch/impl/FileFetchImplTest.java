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
import static org.mockito.Mockito.spy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

import io.wcm.testing.mock.aem.junit.AemContext;

public class FileFetchImplTest {

  @Rule
  public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);
  private FileFetcherImpl fileFetch;
  
  HttpURLConnection mockConnection;
  
  Replicator replicator;
  
  Map<String, Object> props = new HashMap<>();

  @Before
	public void init() throws IOException, Exception {

		replicator = Mockito.mock(Replicator.class);
		context.registerService(Replicator.class, replicator);

		ResourceResolverFactory rrf = context.getService(ResourceResolverFactory.class);
		context.registerService(ResourceResolverFactory.class, rrf);

		fileFetch = spy(new FileFetcherImpl());

		props.put("damPath", "/content/dam/an-asset.png");
		props.put("headers", new Object[] { "Hi=123" });
		props.put("mimeType", "image/png");
		props.put("scheduler.expression", "* * * * *");
		props.put("remoteUrl", "https://www.danklco.com/me.png");
		props.put("validResponseCodes", "200");
		props.put("timeout", "5000");

		mockConnection = Mockito.mock(HttpURLConnection.class);
		Mockito.when(mockConnection.getHeaderField("Last-Modified")).thenReturn("1970-01-01");
		Mockito.doReturn(mockConnection).when(fileFetch).openConnection();
	}

  @Test
  public void testFetch() throws IOException, ReplicationException {
	  
		Mockito.when(mockConnection.getResponseCode()).thenReturn(200);
		Mockito.when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream("Hello World".getBytes()));
		context.registerInjectActivateService(fileFetch, props);

		assertNull(fileFetch.getLastException());
		assertTrue(fileFetch.isLastJobSucceeded());
		assertEquals("1970-01-01", fileFetch.getLastModified());

		fileFetch.updateFile();

		assertNull(fileFetch.getLastException());
		assertTrue(fileFetch.isLastJobSucceeded());
  }
  

  @Test
  public void testFetchNoUpdate() throws IOException, ReplicationException {
	  
		Mockito.when(mockConnection.getResponseCode()).thenReturn(304);
		context.registerInjectActivateService(fileFetch, props);

		assertNull(fileFetch.getLastException());
		assertTrue(fileFetch.isLastJobSucceeded());
		assertEquals(null, fileFetch.getLastModified());
  }

  @Test
  public void testBadUrl() throws IOException, ReplicationException {
	  
		Mockito.when(mockConnection.getResponseCode()).thenReturn(400);
		context.registerInjectActivateService(fileFetch, props);

		assertNotNull(fileFetch.getLastException());
		assertFalse(fileFetch.isLastJobSucceeded());

  }

}
