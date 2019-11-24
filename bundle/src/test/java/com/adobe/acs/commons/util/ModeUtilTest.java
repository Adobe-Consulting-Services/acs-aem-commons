/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2015 Adobe
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
package com.adobe.acs.commons.util;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Set;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.settings.SlingSettingsService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.day.cq.wcm.api.AuthoringUIMode;
import com.day.cq.wcm.api.WCMMode;

@RunWith(MockitoJUnitRunner.class)
public class ModeUtilTest {

    @Mock
    SlingHttpServletRequest request;

    @Mock
    SlingSettingsService slingSettings;

    @Mock
    ComponentContext context;

    @InjectMocks
    ModeUtil util;

    Set<String> modes = null;

    @Before
    public void setUp() {
        modes = new HashSet<>();
        when(slingSettings.getRunModes()).thenReturn(modes);
    }

    @After
    public void tearDown() {
        modes = null;
        reset(slingSettings, context);
    }

    @Test
    public void testIsAuthor() throws Exception {
        modes.add("author");

        ModeUtil.configure(slingSettings);
        assertTrue(ModeUtil.isAuthor());
        verify(slingSettings).getRunModes();
        verifyNoMoreInteractions(slingSettings, context);
    }

    @Test
    public void testIsNotAuthor() throws Exception {
        modes.add("publish");

        ModeUtil.configure(slingSettings);
        assertFalse(ModeUtil.isAuthor());
        verify(slingSettings).getRunModes();
        verifyNoMoreInteractions(slingSettings, context);
    }

    @Test
    public void testIsPublish() throws Exception {
        modes.add("publish");

        ModeUtil.configure(slingSettings);
        assertTrue(ModeUtil.isPublish());
        verify(slingSettings).getRunModes();
        verifyNoMoreInteractions(slingSettings, context);
    }

    @Test
    public void testIsNotPublish() throws Exception {
        modes.add("author");

        ModeUtil.configure(slingSettings);
        assertFalse(ModeUtil.isPublish());
        verify(slingSettings).getRunModes();
        verifyNoMoreInteractions(slingSettings, context);
    }

    @Test
    public void testIsRunmode() throws Exception {
        modes.add("publish");

        ModeUtil.configure(slingSettings);
        assertTrue(ModeUtil.isRunmode("publish"));
        verify(slingSettings).getRunModes();
        verifyNoMoreInteractions(slingSettings, context);

    }

    @Test
    public void testIsNotRunmode() throws Exception {
        modes.add("publish");

        ModeUtil.configure(slingSettings);
        assertFalse(ModeUtil.isRunmode("author"));
        verify(slingSettings).getRunModes();
        verifyNoMoreInteractions(slingSettings, context);

    }

    @Test
    public void testIsAnalytics() {
        when(request.getAttribute(WCMMode.class.getName())).thenReturn(WCMMode.ANALYTICS);
        assertTrue(ModeUtil.isAnalytics(request));
        verify(request, atLeast(1)).getAttribute(WCMMode.class.getName());
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsNotAnalytics() {
        when(request.getAttribute(WCMMode.class.getName())).thenReturn(WCMMode.EDIT);
        assertFalse(ModeUtil.isAnalytics(request));
        verify(request, atLeast(1)).getAttribute(WCMMode.class.getName());
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsDesign() {
        when(request.getAttribute(WCMMode.class.getName())).thenReturn(WCMMode.DESIGN);
        assertTrue(ModeUtil.isDesign(request));
        verify(request, atLeast(1)).getAttribute(WCMMode.class.getName());
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsNotDesign() {
        when(request.getAttribute(WCMMode.class.getName())).thenReturn(WCMMode.EDIT);
        assertFalse(ModeUtil.isDesign(request));
        verify(request, atLeast(1)).getAttribute(WCMMode.class.getName());
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsDisabled() {
        when(request.getAttribute(WCMMode.class.getName())).thenReturn(WCMMode.DISABLED);
        assertTrue(ModeUtil.isDisabled(request));
        verify(request, atLeast(1)).getAttribute(WCMMode.class.getName());
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsNotDisabled() {
        when(request.getAttribute(WCMMode.class.getName())).thenReturn(WCMMode.EDIT);
        assertFalse(ModeUtil.isDisabled(request));
        verify(request, atLeast(1)).getAttribute(WCMMode.class.getName());
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsEdit() {
        when(request.getAttribute(WCMMode.class.getName())).thenReturn(WCMMode.EDIT);
        assertTrue(ModeUtil.isEdit(request));
        verify(request, atLeast(1)).getAttribute(WCMMode.class.getName());
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsNotEdit() {
        when(request.getAttribute(WCMMode.class.getName())).thenReturn(WCMMode.PREVIEW);
        assertFalse(ModeUtil.isEdit(request));
        verify(request, atLeast(1)).getAttribute(WCMMode.class.getName());
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsPreview() {
        when(request.getAttribute(WCMMode.class.getName())).thenReturn(WCMMode.PREVIEW);
        assertTrue(ModeUtil.isPreview(request));
        verify(request, atLeast(1)).getAttribute(WCMMode.class.getName());
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsNotPreview() {
        when(request.getAttribute(WCMMode.class.getName())).thenReturn(WCMMode.EDIT);
        assertFalse(ModeUtil.isPreview(request));
        verify(request, atLeast(1)).getAttribute(WCMMode.class.getName());
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsReadOnly() {
        when(request.getAttribute(WCMMode.class.getName())).thenReturn(WCMMode.READ_ONLY);
        assertTrue(ModeUtil.isReadOnly(request));
        verify(request, atLeast(1)).getAttribute(WCMMode.class.getName());
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsNotReadOnly() {
        when(request.getAttribute(WCMMode.class.getName())).thenReturn(WCMMode.EDIT);
        assertFalse(ModeUtil.isReadOnly(request));
        verify(request, atLeast(1)).getAttribute(WCMMode.class.getName());
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsClassic() {
        when(request.getAttribute(AuthoringUIMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(AuthoringUIMode.CLASSIC);
        assertTrue(ModeUtil.isClassic(request));
        verify(request, atLeast(1)).getAttribute(AuthoringUIMode.class.getName());
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsNotClassic() {
        when(request.getAttribute(AuthoringUIMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(AuthoringUIMode.TOUCH);
        assertFalse(ModeUtil.isClassic(request));
        verify(request, atLeast(1)).getAttribute(AuthoringUIMode.class.getName());
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsTouch() {
        when(request.getAttribute(AuthoringUIMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(AuthoringUIMode.TOUCH);
        assertTrue(ModeUtil.isTouch(request));
        verify(request, atLeast(1)).getAttribute(AuthoringUIMode.class.getName());
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsNotTouch() {
        when(request.getAttribute(AuthoringUIMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(AuthoringUIMode.CLASSIC);
        assertFalse(ModeUtil.isTouch(request));
        verify(request, atLeast(1)).getAttribute(AuthoringUIMode.class.getName());
        verifyNoMoreInteractions(request);
    }

    @Test(expected = ConfigurationException.class)
    public void testActivateBothModes() throws Exception {
        modes.add("author");
        modes.add("publish");
        ModeUtil.configure(slingSettings);
        verifyNoMoreInteractions(slingSettings, context);

    }

}
