/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.scripting.impl;

import javax.script.Bindings;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.scripting.api.BindingsValuesProvider;

import com.day.cq.search.QueryBuilder;

@Component
@Service
public class QueryBuilderBindingsValuesProvider implements BindingsValuesProvider {

    @Reference
    private QueryBuilder queryBuilder;

    @Override
    public void addBindings(Bindings bindings) {
        bindings.put("queryBuilder", queryBuilder);
    }
}
