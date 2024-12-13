/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2024 Adobe
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

package com.adobe.acs.commons.wcm.impl;

import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, AemContextExtension.class})
class CopySitesPublishUrlFeatureTest {


    @InjectMocks
    CopySitesPublishUrlFeature copySitesPublishUrlFeature = new CopySitesPublishUrlFeature();
    @Mock
    CopySitesPublishUrlFeature.Config config;

    @BeforeEach
    void setUp() {
        copySitesPublishUrlFeature.activate(config);
    }

    @Test
    void testGetName() {
        assertEquals("com.adobe.acs.commons.wcm.impl.copysitespublishurlfeature.feature.flag",
                copySitesPublishUrlFeature.getName());
    }

    @Test
    void testGetDescription() {
        assertEquals("ACS AEM Commons feature flag enables or disables the copy publish URL dropdown field in the Sites Editor.",
                copySitesPublishUrlFeature.getDescription());
    }

    @Test
    void testIsEnabled() {
        when(config.feature_flag_active_status()).thenReturn(true);
        assertTrue(copySitesPublishUrlFeature.isEnabled(null));
    }

}