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
package com.adobe.acs.commons.marketo.client.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiFunction;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicRequestLine;
import org.jetbrains.annotations.NotNull;
import org.apache.http.ProtocolVersion;

import com.adobe.acs.commons.marketo.client.MarketoApiException;

public class StaticResponseMarketoClient extends MarketoClientImpl {

    private String resourcePath;
    private Iterator<String> resourcePaths;

    public StaticResponseMarketoClient(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public StaticResponseMarketoClient(String[] resourcePaths) {
        this.resourcePaths = Arrays.asList(resourcePaths).iterator();
        if (this.resourcePaths.hasNext()) {
            resourcePath = this.resourcePaths.next();
        }
    }

    @Override
    protected <T> @NotNull T getApiResponse(@NotNull String url, String bearerToken,
            BiFunction<HttpGet, HttpResponse, ParsedResponse<T>> callback)
            throws MarketoApiException {
        InputStream is = StaticResponseMarketoClient.class.getResourceAsStream(resourcePath);
        if (resourcePaths != null && resourcePaths.hasNext()) {
            resourcePath = resourcePaths.next();
        }
        HttpGet req = mock(HttpGet.class);
        when(req.getRequestLine()).thenReturn(new BasicRequestLine("GET", url, new ProtocolVersion("http", 0, 0)));
        HttpResponse res = mock(HttpResponse.class);

        StatusLine status = mock(StatusLine.class);
        when(status.getStatusCode()).thenReturn(200);

        when(res.getStatusLine()).thenReturn(status);
        when(res.getEntity())
                .thenReturn(new InputStreamEntity(is));

        ParsedResponse<T> resp = callback.apply(req, res);
        if (resp.isSuccess()) {
            return resp.getResult();
        } else {
            throw resp.getException();
        }
    }
}
