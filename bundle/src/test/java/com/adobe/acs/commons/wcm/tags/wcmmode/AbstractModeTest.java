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
import javax.servlet.jsp.tagext.TagSupport;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.day.cq.wcm.api.WCMMode;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractModeTest {

    @Before
    public void setup() {
        when(pageContext.getRequest()).thenReturn(request);
        getTag().release();
    }

    @Mock
    private PageContext pageContext;

    @Mock
    private ServletRequest request;

    abstract AbstractMode getTag();

    abstract WCMMode getCorrectMode();

    @Test
    public void test_with_null_wcmmode() throws Exception {
        assertEquals(TagSupport.SKIP_BODY, getTag().doStartTag());
    }

    @Test
    public void test_with_correct_wcmmode() throws Exception {
        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(getCorrectMode());
        assertEquals(TagSupport.EVAL_BODY_INCLUDE, getTag().doStartTag());
    }

    @Test
    public void test_with_wrong_wcmmode() throws Exception {
        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(getWrongMode());
        assertEquals(TagSupport.SKIP_BODY, getTag().doStartTag());
    }

    @Test
    public void test_with_correct_wcmmode_and_not() throws Exception {
        getTag().setNot(true);
        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(getCorrectMode());
        assertEquals(TagSupport.SKIP_BODY, getTag().doStartTag());
    }

    @Test
    public void test_with_null_wcmmode_and_not() throws Exception {
        getTag().setNot(true);
        assertEquals(TagSupport.EVAL_BODY_INCLUDE, getTag().doStartTag());
    }

    @Test
    public void test_with_wrong_wcmmode_and_not() throws Exception {
        getTag().setNot(true);
        when(request.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(getWrongMode());
        assertEquals(TagSupport.EVAL_BODY_INCLUDE, getTag().doStartTag());
    }
    
    @Test
    public void test_end_tag() throws Exception {
        assertEquals(TagSupport.EVAL_PAGE, getTag().doEndTag());
    }

    WCMMode getWrongMode() {
        if (getCorrectMode() == WCMMode.EDIT) {
            return WCMMode.DISABLED;
        } else {
            return WCMMode.EDIT;
        }
    }

    ServletRequest getRequest() {
        return request;
    }

}
