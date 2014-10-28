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

package com.adobe.acs.commons.quickly;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Command {
    private static final Logger log = LoggerFactory.getLogger(Command.class);

    public static final String REQUEST_PARAM_CMD = "cmd";

    private final String raw;

    private final String operation;

    private final String param;

    private final String[] params;

    public Command(final SlingHttpServletRequest request) {
        this(request.getParameter(REQUEST_PARAM_CMD));
    }

    public Command(final String raw) {
        this.raw = StringUtils.stripToEmpty(raw);
        this.operation = StringUtils.stripToEmpty(StringUtils.lowerCase(StringUtils.substringBefore(this.raw, " ")));
        this.param = StringUtils.stripToEmpty(StringUtils.removeStart(this.raw, this.operation));
        this.params = StringUtils.split(this.param);

        if(log.isTraceEnabled()) {
            log.trace("Raw: {}", this.raw);
            log.trace("Operation: {}", this.operation);
            log.trace("Param: {}", this.params);
            log.trace("Params: {}", Arrays.toString(this.params));
        }
    }

    public String getOp() {
        return this.operation;
    }

    public String getParam() {
        return this.param;
    }

    public String toString() { return this.raw; }

    public String[] getParams() { return this.params; }
}
