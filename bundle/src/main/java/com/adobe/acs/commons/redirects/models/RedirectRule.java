/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.redirects.models;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Model(adaptables = Resource.class)
public class RedirectRule {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String SOURCE_PROPERTY_NAME = "source";
    public static final String TARGET_PROPERTY_NAME = "target";
    public static final String STATUS_CODE_PROPERTY_NAME = "statusCode";
    public static final String UNTIL_DATE_PROPERTY_NAME = "untilDate";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    @Inject
    private String source;

    @Inject
    private String target;

    @Inject
    private int statusCode;

    private String untilDate;

    private Pattern ptrn;

    private SubstitutionElement[] substitutions;

    public RedirectRule(ValueMap resource) {
        this(resource.get(SOURCE_PROPERTY_NAME, ""), resource.get(TARGET_PROPERTY_NAME, ""),
                resource.get(STATUS_CODE_PROPERTY_NAME, 0), resource.get(UNTIL_DATE_PROPERTY_NAME, String.class));
    }

    public RedirectRule(String source, String target, int statusCode, String untilStr) {
        this.source = source.trim();
        this.target = target.trim();
        this.statusCode = statusCode;

        String regex = this.source;
        if (regex.endsWith("*")) {
            regex = regex.replaceAll("\\*$", "(.*)");
        }
        ptrn = toRegex(regex);
        substitutions = SubstitutionElement.parse(this.target);
        if (StringUtils.isNotBlank(untilStr)) {
            untilDate = untilStr.trim();
        }
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Pattern getRegex() {
        return ptrn;
    }

    public String getUntilDate() {
        return untilDate;
    }

    public ZonedDateTime getUntilDateTime() {
        if (untilDate != null && !untilDate.isEmpty()) {
            try {
                LocalDate ld = DATE_FORMATTER.parse(untilDate).query(LocalDate::from);
                return ld == null ? null : ld.atStartOfDay().plusDays(1).minusSeconds(1).atZone(ZoneId.systemDefault());
            } catch (DateTimeParseException e){
                // not fatal. log and continue
                log.error("Invalid UntilDateTime {}", untilDate, e);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("RedirectRule{source='%s', target='%s', statusCode=%s, untilDate=%s}",
                source, target, statusCode, untilDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RedirectRule that = (RedirectRule) o;

        return source != null ? source.equals(that.source) : that.source == null;
    }

    @Override
    public int hashCode() {
        return source != null ? source.hashCode() : 0;
    }

    public String evaluate(Matcher matcher) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < substitutions.length; i++) {
            buf.append(substitutions[i].evaluate(matcher));
        }
        return buf.toString();
    }

    static Pattern toRegex(String src) {
        Pattern ptrn;
        try {
            ptrn = Pattern.compile(src);
            int groupCount = ptrn.matcher("").groupCount();
            if (groupCount == 0) {
                ptrn = null;
            }
        } catch (PatternSyntaxException e) {
            log.info("invalid regex: {}", src);
            ptrn = null;
        }
        return ptrn;
    }

}
