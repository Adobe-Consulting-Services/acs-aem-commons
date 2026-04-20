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
package com.adobe.acs.commons.marketo.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.junit.Test;

import com.adobe.acs.commons.marketo.client.MarketoApiException;

public class MarketoAPIExceptionTest {

    @Test
    public void canHandleNullRequestResponse() {
        MarketoApiException ex = new MarketoApiException("Bad", null, null);
        assertEquals("Bad	REQUEST{null}	RESPONSE{Status Code: -1, Reason Phrase: null, Response Body: null}",
                ex.getMessage());

        assertNull(ex.getReasonString());
        assertNull(ex.getRequestLine());
        assertNull(ex.getResponseBody());
        assertEquals(-1, ex.getStatusCode());
    }

    @Test
    public void canParseValues() throws UnsupportedEncodingException {
        HttpRequestBase request = new HttpGet("http://www.marketo.com");

        HttpResponse response = mock(HttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(418);
        when(statusLine.getReasonPhrase()).thenReturn("I'm a Teapot");
        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity()).thenReturn(new StringEntity("Short and Stout"));

        MarketoApiException ex = new MarketoApiException("Bad", request, response);
        assertEquals("Bad	REQUEST{GET http://www.marketo.com HTTP/1.1}	RESPONSE{Status Code: 418, Reason Phrase: I'm a Teapot, Response Body: Short and Stout}",
                ex.getMessage());

        assertEquals("I'm a Teapot", ex.getReasonString());
        assertEquals("GET http://www.marketo.com HTTP/1.1", ex.getRequestLine());
        assertEquals("Short and Stout", ex.getResponseBody());
        assertEquals(418, ex.getStatusCode());
    }

}
