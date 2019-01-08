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

import static org.mockito.Mockito.*;

import javax.script.Bindings;

import junitx.util.PrivateAccessor;

import org.junit.Test;

import com.day.cq.search.QueryBuilder;

public class QueryBuilderBindingsValuesProviderTest {

    @Test
    public void test() throws Exception {
        QueryBuilderBindingsValuesProvider provider = new QueryBuilderBindingsValuesProvider();
        QueryBuilder qb = mock(QueryBuilder.class);
        PrivateAccessor.setField(provider, "queryBuilder", qb);

        Bindings bindings = mock(Bindings.class);
        provider.addBindings(bindings);
        verify(bindings).put("queryBuilder", qb);
    }

}
