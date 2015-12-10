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
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.day.cq.wcm.api.AuthoringUIMode;
import com.day.cq.wcm.api.WCMMode;

@RunWith(PowerMockRunner.class)
@PrepareForTest({WCMMode.class, AuthoringUIMode.class})
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
    public void setUp() throws Exception {
        modes = new HashSet<String>();
        when(slingSettings.getRunModes()).thenReturn(modes);
        PowerMockito.mockStatic(WCMMode.class, AuthoringUIMode.class);
    }

    @After
    public void tearDown() throws Exception {
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
    public void testIsAnalytics() throws Exception {
        when(WCMMode.fromRequest(request)).thenReturn(WCMMode.ANALYTICS);
        assertTrue(ModeUtil.isAnalytics(request));
        PowerMockito.verifyStatic();
        WCMMode.fromRequest(request);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsNotAnalytics() throws Exception {
        when(WCMMode.fromRequest(request)).thenReturn(WCMMode.EDIT);
        assertFalse(ModeUtil.isAnalytics(request));
        PowerMockito.verifyStatic();
        WCMMode.fromRequest(request);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsDesign() throws Exception {
        when(WCMMode.fromRequest(request)).thenReturn(WCMMode.DESIGN);
        assertTrue(ModeUtil.isDesign(request));
        PowerMockito.verifyStatic();
        WCMMode.fromRequest(request);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsNotDesign() throws Exception {
        when(WCMMode.fromRequest(request)).thenReturn(WCMMode.EDIT);
        assertFalse(ModeUtil.isDesign(request));
        PowerMockito.verifyStatic();
        WCMMode.fromRequest(request);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsDisabled() throws Exception {
        when(WCMMode.fromRequest(request)).thenReturn(WCMMode.DISABLED);
        assertTrue(ModeUtil.isDisabled(request));
        PowerMockito.verifyStatic();
        WCMMode.fromRequest(request);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsNotDisabled() throws Exception {
        when(WCMMode.fromRequest(request)).thenReturn(WCMMode.EDIT);
        assertFalse(ModeUtil.isDisabled(request));
        PowerMockito.verifyStatic();
        WCMMode.fromRequest(request);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsEdit() throws Exception {
        when(WCMMode.fromRequest(request)).thenReturn(WCMMode.EDIT);
        assertTrue(ModeUtil.isEdit(request));
        PowerMockito.verifyStatic();
        WCMMode.fromRequest(request);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsNotEdit() throws Exception {
        when(WCMMode.fromRequest(request)).thenReturn(WCMMode.PREVIEW);
        assertFalse(ModeUtil.isEdit(request));
        PowerMockito.verifyStatic();
        WCMMode.fromRequest(request);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsPreview() throws Exception {
        when(WCMMode.fromRequest(request)).thenReturn(WCMMode.PREVIEW);
        assertTrue(ModeUtil.isPreview(request));
        PowerMockito.verifyStatic();
        WCMMode.fromRequest(request);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsNotPreview() throws Exception {
        when(WCMMode.fromRequest(request)).thenReturn(WCMMode.EDIT);
        assertFalse(ModeUtil.isPreview(request));
        PowerMockito.verifyStatic();
        WCMMode.fromRequest(request);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsReadOnly() throws Exception {
        when(WCMMode.fromRequest(request)).thenReturn(WCMMode.READ_ONLY);
        assertTrue(ModeUtil.isReadOnly(request));
        PowerMockito.verifyStatic();
        WCMMode.fromRequest(request);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsNotReadOnly() throws Exception {
        when(WCMMode.fromRequest(request)).thenReturn(WCMMode.EDIT);
        assertFalse(ModeUtil.isReadOnly(request));
        PowerMockito.verifyStatic();
        WCMMode.fromRequest(request);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsClassic() throws Exception {
        when(AuthoringUIMode.fromRequest(request)).thenReturn(AuthoringUIMode.CLASSIC);
        assertTrue(ModeUtil.isClassic(request));
        PowerMockito.verifyStatic();
        AuthoringUIMode.fromRequest(request);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsNotClassic() throws Exception {
        when(AuthoringUIMode.fromRequest(request)).thenReturn(AuthoringUIMode.TOUCH);
        assertFalse(ModeUtil.isClassic(request));
        PowerMockito.verifyStatic();
        AuthoringUIMode.fromRequest(request);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsTouch() throws Exception {
        when(AuthoringUIMode.fromRequest(request)).thenReturn(AuthoringUIMode.TOUCH);
        assertTrue(ModeUtil.isTouch(request));
        PowerMockito.verifyStatic();
        AuthoringUIMode.fromRequest(request);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsNotTouch() throws Exception {
        when(AuthoringUIMode.fromRequest(request)).thenReturn(AuthoringUIMode.CLASSIC);
        assertFalse(ModeUtil.isTouch(request));
        PowerMockito.verifyStatic();
        AuthoringUIMode.fromRequest(request);
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
