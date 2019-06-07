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
import javax.servlet.jsp.PageContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.day.cq.wcm.api.WCMMode;

@RunWith(MockitoJUnitRunner.class)
public class WCMModeFunctionsTest {

    @Mock
    private ServletRequest request;

    @Mock
    private PageContext pageContext;

    @Test
    public void testIsDesign() {
        assertFalse(WCMModeFunctions.isDesign(null));

        // null request
        assertFalse(WCMModeFunctions.isDesign(pageContext));

        when(pageContext.getRequest()).thenReturn(request);
        assertFalse(WCMModeFunctions.isDesign(pageContext));

        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.DESIGN);
        assertTrue(WCMModeFunctions.isDesign(pageContext));

        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.EDIT);
        assertFalse(WCMModeFunctions.isDesign(pageContext));
    }

    @Test
    public void testIsDisabled() {
        assertTrue(WCMModeFunctions.isDisabled(null));

        // null request
        assertTrue(WCMModeFunctions.isDisabled(pageContext));

        when(pageContext.getRequest()).thenReturn(request);
        assertTrue(WCMModeFunctions.isDisabled(pageContext));

        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.DISABLED);
        assertTrue(WCMModeFunctions.isDisabled(pageContext));

        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.EDIT);
        assertFalse(WCMModeFunctions.isDisabled(pageContext));
    }

    @Test
    public void testIsEdit() {
        assertFalse(WCMModeFunctions.isEdit(null));

        // null request
        assertFalse(WCMModeFunctions.isEdit(pageContext));

        when(pageContext.getRequest()).thenReturn(request);
        assertFalse(WCMModeFunctions.isEdit(pageContext));

        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.EDIT);
        assertTrue(WCMModeFunctions.isEdit(pageContext));

        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.DISABLED);
        assertFalse(WCMModeFunctions.isEdit(pageContext));
    }

    @Test
    public void testIsPreview() {
        assertFalse(WCMModeFunctions.isPreview(null));

        // null request
        assertFalse(WCMModeFunctions.isPreview(pageContext));

        when(pageContext.getRequest()).thenReturn(request);
        assertFalse(WCMModeFunctions.isPreview(pageContext));

        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.PREVIEW);
        assertTrue(WCMModeFunctions.isPreview(pageContext));

        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.DISABLED);
        assertFalse(WCMModeFunctions.isPreview(pageContext));
    }

}
