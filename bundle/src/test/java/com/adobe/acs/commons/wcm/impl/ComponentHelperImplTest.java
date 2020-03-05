/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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

import com.adobe.acs.commons.wcm.ComponentEditType;
import com.day.cq.wcm.api.AuthoringUIMode;
import com.day.cq.wcm.api.WCMMode;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.text.StringContainsInOrder.stringContainsInOrder;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ComponentHelperImplTest {

    private ComponentHelperImpl componentHelper = new ComponentHelperImpl();

    @Mock
    private SlingHttpServletRequest request;

    @Test
    public void generateClassicUIPlaceholder() throws Exception {
        String className = RandomStringUtils.randomAlphanumeric(5);
        String title = RandomStringUtils.randomAlphanumeric(10);

        String output = componentHelper.generateClassicUIPlaceholder(className, title);
        assertThat(output, stringContainsInOrder(Arrays.asList("class=\"" + className + "\"", "alt=\"" + title + "\"", "title=\"" + title + "\"")));
    }

    @Test
    public void isDesignMode() throws Exception {
        setupWCMMode(WCMMode.DESIGN);
        assertTrue(componentHelper.isDesignMode(request));
        verifyWCMModeAccess();
    }

    @Test
    public void isNotDesignMode() throws Exception {
        setupWCMMode(WCMMode.DISABLED);
        assertFalse(componentHelper.isDesignMode(request));
        verifyWCMModeAccess();
    }

    @Test
    public void isDisabledMode() throws Exception {
        setupWCMMode(WCMMode.DISABLED);
        assertTrue(componentHelper.isDisabledMode(request));
        verifyWCMModeAccess();
    }

    @Test
    public void isNotDisabledMode() throws Exception {
        setupWCMMode(WCMMode.EDIT);
        assertFalse(componentHelper.isDisabledMode(request));
        verifyWCMModeAccess();
    }

    @Test
    public void isEditMode() throws Exception {
        setupWCMMode(WCMMode.EDIT);
        assertTrue(componentHelper.isEditMode(request));
        verifyWCMModeAccess();
    }

    @Test
    public void isNotEditMode() throws Exception {
        setupWCMMode(WCMMode.PREVIEW);
        assertFalse(componentHelper.isEditMode(request));
        verifyWCMModeAccess();
    }

    @Test
    public void isPreviewMode() throws Exception {
        setupWCMMode(WCMMode.PREVIEW);
        assertTrue(componentHelper.isPreviewMode(request));
        verifyWCMModeAccess();
    }

    @Test
    public void isNotPreviewMode() throws Exception {
        setupWCMMode(WCMMode.EDIT);
        assertFalse(componentHelper.isPreviewMode(request));
        verifyWCMModeAccess();
    }

    @Test
    public void isReadOnlyMode() throws Exception {
        setupWCMMode(WCMMode.READ_ONLY);
        assertTrue(componentHelper.isReadOnlyMode(request));
        verifyWCMModeAccess();
    }

    @Test
    public void isNotReadOnlyMode() throws Exception {
        setupWCMMode(WCMMode.EDIT);
        assertFalse(componentHelper.isReadOnlyMode(request));
        verifyWCMModeAccess();
    }

    @Test
    public void isAuthoringModeWhenEdit() throws Exception {
        setupWCMMode(WCMMode.EDIT);
        assertTrue(componentHelper.isAuthoringMode(request));
        verifyWCMModeAccess();
    }

    @Test
    public void isAuthoringModeWhenDesign() throws Exception {
        setupWCMMode(WCMMode.DESIGN);
        assertTrue(componentHelper.isAuthoringMode(request));
        verifyWCMModeAccess(2);
    }

    @Test
    public void isNotAuthoringModeWhenPreview() throws Exception {
        setupWCMMode(WCMMode.PREVIEW);
        assertFalse(componentHelper.isAuthoringMode(request));
        verifyWCMModeAccess(2);
    }

    @Test
    public void isTouchAuthoringMode() throws Exception {
        when(request.getAttribute(AuthoringUIMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(AuthoringUIMode.TOUCH);
        assertTrue(componentHelper.isTouchAuthoringMode(request));
        verify(request).getAttribute(AuthoringUIMode.REQUEST_ATTRIBUTE_NAME);
    }

    @Test
    public void isNotTouchAuthoringMode() throws Exception {
        when(request.getAttribute(AuthoringUIMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(AuthoringUIMode.CLASSIC);
        assertFalse(componentHelper.isTouchAuthoringMode(request));
        verify(request).getAttribute(AuthoringUIMode.REQUEST_ATTRIBUTE_NAME);
    }

    @Test
    public void getEditIconImgTag() throws Exception {
        String output = componentHelper.getEditIconImgTag(ComponentEditType.IMAGE);
        assertEquals("<img src=\"/libs/cq/ui/resources/0.gif\" class=\"cq-image-placeholder\" alt=\"IMAGE\" title=\"IMAGE\" />", output);
    }

    private void setupWCMMode(WCMMode mode) {
        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(mode);
    }

    private void verifyWCMModeAccess() {
        verifyWCMModeAccess(1);
    }

    private void verifyWCMModeAccess(int count) {
        verify(request, times(count)).getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME);
        verifyNoMoreInteractions(request);
    }

}