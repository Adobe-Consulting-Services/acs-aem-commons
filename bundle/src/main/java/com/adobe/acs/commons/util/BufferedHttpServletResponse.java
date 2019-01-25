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
package com.adobe.acs.commons.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class BufferedHttpServletResponse extends BufferedServletResponse implements HttpServletResponse {

    private final HttpServletResponse wrappedResponse;

    public BufferedHttpServletResponse(HttpServletResponse wrappedResponse) throws IOException {
        super(wrappedResponse);
        this.wrappedResponse = wrappedResponse;
    }

    public BufferedHttpServletResponse(HttpServletResponse wrappedResponse, StringWriter writer,
            ByteArrayOutputStream outputStream) throws IOException {
        super(wrappedResponse, writer, outputStream);
        this.wrappedResponse = wrappedResponse;
    }

    @Override
    public void addCookie(Cookie cookie) {
        wrappedResponse.addCookie(cookie);
    }

    @Override
    public boolean containsHeader(String name) {
        return wrappedResponse.containsHeader(name);
    }

    @Override
    public String encodeURL(String url) {
        return wrappedResponse.encodeURL(url);
    }

    @Override
    public String encodeRedirectURL(String url) {
        return wrappedResponse.encodeRedirectURL(url);
    }

    @Override
    public String encodeUrl(String url) {
        return wrappedResponse.encodeUrl(url);
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return wrappedResponse.encodeRedirectUrl(url);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        wrappedResponse.sendError(sc, msg);
    }

    @Override
    public void sendError(int sc) throws IOException {
        wrappedResponse.sendError(sc);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        wrappedResponse.sendRedirect(location);
    }

    @Override
    public void setDateHeader(String name, long date) {
        wrappedResponse.setDateHeader(name, date);
    }

    @Override
    public void addDateHeader(String name, long date) {
        wrappedResponse.addDateHeader(name, date);
    }

    public void setHeader(String name, String value) {
        wrappedResponse.setHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        wrappedResponse.addHeader(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        wrappedResponse.setIntHeader(name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {
        wrappedResponse.addIntHeader(name, value);
    }

    @Override
    public void setStatus(int sc) {
        wrappedResponse.setStatus(sc);
    }

    @Override
    public void setStatus(int sc, String sm) {
        wrappedResponse.setStatus(sc, sm);
    }

    @Override
    public int getStatus() {
        return wrappedResponse.getStatus();
    }

    @Override
    public String getHeader(String name) {
        return wrappedResponse.getHeader(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return wrappedResponse.getHeaders(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return wrappedResponse.getHeaderNames();
    }

}
