package com.adobe.acs.commons.cors;


public class CORSUtil {

    private CORSUtil() {

    }

    public static String[] getHeadersAsArray(final String headerValue) {

        if (headerValue == null)
            return new String[0]; // empty array

        String trimmedHeaderValue = headerValue.trim();

        if (trimmedHeaderValue.isEmpty())
            return new String[0];

        return trimmedHeaderValue.split("\\s*,\\s*|\\s+");
    }
}
