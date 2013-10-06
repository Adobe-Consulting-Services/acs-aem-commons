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
package com.adobe.acs.commons.util;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Map;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Test;

import com.adobe.acs.commons.util.TemplateUtil;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;

public class TemplateUtilTest {

    static String TMPL_FAKE = "/apps/templates/fake";

    static String TMPL_FAKE2 = "/apps/templates/fake2";

    @Test
    public void test_that_null_page_always_returns_false() {
        assertThat(TemplateUtil.hasTemplate(null, TMPL_FAKE), is(false));
    }

    @Test
    public void test_that_null_template_always_returns_false() {
        Page page = mock(Page.class);
        ValueMap properties = createTemplateValueMap(TMPL_FAKE);
        when(page.getProperties()).thenReturn(properties);
        
        assertThat(TemplateUtil.hasTemplate(page, null), is(false));
    }

    @Test
    public void test_that_null_properties_always_returns_false() {
        Page page = mock(Page.class);
        // implicit page.getProperties() == null
        
        assertThat(TemplateUtil.hasTemplate(page, TMPL_FAKE), is(false));
    }

    @Test
    public void test_that_correct_template_returns_true() {
        Page page = mock(Page.class);
        ValueMap properties = createTemplateValueMap(TMPL_FAKE);
        when(page.getProperties()).thenReturn(properties);
        
        assertThat(TemplateUtil.hasTemplate(page, TMPL_FAKE), is(true));
    }

    @Test
    public void test_that_correct_template_returns_false() {
        Page page = mock(Page.class);
        ValueMap properties = createTemplateValueMap(TMPL_FAKE2);
        when(page.getProperties()).thenReturn(properties);
        
        assertThat(TemplateUtil.hasTemplate(page, TMPL_FAKE), is(false));
    }

    private ValueMap createTemplateValueMap(String templatePath) {
        Map<String, Object> props = Collections.singletonMap(NameConstants.PN_TEMPLATE, (Object) templatePath);
        return new ValueMapDecorator(props);
    }

}
