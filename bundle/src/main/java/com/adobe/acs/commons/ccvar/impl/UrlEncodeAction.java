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
package com.adobe.acs.commons.ccvar.impl;

import com.adobe.acs.commons.ccvar.TransformAction;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * {@link TransformAction} used to transform values with basic URL encoding.
 */
@Component(service = TransformAction.class)
public class UrlEncodeAction implements TransformAction {
    private static final Logger LOG = LoggerFactory.getLogger(UrlEncodeAction.class);
    private static final String ACTION_NAME = "url";

    @Override
    public String getName() {
        return ACTION_NAME;
    }

    @Override
    public String execute(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            LOG.error("Unable to URL encode value {}", value);
        }
        return value;
    }

    @Override
    public boolean disableEscaping() {
        return true;
    }
}
