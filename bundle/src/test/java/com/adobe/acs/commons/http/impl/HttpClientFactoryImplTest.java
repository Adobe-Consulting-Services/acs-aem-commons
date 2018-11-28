/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.http.impl;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockserver.model.HttpRequest.*;
import static org.mockserver.model.HttpResponse.*;

import com.adobe.acs.commons.http.JsonObjectResponseHandler;
import com.google.gson.JsonObject;
import junitx.util.PrivateAccessor;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;

import java.util.HashMap;
import java.util.Map;

public class HttpClientFactoryImplTest {

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);

    private MockServerClient mockServerClient;

    private HttpClientFactoryImpl impl;

    private Map<String, Object> config;

    private String username;

    private String password;

    @Before
    public void setup() throws Exception {
        config = new HashMap<String, Object>();
        username = RandomStringUtils.randomAlphabetic(5);
        password = RandomStringUtils.randomAlphabetic(6);
        final String authHeaderValue = Base64.encodeBase64String((username + ":" + password).getBytes());

        config.put("hostname", "localhost");
        config.put("port", mockServerRule.getPort().intValue());

        mockServerClient.when(
                request().withMethod("GET").withPath("/anon")
        ).respond(
                response().withStatusCode(200).withBody("OK")
        );
        mockServerClient.when(
                request().withMethod("GET").withPath("/anonJson")
        ).respond(
                response().withStatusCode(200).withBody("{ 'foo' : 'bar' }")
        );
        mockServerClient.when(
                request().withMethod("GET").withPath("/auth")
                        .withHeader("Authorization", "Basic " + authHeaderValue)
        ).respond(
                response().withStatusCode(200).withBody("OK")
        );
        impl = new HttpClientFactoryImpl();
        PrivateAccessor.setField(impl, "httpClientBuilderFactory", new HttpClientBuilderFactory() {
            @Override
            public HttpClientBuilder newBuilder() {
                return HttpClients.custom();
            }
        });
    }

    @Test
    public void testAnonymousGet() throws Exception {
        impl.activate(config);

        Request get = impl.get("/anon");
        Executor exec = impl.getExecutor();
        String str = exec.execute(get).handleResponse(new BasicResponseHandler());
        assertThat(str, is("OK"));
    }

    @Test
    public void testJsonGet() throws Exception {
        impl.activate(config);

        Request get = impl.get("/anonJson");
        Executor exec = impl.getExecutor();
        JsonObject jsonObject = exec.execute(get).handleResponse(new JsonObjectResponseHandler()).getAsJsonObject();
        assertThat(jsonObject.has("foo"), is(true));
        assertThat(jsonObject.get("foo").getAsString(), is("bar"));
    }

    @Test
    public void testAuthenticatedGet() throws Exception {
        config.put("username", username);
        config.put("password", password);
        impl.activate(config);

        Request get = impl.get("/auth");
        Executor exec = impl.getExecutor();
        String str = exec.execute(get).handleResponse(new BasicResponseHandler());
        assertThat(str, is("OK"));
    }

    @Test
    public void testDisableSSLCertCheck() throws Exception {
        // this test doesn't actually test anything, but at least ensures that the SSL
        // initialization code doesn't throw exceptions
        config.put("use.ssl", true);
        config.put("disable.certificate.check", true);
        impl.activate(config);
    }

}