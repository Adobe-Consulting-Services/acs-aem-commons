package com.adobe.acs.commons.analysis.jcrchecksum.impl.options;

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGeneratorOptions;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ChecksumGeneratorOptionsFactory {
    private static final Logger log = LoggerFactory.getLogger(ChecksumGeneratorOptionsFactory.class);

    public static ChecksumGeneratorOptions getOptions(SlingHttpServletRequest request, String name) throws IOException {
        if (StringUtils.equalsIgnoreCase("REQUEST", name)) {
            return new RequestChecksumGeneratorOptions(request);
        } else {
            return new DefaultChecksumGeneratorOptions(request);
        }
    }
}
