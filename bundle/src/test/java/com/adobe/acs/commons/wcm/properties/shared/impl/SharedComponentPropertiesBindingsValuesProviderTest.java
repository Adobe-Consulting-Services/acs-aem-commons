package com.adobe.acs.commons.wcm.properties.shared.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.HashMap;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.wcm.PageRootProvider;
import com.adobe.acs.commons.wcm.properties.shared.SharedComponentProperties;

@RunWith(MockitoJUnitRunner.class)
public class SharedComponentPropertiesBindingsValuesProviderTest {

    private PageRootProvider pageRootProvider;
    private Resource resource;
    private Resource sharedPropsResource;
    private Resource globalPropsResource;
    private SlingHttpServletRequest request;
    private Bindings bindings;
    private ResourceResolver resourceResolver;
    private ValueMap sharedProps;
    private ValueMap globalProps;

    @Before
    public void setUp() {

        resource = mock(Resource.class);
        pageRootProvider = mock(PageRootProvider.class);
        bindings = new SimpleBindings();
        sharedPropsResource = mock(Resource.class);
        globalPropsResource = mock(Resource.class);
        resourceResolver = mock(ResourceResolver.class);
        request = mock(SlingHttpServletRequest.class);

        bindings.put(SlingBindings.REQUEST, request);
        bindings.put(SlingBindings.RESOURCE, resource);

        when(resource.getResourceResolver()).thenReturn(resourceResolver);
        when(resource.getPath()).thenReturn("/content/test");
        when(pageRootProvider.getRootPagePath(anyString()))
                .thenReturn("/content/test");

        sharedProps = new ValueMapDecorator(new HashMap<>());
        globalProps = new ValueMapDecorator(new HashMap<>());

        when(globalPropsResource.getValueMap()).thenReturn(globalProps);
        when(sharedPropsResource.getValueMap()).thenReturn(sharedProps);
        when(resource.getValueMap()).thenReturn(ValueMap.EMPTY);
    }

    @Test
    public void testDisabled() {

        SharedComponentPropertiesBindingsValuesProvider provider =
                new SharedComponentPropertiesBindingsValuesProvider();

        activate(provider, false);

        provider.addBindings(bindings);

        assertEquals(ValueMap.EMPTY,
                bindings.get(SharedComponentProperties.GLOBAL_PROPERTIES));
    }

    @Test
    public void testEnabled() {

        SharedComponentPropertiesBindingsValuesProvider provider =
                new SharedComponentPropertiesBindingsValuesProvider();

        activate(provider, true);

        provider.addBindings(bindings);

        assertNotNull(bindings.get(SharedComponentProperties.GLOBAL_PROPERTIES));
    }

    private void activate(
            SharedComponentPropertiesBindingsValuesProvider provider,
            boolean enabled) {

        SharedComponentPropertiesBindingsValuesProvider.Config config =
                mock(SharedComponentPropertiesBindingsValuesProvider.Config.class);

        when(config.enabled()).thenReturn(enabled);

        provider.activate(config);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
