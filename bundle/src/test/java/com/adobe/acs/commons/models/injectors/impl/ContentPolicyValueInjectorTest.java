/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2025 Adobe
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
package com.adobe.acs.commons.models.injectors.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class ContentPolicyValueInjectorTest {

    @Rule
    private OsgiContext context = new OsgiContext();


    @Test
    public void testDisabledInjector() {
        ContentPolicyValueInjector injector = new ContentPolicyValueInjector();
        Map<String,String> props = new HashMap<>();
        props.put("enabled", "false");
        context.registerInjectActivateService(injector, props);

        try (MockedStatic<InjectorUtils> injectorUtils = Mockito.mockStatic(InjectorUtils.class)) {
            injectorUtils.when(() -> InjectorUtils.getContentPolicy(any()))
                .thenReturn(null);
            injectorUtils.verify(() -> InjectorUtils.getContentPolicy(any()),never());
        }
    }

}
