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

public enum ExportColumn {

    SOURCE("Source Url"),
    TARGET("Target Url"),
    STATUS_CODE("Status Code"),
    OFF_TIME("Off Time"),
    ON_TIME("On Time"),
    NOTES("Notes"),
    EVALUATE_URI("Evaluate URI"),
    IGNORE_CONTEXT_PREFIX("Ignore Context Prefix"),
    TAGS("Tags"),
    CREATED("Created"),
    CREATED_BY("Created By"),
    MODIFIED("Modified"),
    MODIFIED_BY("Modified By")
    ;

    private String title;

    ExportColumn(String title){
        this.title = title;
    }

    public String getTitle(){
        return title;
    }
}
