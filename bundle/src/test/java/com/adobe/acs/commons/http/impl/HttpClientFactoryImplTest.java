/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.http.impl;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockserver.model.HttpRequest.*;
import static org.mockserver.model.HttpResponse.*;

import com.adobe.acs.commons.http.JsonObjectResponseHandler;
import com.google.gson.JsonObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class HttpClientFactoryImplTest {

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);

    private MockServerClient mockServerClient;

    private String username;

    private String password;

    @Before
    public void setup() throws Exception {
        username = RandomStringUtils.randomAlphabetic(5);
        password = RandomStringUtils.randomAlphabetic(6);
        final String authHeaderValue = Base64.encodeBase64String((username + ":" + password).getBytes());

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
    }

    protected HttpClientFactoryImpl createFactory() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        return createFactory(false, null, null);
    }

    protected HttpClientFactoryImpl createFactory(boolean isDisabledCertCheck, String username, String password) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        return new HttpClientFactoryImpl(false, "localhost", mockServerRule.getPort().intValue(),
                30000, 30000, isDisabledCertCheck, username, password,
                new HttpClientBuilderFactory() {
            @Override
            public HttpClientBuilder newBuilder() {
                return HttpClients.custom();
            }
        });
    }

    @Test
    public void testAnonymousGet() throws Exception {
        HttpClientFactoryImpl impl = createFactory();
        Request get = impl.get("/anon");
        Executor exec = impl.getExecutor();
        String str = exec.execute(get).handleResponse(new BasicResponseHandler());
        assertThat(str, is("OK"));
    }

    @Test
    public void testJsonGet() throws Exception {
        HttpClientFactoryImpl impl = createFactory();
        Request get = impl.get("/anonJson");
        Executor exec = impl.getExecutor();
        JsonObject jsonObject = exec.execute(get).handleResponse(new JsonObjectResponseHandler()).getAsJsonObject();
        assertThat(jsonObject.has("foo"), is(true));
        assertThat(jsonObject.get("foo").getAsString(), is("bar"));
    }

    @Test
    public void testAuthenticatedGet() throws Exception {
        HttpClientFactoryImpl impl = createFactory(false, username, password);

        Request get = impl.get("/auth");
        Executor exec = impl.getExecutor();
        String str = exec.execute(get).handleResponse(new BasicResponseHandler());
        assertThat(str, is("OK"));
    }

    @Test
    public void testDisableSSLCertCheck() throws Exception {
        // this test doesn't actually test anything, but at least ensures that the SSL
        // initialization code doesn't throw exceptions
        HttpClientFactoryImpl impl = createFactory(false, null, null);
    }

}