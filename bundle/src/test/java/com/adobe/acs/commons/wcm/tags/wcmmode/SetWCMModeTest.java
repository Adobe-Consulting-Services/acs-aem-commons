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

import static org.mockito.Mockito.*;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.day.cq.wcm.api.WCMMode;

@RunWith(MockitoJUnitRunner.class)
public class SetWCMModeTest {

    @Before
    public void setup() {
        when(pageContext.getRequest()).thenReturn(request);
        tag.release();
    }

    @Mock
    private PageContext pageContext;

    @Mock
    private ServletRequest request;

    @InjectMocks
    private SetWCMMode tag = new SetWCMMode();

    @Test
    public void test_setting_from_null() throws Exception {
        tag.setMode("edit");
        tag.doStartTag();
        tag.doEndTag();

        verify(request, atLeastOnce()).getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME);
        verify(request).setAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME, WCMMode.EDIT);
        verify(request).setAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME, WCMMode.DISABLED);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void test_setting_to_change() throws Exception {
        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.DESIGN);

        tag.setMode("edit");
        tag.doStartTag();
        tag.doEndTag();

        verify(request, atLeastOnce()).getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME);
        verify(request).setAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME, WCMMode.EDIT);
        verify(request).setAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME, WCMMode.DESIGN);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void test_setting_to_change_no_restore() throws Exception {
        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.DESIGN);

        tag.setRestore(false);
        tag.setMode("edit");
        tag.doStartTag();
        tag.doEndTag();

        verify(request, atLeastOnce()).getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME);
        verify(request).setAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME, WCMMode.EDIT);
        verifyNoMoreInteractions(request);
    }

}
