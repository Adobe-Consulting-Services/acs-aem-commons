/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.properties;

import java.util.HashMap;
import java.util.Map;

import com.adobe.acs.commons.properties.impl.AllPagePropertiesContentVariableProvider;
import com.adobe.acs.commons.properties.impl.PropertyAggregatorServiceImpl;

import com.adobe.acs.commons.properties.impl.PropertyConfigServiceImpl;
import io.wcm.testing.mock.aem.junit.AemContext;

public class TemplatedDialogTestUtil {

    public static  Map<String, Object> defaultConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("exclude.list", "cq:(.*)");
        return map;
    }

    public static PropertyAggregatorService defaultService(AemContext context) {
        Map<String, Object> config = defaultConfigMap();
        context.registerInjectActivateService(new PropertyConfigServiceImpl(), config);
        context.registerInjectActivateService(new AllPagePropertiesContentVariableProvider());
        return context.registerInjectActivateService(new PropertyAggregatorServiceImpl());
    }
}