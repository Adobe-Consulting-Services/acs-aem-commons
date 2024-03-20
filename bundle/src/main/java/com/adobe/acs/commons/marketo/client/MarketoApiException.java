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
package com.adobe.acs.commons.marketo.client;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarketoApiException extends IOException {

    private static final Logger log = LoggerFactory.getLogger(MarketoApiException.class);

    private final String requestLine;
    private final int statusCode;
    private final String reasonString;
    private final String responseBody;

    private static final String getResponseBody(HttpResponse response) {
        if (response != null) {
            try {
                return StringEscapeUtils
                        .escapeHtml(StringUtils.abbreviate(EntityUtils.toString(response.getEntity()), 100));
            } catch (ParseException | IOException e) {
                log.warn("Failed to read response from: {}", response, e);
            }
        }
        return null;
    }

    public MarketoApiException(String message, HttpRequestBase request, HttpResponse response) {
        this(message, request, response, getResponseBody(response), null);
    }

    public MarketoApiException(String message, HttpRequestBase request, HttpResponse response, String responseBody,
            Exception cause) {
        super(message, cause);
        this.requestLine = Optional.ofNullable(request).map(r -> r.getRequestLine().toString()).orElse(null);
        if (response != null) {
            this.statusCode = response.getStatusLine().getStatusCode();
            this.reasonString = response.getStatusLine().getReasonPhrase();
            this.responseBody = responseBody;
        } else {
            this.statusCode = -1;
            this.reasonString = null;
            this.responseBody = responseBody;
        }
    }

    public MarketoApiException(String message, HttpRequestBase request, HttpResponse response, String responseBody) {
        this(message, request, response, responseBody, null);
    }

    @Override
    public String getMessage() {
        return String.format(
                "%s\tREQUEST{%s}\tRESPONSE{Status Code: %d, Reason Phrase: %s, Response Body: %s}",
                super.getMessage(), getRequestLine(), getStatusCode(), getReasonString(), getResponseBody());
    }

    /**
     * @return the requestLine
     */
    public String getRequestLine() {
        return requestLine;
    }

    /**
     * @return the statusCode
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @return the reasonString
     */
    public String getReasonString() {
        return reasonString;
    }

    /**
     * @return the responseBody
     */
    public String getResponseBody() {
        return responseBody;
    }

}
