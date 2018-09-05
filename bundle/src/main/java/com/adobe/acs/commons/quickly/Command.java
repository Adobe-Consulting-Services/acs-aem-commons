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

    private static final String[] PUNCTUATIONS = { "!" };
    public static final String REQUEST_PARAM_CMD = "cmd";

    private final String raw;

    private final String operation;

    private final String param;

    private final String[] params;

    private final String[] punctuation;

    public Command(final SlingHttpServletRequest request) {
        this(request.getParameter(REQUEST_PARAM_CMD));
    }

    public Command(final String raw) {
        this.raw = StringUtils.stripToEmpty(raw);

        String opWithPunctuation = StringUtils.stripToEmpty(StringUtils.lowerCase(StringUtils.substringBefore(this.raw, " ")));
        int punctuationIndex = StringUtils.indexOfAny(opWithPunctuation, PUNCTUATIONS);

        if(punctuationIndex > 0) {
            this.punctuation = StringUtils.substring(opWithPunctuation, punctuationIndex).split("(?!^)");
            this.operation = StringUtils.substring(opWithPunctuation, 0, punctuationIndex);
        } else {
            this.punctuation = new String[]{};
            this.operation = opWithPunctuation;
        }

        this.param = StringUtils.stripToEmpty(StringUtils.removeStart(this.raw, opWithPunctuation));
        this.params = StringUtils.split(this.param);

        if(log.isTraceEnabled()) {
            log.trace("Raw: {}", this.raw);
            log.trace("Operation: {}", this.operation);
            log.trace("Punctuation: {}", Arrays.toString(this.punctuation));
            log.trace("Param: {}", this.param);
            log.trace("Params: {}", Arrays.toString(this.params));
        }
    }

    public String getOp() {
        return this.operation;
    }

    public String getParam() {
        return this.param;
    }

    public String toString() {
        return this.raw;
    }

    public String[] getParams() {
        return Arrays.copyOf(this.params, this.params.length);
    }

    public String[] getPunctuation() {
        return Arrays.copyOf(this.punctuation, this.punctuation.length);
    }

}
