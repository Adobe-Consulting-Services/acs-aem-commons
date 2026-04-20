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
package com.adobe.acs.commons.indesign.dynamicdeckdynamo.services.impl;

import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class DynamicDeckConfigurationServiceImplTest {
    private final AemContext context = new AemContext();

    private DynamicDeckConfigurationServiceImpl dynamicDeckConfigurationService;

    @BeforeEach
    void setUp() {
        dynamicDeckConfigurationService = context.registerInjectActivateService(new DynamicDeckConfigurationServiceImpl(),
                ImmutableMap.of("placeholderImagePath", "imagePath",
                        "collectionQuery", "query"));
    }

    @Test
    void shouldReturnCollectionQuery() {
        assertEquals("query",
                dynamicDeckConfigurationService.getCollectionQuery());
    }

    @Test
    void shouldReturnPlaceholderImagePath() {
        assertEquals("imagePath", dynamicDeckConfigurationService.getPlaceholderImagePath());
    }
}