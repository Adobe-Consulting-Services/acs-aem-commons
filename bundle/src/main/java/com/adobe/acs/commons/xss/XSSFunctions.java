/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.xss;

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.xss.XSSAPI;
import tldgen.Function;


/**
 * XSSAPI JSP Function wrappers.
 */
@ProviderType
@SuppressWarnings("checkstyle:abbreviationaswordinname")
public final class XSSFunctions {

    /**
     * Encode a string for HTML.
     * 
     * @param xssAPI the XSSAPI
     * @param source the source string
     * @return the encoded string
     */
    @Function
    public static CharSequence encodeForHTML(XSSAPI xssAPI, String source) {
        return xssAPI.encodeForHTML(source);
    }

    /**
     * Encode a string for an HTML attribute.
     * 
     * @param xssAPI the XSSAPI
     * @param source the source string
     * @return the encoded string
     */
    @Function
    public static CharSequence encodeForHTMLAttr(XSSAPI xssAPI, String source) {
        return xssAPI.encodeForHTMLAttr(source);
    }

    /**
     * Encode a string for an JavaScript string.
     * 
     * @param xssAPI the XSSAPI
     * @param source the source string
     * @return the encoded string
     */
    @Function
    public static CharSequence encodeForJSString(XSSAPI xssAPI, String source) {
        return xssAPI.encodeForJSString(source);
    }

    /**
     * Filter a string for HTML.
     * 
     * @param xssAPI the XSSAPI
     * @param source the source string
     * @return the encoded string
     */
    @Function
    public static CharSequence filterHTML(XSSAPI xssAPI, String source) {
        return xssAPI.filterHTML(source);
    }

    /**
     * Get a valid href. This does not use the standard XSS API due to a bug
     * impacting CQ 5.6.1 (and earlier). Internal bug reference: GRANITE-4193
     * 
     * @param xssAPI the XSSAPI
     * @param source the source string
     * @return the encoded string
     */
    @Function
    public static CharSequence getValidHref(XSSAPI xssAPI, String source) {
        return xssAPI.getValidHref(source);
    }

    private XSSFunctions() {
    }

    /**
     * Validate a string which should contain a dimension, returning a default value if the source is
     * empty, can't be parsed, or contains XSS risks.  Allows integer dimensions and the keyword "auto".
     *
     * @param xssAPI the XSSAPI
     * @param dimension the source dimension
     * @param defaultValue a default value if the source can't be used
     * @return a sanitized dimension
     */
    @Function
    public static String getValidDimension(XSSAPI xssAPI, String dimension, String defaultValue) {
        return xssAPI.getValidDimension(dimension, defaultValue);
    }

    /**
     * Validate a string which should contain an integer, returning a default value if the source is
     * empty, can't be parsed, or contains XSS risks.
     *
     * @param xssAPI the XSSAPI
     * @param integer the source integer
     * @param defaultValue a default value if the source can't be used
     * @return a sanitized integer
     */
    @Function
    public static Integer getValidInteger(XSSAPI xssAPI, String integer, int defaultValue) {
        return xssAPI.getValidInteger(integer, defaultValue);
    }

    /**
     * Validate a Javascript token.  The value must be either a single identifier, a literal number,
     * or a literal string.
     *
     * @param xssAPI the XSSAPI
     * @param token the source token
     * @param defaultValue a default value to use if the source doesn't meet validity constraints.
     * @return a string containing a single identifier, a literal number, or a literal string token
     */
    @Function
    public static String getValidJSToken(XSSAPI xssAPI, String token, String defaultValue) {
        return xssAPI.getValidJSToken(token, defaultValue);
    }

}
