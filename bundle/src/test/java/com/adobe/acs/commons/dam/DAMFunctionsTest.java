/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.dam;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;

public class DAMFunctionsTest {

    @Test
    public void testGetTitleOrName() {
        Asset assetWithTitle = mock(Asset.class);
        String title = RandomStringUtils.randomAlphanumeric(10);
        when(assetWithTitle.getMetadataValue(DamConstants.DC_TITLE)).thenReturn(title);
        assertEquals(title, DAMFunctions.getTitleOrName(assetWithTitle));

        Asset assetWithoutTitle = mock(Asset.class);
        String name = RandomStringUtils.randomAlphanumeric(10);
        when(assetWithoutTitle.getName()).thenReturn(name);
        assertEquals(name, DAMFunctions.getTitleOrName(assetWithoutTitle));

        verify(assetWithTitle, only()).getMetadataValue(DamConstants.DC_TITLE);
        verify(assetWithoutTitle).getMetadataValue(DamConstants.DC_TITLE);
        verify(assetWithoutTitle).getName();
        verifyNoMoreInteractions(assetWithoutTitle);
    }

}
