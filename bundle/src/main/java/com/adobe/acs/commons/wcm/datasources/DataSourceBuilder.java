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
package com.adobe.acs.commons.wcm.datasources;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import java.io.IOException;
import java.util.List;

@SuppressWarnings("squid:S1214")
public interface DataSourceBuilder {

    String TEXT = "text";

    String VALUE = "value";

    /**
     * Create and add a DataSource to the the HTTP Request.
     *
     * @param slingRequest the Sling HTTP Servlet Request object to add the DataSource to
     * @param options the DataSource options
     */
    void addDataSource(SlingHttpServletRequest slingRequest, List<DataSourceOption> options);


    /**
     * Print the DataSourceOptions out in a JSON format to the response.
     *
     * @param slingRequest the slingRequest
     * @param slingResponse the slingResponse
     * @throws IOException
     */
    void writeDataSourceOptions(SlingHttpServletRequest slingRequest,
                                SlingHttpServletResponse slingResponse) throws IOException;
}