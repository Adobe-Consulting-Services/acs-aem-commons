/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.mcp.util;

import aQute.bnd.annotation.ProviderType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.sling.api.request.RequestParameter;

/**
 * @deprecated Class was moved to com.adobe.acs.commons.data.Spreadsheet
 */
@ProviderType
public class Spreadsheet extends com.adobe.acs.commons.data.Spreadsheet {

    /**
     * @deprecated Class was moved to com.adobe.acs.commons.data.Spreadsheet
     */
    public Spreadsheet(boolean convertHeaderNames, String... headerArray) {
        super(convertHeaderNames, headerArray);
    }

    /**
     * @deprecated Class was moved to com.adobe.acs.commons.data.Spreadsheet
     */
    public Spreadsheet(boolean convertHeaderNames, InputStream file, String... required) throws IOException {
        super(convertHeaderNames, file, required);
    }

    /**
     * @deprecated Class was moved to com.adobe.acs.commons.data.Spreadsheet
     */
    public Spreadsheet(boolean convertHeaderNames, RequestParameter file, String... required) throws IOException {
        super(convertHeaderNames, file, required);
    }

    /**
     * @deprecated Class was moved to com.adobe.acs.commons.data.Spreadsheet
     */
    public Spreadsheet(InputStream file, String... required) throws IOException {
        super(file, required);
    }

    /**
     * @deprecated Class was moved to com.adobe.acs.commons.data.Spreadsheet
     */
    public Spreadsheet(RequestParameter file, String... required) throws IOException {
        super(file, required);
    }

    /**
     * @return the dataRows
     *
     * @deprecated use getDataRowsAsCompositeVariants
     */
    @Deprecated
    public List<Map<String, String>> getDataRows() {
        return getDataRowsAsCompositeVariants().stream().map(m -> {
            return m.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString()));
        }).collect(Collectors.toList());
    }
}
