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

import java.util.Calendar;

public enum ExportColumn {

    SOURCE("Source Url", RedirectRule.SOURCE_PROPERTY_NAME, Boolean.class, true),
    TARGET("Target Url", RedirectRule.TARGET_PROPERTY_NAME, Boolean.class, true),
    STATUS_CODE("Status Code", RedirectRule.STATUS_CODE_PROPERTY_NAME, Integer.class, true),
    OFF_TIME("Off Time", RedirectRule.UNTIL_DATE_PROPERTY_NAME, Calendar.class, true),
    ON_TIME("On Time", RedirectRule.EFFECTIVE_FROM_PROPERTY_NAME, Calendar.class, true),
    NOTES("Notes", RedirectRule.NOTE_PROPERTY_NAME, String.class, true),
    EVALUATE_URI("Evaluate URI", RedirectRule.EVALUATE_URI_PROPERTY_NAME, Boolean.class, true),
    IGNORE_CONTEXT_PREFIX("Ignore Context Prefix", RedirectRule.CONTEXT_PREFIX_IGNORED_PROPERTY_NAME, Boolean.class, true),
    TAGS("Tags", RedirectRule.TAGS_PROPERTY_NAME, String[].class, true),
    CREATED("Created", RedirectRule.CREATED_PROPERTY_NAME, Calendar.class, false),
    CREATED_BY("Created By", RedirectRule.CREATED_BY_PROPERTY_NAME, String.class, false),
    MODIFIED("Modified", RedirectRule.MODIFIED_PROPERTY_NAME, Calendar.class, false),
    MODIFIED_BY("Modified By", RedirectRule.MODIFIED_BY_PROPERTY_NAME, String.class, false),
    CACHE_CONTROL("Cache-Control", RedirectRule.CACHE_CONTROL_HEADER_NAME, String.class, false);

    private final String title;
    private final String propertyName;
    private final Class<?> type;
    private final boolean importable;

    @SuppressWarnings("squid:UnusedPrivateMethod") // false positive
    ExportColumn(String title, String propertyName, Class<?> type, boolean importable){
        this.title = title;
        this.propertyName = propertyName;
        this.type = type;
        this.importable = importable;
    }

    public String getTitle(){
        return title;
    }

    public String getPropertyName(){
        return propertyName;
    }

    public Class<?> getPropertyType(){
        return type;
    }

    public boolean isImportable(){
        return importable;
    }
}
