/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.version.impl;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.jcr.resource.JcrResourceUtil;

public final class EvolutionConfig {

    private String[] ignoreProperties;
    private String[] ignoreResources;

    public EvolutionConfig(String[] ignoreProperties, String[] ignoreResources) {
        this.ignoreProperties = ignoreProperties;
        this.ignoreResources = ignoreResources;
    }

    public int getDepthForPath(String path) {
        return StringUtils.countMatches(StringUtils.substringAfterLast(path, "jcr:frozenNode"), "/");
    }

    public String getRelativePropertyName(String path) {
        return StringUtils.substringAfterLast(path, "jcr:frozenNode").replaceFirst("/", "");
    }

    public String getRelativeResourceName(String path) {
        return StringUtils.substringAfterLast(path, "jcr:frozenNode/");
    }

    public boolean handleProperty(String name) {
        for (String entry : ignoreProperties) {
            if (Pattern.matches(entry, name)) {
                return false;
            }
        }
        return true;
    }

    public boolean handleResource(String name) {
        for (String entry : ignoreResources) {
            if (Pattern.matches(entry, name)) {
                return false;
            }
        }
        return true;
    }

    public String printProperty(javax.jcr.Property property) {
        try {
            return printObject(JcrResourceUtil.toJavaObject(property));
        } catch (RepositoryException e1) {
            return e1.getMessage();
        }
    }

    public String printObject(Object obj) {
        if (obj == null) {
            return "";
        }
        if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof String[]) {
            String[] values = (String[]) obj;
            String result = "[";
            for (int i = 0; i < values.length; i++) {
                result += values[i];
                if (i != (values.length - 1)) {
                    result += ", ";
                }
            }
            result += "]";
            return result;
        } else if (obj instanceof Calendar) {
            Calendar value = (Calendar) obj;
            DateFormat dateFormat = DateFormat.getDateTimeInstance();
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            return dateFormat.format(value.getTime());
        } else {
            return obj.toString();
        }
    }

}
