package com.adobe.acs.commons.wcm;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Map;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Test;

import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;

public class TemplateUtilsTest {

    static String TMPL_FAKE = "/apps/templates/fake";

    static String TMPL_FAKE2 = "/apps/templates/fake2";

    @Test
    public void test_that_null_page_always_returns_false() {
        assertThat(TemplateUtils.hasTemplate(null, TMPL_FAKE), is(false));
    }

    @Test
    public void test_that_null_template_always_returns_false() {
        Page page = mock(Page.class);
        ValueMap properties = createTemplateValueMap(TMPL_FAKE);
        when(page.getProperties()).thenReturn(properties);
        
        assertThat(TemplateUtils.hasTemplate(page, null), is(false));
    }

    @Test
    public void test_that_null_properties_always_returns_false() {
        Page page = mock(Page.class);
        // implicit page.getProperties() == null
        
        assertThat(TemplateUtils.hasTemplate(page, TMPL_FAKE), is(false));
    }

    @Test
    public void test_that_correct_template_returns_true() {
        Page page = mock(Page.class);
        ValueMap properties = createTemplateValueMap(TMPL_FAKE);
        when(page.getProperties()).thenReturn(properties);
        
        assertThat(TemplateUtils.hasTemplate(page, TMPL_FAKE), is(true));
    }

    @Test
    public void test_that_correct_template_returns_false() {
        Page page = mock(Page.class);
        ValueMap properties = createTemplateValueMap(TMPL_FAKE2);
        when(page.getProperties()).thenReturn(properties);
        
        assertThat(TemplateUtils.hasTemplate(page, TMPL_FAKE), is(false));
    }

    private ValueMap createTemplateValueMap(String templatePath) {
        Map<String, Object> props = Collections.singletonMap(NameConstants.PN_TEMPLATE, (Object) templatePath);
        return new ValueMapDecorator(props);
    }

}
