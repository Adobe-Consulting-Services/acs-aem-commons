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
package com.adobe.acs.commons.wcm.tags.wcmmode;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.servlet.ServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.day.cq.wcm.api.WCMMode;

@RunWith(MockitoJUnitRunner.class)
public class WCMModeFunctionsTest {

    @Mock
    private ServletRequest request;

    @Test
    public void testIsDesign() {
        // null
        assertFalse(WCMModeFunctions.isDesign(request));

        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.DESIGN);
        assertTrue(WCMModeFunctions.isDesign(request));

        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.EDIT);
        assertFalse(WCMModeFunctions.isDesign(request));
    }

    @Test
    public void testIsDisabled() {
        // null
        assertTrue(WCMModeFunctions.isDisabled(request));

        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.DISABLED);
        assertTrue(WCMModeFunctions.isDisabled(request));

        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.EDIT);
        assertFalse(WCMModeFunctions.isDisabled(request));
    }

    @Test
    public void testIsEdit() {
        // null
        assertFalse(WCMModeFunctions.isEdit(request));

        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.EDIT);
        assertTrue(WCMModeFunctions.isEdit(request));

        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.DISABLED);
        assertFalse(WCMModeFunctions.isEdit(request));
    }

    @Test
    public void testIsPreview() {
        // null
        assertFalse(WCMModeFunctions.isPreview(request));

        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.PREVIEW);
        assertTrue(WCMModeFunctions.isPreview(request));

        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.DISABLED);
        assertFalse(WCMModeFunctions.isPreview(request));
    }

}
