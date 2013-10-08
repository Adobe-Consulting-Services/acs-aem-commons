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
package com.adobe.acs.commons.contentfinder.querybuilder.impl.querybuilder;

import com.adobe.acs.commons.contentfinder.querybuilder.impl.ContentFinderHitBuilder;
import com.day.cq.search.Query;
import com.day.cq.search.result.Hit;
import com.day.cq.search.writer.ResultHitWriter;
import org.apache.felix.scr.annotations.Component;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

import javax.jcr.RepositoryException;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - ContentFinder Result Hit Writer",
        description = "QueryBuilder Hit Writer used for creating ContentFinder compatible results",
        factory = "com.day.cq.search.result.ResultHitWriter/cf",
        immediate = false,
        metatype = false
)
public class ContentFinderResultHitWriter implements ResultHitWriter {
    /**
     * Result hit writer integration
     *
     * @param hit
     * @param jsonWriter
     * @param query
     * @throws javax.jcr.RepositoryException
     * @throws org.apache.sling.commons.json.JSONException
     */
    @Override
    public void write(Hit hit, JSONWriter jsonWriter, Query query) throws RepositoryException, JSONException {
        Map<String, Object> map = ContentFinderHitBuilder.buildGenericResult(hit);

        jsonWriter.object();

        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            jsonWriter.key(entry.getKey()).value(entry.getValue());
        }

        jsonWriter.endObject();
    }
}
