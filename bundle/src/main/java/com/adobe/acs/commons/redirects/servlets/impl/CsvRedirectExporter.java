/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2024 Adobe
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
package com.adobe.acs.commons.redirects.servlets.impl;

import com.adobe.acs.commons.redirects.models.ExportColumn;
import com.adobe.acs.commons.redirects.models.RedirectRule;
import com.day.text.csv.Csv;

import java.io.IOException;
import java.io.OutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Locale;

public class CsvRedirectExporter implements RedirectExporter {
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    @Override
    public void export(Collection<RedirectRule> rules, OutputStream outputStream) throws IOException {
        Csv csv = new Csv();
        csv.writeInit(outputStream, "UTF-8");

        csv.writeRow(
                ExportColumn.SOURCE.getTitle(),
                ExportColumn.TARGET.getTitle(),
                ExportColumn.STATUS_CODE.getTitle(),
                ExportColumn.OFF_TIME.getTitle(),
                ExportColumn.ON_TIME.getTitle(),
                ExportColumn.NOTES.getTitle(),
                ExportColumn.EVALUATE_URI.getTitle(),
                ExportColumn.IGNORE_CONTEXT_PREFIX.getTitle(),
                ExportColumn.TAGS.getTitle(),
                ExportColumn.CREATED.getTitle(),
                ExportColumn.CREATED_BY.getTitle(),
                ExportColumn.MODIFIED.getTitle(),
                ExportColumn.MODIFIED_BY.getTitle(),
                ExportColumn.CACHE_CONTROL.getTitle()
        );
        for (RedirectRule rule : rules) {
            csv.writeRow(
                    rule.getSource(),
                    rule.getTarget(),
                    String.valueOf(rule.getStatusCode()),
                    rule.getUntilDate() == null ? null : formatDate(rule.getUntilDate()),
                    rule.getEffectiveFrom() == null ? null : formatDate(rule.getEffectiveFrom()),
                    rule.getNote(),
                    String.valueOf(rule.getEvaluateURI()),
                    String.valueOf(rule.getContextPrefixIgnored()),
                    rule.getTagIds() == null ? null : String.join("\n", rule.getTagIds()),
                    rule.getCreated() == null ? null : formatDate(rule.getCreated()),
                    rule.getCreatedBy(),
                    rule.getModified() == null ? null : formatDate(rule.getModified()),
                    rule.getModifiedBy(),
                    rule.getCacheControlHeader()
            );
        }
        csv.close();
    }

    private String formatDate(Calendar date) {
        if (date == null) {
            return null;
        }
        return DATE_FORMATTER.format(date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate());
    }
}
