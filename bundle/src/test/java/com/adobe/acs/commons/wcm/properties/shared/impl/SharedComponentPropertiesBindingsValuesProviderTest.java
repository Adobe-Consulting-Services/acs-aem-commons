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
package com.adobe.acs.commons.wcm.properties.shared.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

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
import org.osgi.annotation.versioning.ConsumerType;

@RunWith(MockitoJUnitRunner.class)
public class SharedComponentPropertiesBindingsValuesProviderTest {

    public static final String SITE_ROOT = "/content/acs-commons";
    public static final String RESOURCE_TYPE = "acs-commons/components/content/generic-text";

    /**
     * Pre-6.5.7 LazyBindings support.
     */
    private static final String REL_PATH_SLING_API_2_22_0 = "org.apache.sling.api-2.22.0.jar";

    private PageRootProvider pageRootProvider;
    private Resource resource;
    private Resource sharedPropsResource;
    private Resource globalPropsResource;
    private SlingHttpServletRequest request;
    private Bindings bindings;

    private ResourceResolver resourceResolver;
    private ValueMap sharedProps;
    private ValueMap globalProps;

    private ValueMap localProps;

    /**
     * Pre-6.5.7 LazyBindings support. This class simulates the LazyBindings and LazyBindings.Supplier class hierarchy
     * until this project upgrades to a dependency list that includes org.apache.sling.api version 2.22.0+.
     *
     * @see <a href="https://sling.apache.org/apidocs/sling12/org/apache/sling/api/scripting/LazyBindings.html">LazyBindings</a>
     */
    @ConsumerType
    private static class LazyLikeBindings extends SimpleBindings {
        @ConsumerType
        @FunctionalInterface
        interface Supplier extends java.util.function.Supplier {
        }
    }

    @Before
    public void setUp() throws Exception {
        resource = mock(Resource.class);
        pageRootProvider = mock(PageRootProvider.class);
        bindings = new SimpleBindings();
        sharedPropsResource = mock(Resource.class);
        globalPropsResource = mock(Resource.class);
        resourceResolver = mock(ResourceResolver.class);
        request = mock(SlingHttpServletRequest.class);

        final String globalPropsPath = SITE_ROOT + "/jcr:content/" + SharedComponentProperties.NN_GLOBAL_COMPONENT_PROPERTIES;
        final String sharedPropsPath = SITE_ROOT + "/jcr:content/" + SharedComponentProperties.NN_SHARED_COMPONENT_PROPERTIES + "/"
                + RESOURCE_TYPE;

        bindings.put(SlingBindings.REQUEST, request);
        bindings.put(SlingBindings.RESOURCE, resource);

        when(resource.getResourceResolver()).thenReturn(resourceResolver);
        when(resource.getResourceType()).thenReturn(RESOURCE_TYPE);
        when(resourceResolver.getSearchPath()).thenReturn(new String[]{"/apps/", "/libs/"});
        when(resourceResolver.getResource(sharedPropsPath)).thenReturn(sharedPropsResource);
        when(resourceResolver.getResource(globalPropsPath)).thenReturn(globalPropsResource);

        when(resource.getPath()).thenReturn(SITE_ROOT);
        when(pageRootProvider.getRootPagePath(anyString())).thenReturn(SITE_ROOT);


        sharedProps = new ValueMapDecorator(new HashMap<String, Object>());
        globalProps = new ValueMapDecorator(new HashMap<String, Object>());
        localProps = new ValueMapDecorator(new HashMap<String, Object>());

        sharedProps.put("shared", "value");
        globalProps.put("global", "value");
        localProps.put("local", "value");

        when(globalPropsResource.getValueMap()).thenReturn(globalProps);
        when(sharedPropsResource.getValueMap()).thenReturn(sharedProps);
        when(resource.getValueMap()).thenReturn(localProps);
    }

    @Test
    public void testGetCanonicalResourceTypeRelativePath() {
        // make this test readable by wrapping the long method name with a function
        final BiFunction<String, List<String>, String> asFunction =
                (resourceType, searchPaths) -> SharedComponentPropertiesImpl
                        .getCanonicalResourceTypeRelativePath(resourceType,
                                Optional.ofNullable(searchPaths)
                                        .map(list -> list.toArray(new String[0])).orElse(null));

        final List<String> emptySearchPaths = Collections.emptyList();
        final List<String> realSearchPaths = Arrays.asList("/apps/", "/libs/");
        assertNull("expect null for null rt", asFunction.apply(null, emptySearchPaths));
        assertNull("expect null for empty rt", asFunction.apply("", emptySearchPaths));
        assertNull("expect null for absolute rt and null search paths",
                asFunction.apply("/fail/" + RESOURCE_TYPE, null));
        assertNull("expect null for cq:Page",
                asFunction.apply("cq:Page", realSearchPaths));
        assertNull("expect null for nt:unstructured",
                asFunction.apply("nt:unstructured", realSearchPaths));
        assertNull("expect null for absolute rt and empty search paths",
                asFunction.apply("/fail/" + RESOURCE_TYPE, emptySearchPaths));
        assertNull("expect null for sling nonexisting rt",
                asFunction.apply(Resource.RESOURCE_TYPE_NON_EXISTING, emptySearchPaths));
        assertEquals("expect same for relative rt", RESOURCE_TYPE,
                asFunction.apply(RESOURCE_TYPE, emptySearchPaths));
        assertEquals("expect same for relative rt and real search paths", RESOURCE_TYPE,
                asFunction.apply(RESOURCE_TYPE, realSearchPaths));
        assertEquals("expect relative for /apps/ + relative and real search paths", RESOURCE_TYPE,
                asFunction.apply("/apps/" + RESOURCE_TYPE, realSearchPaths));
        assertEquals("expect relative for /libs/ + relative and real search paths", RESOURCE_TYPE,
                asFunction.apply("/libs/" + RESOURCE_TYPE, realSearchPaths));
        assertNull("expect null for /fail/ + relative and real search paths",
                asFunction.apply("/fail/" + RESOURCE_TYPE, realSearchPaths));
    }

    @Test
    public void addBindings() {
        final SharedComponentPropertiesImpl sharedComponentProperties = new SharedComponentPropertiesImpl();
        sharedComponentProperties.pageRootProvider = pageRootProvider;

        final SharedComponentPropertiesBindingsValuesProvider sharedComponentPropertiesBindingsValuesProvider
                = new SharedComponentPropertiesBindingsValuesProvider();

        sharedComponentPropertiesBindingsValuesProvider.sharedComponentProperties = sharedComponentProperties;
        sharedComponentPropertiesBindingsValuesProvider.activate();
        sharedComponentPropertiesBindingsValuesProvider.addBindings(bindings);

        assertEquals(sharedPropsResource, bindings.get(SharedComponentProperties.SHARED_PROPERTIES_RESOURCE));
        assertEquals(globalPropsResource, bindings.get(SharedComponentProperties.GLOBAL_PROPERTIES_RESOURCE));
        assertEquals(sharedProps, bindings.get(SharedComponentProperties.SHARED_PROPERTIES));
        assertEquals(globalProps, bindings.get(SharedComponentProperties.GLOBAL_PROPERTIES));

        ValueMap mergedProps = (ValueMap) bindings.get(SharedComponentProperties.MERGED_PROPERTIES);

        assertEquals("value", mergedProps.get("global", String.class));
        assertEquals("value", mergedProps.get("shared", String.class));
        assertEquals("value", mergedProps.get("local", String.class));
    }

    @Test
    public void addToLazyBindings() {
        final SharedComponentPropertiesImpl sharedComponentProperties = new SharedComponentPropertiesImpl();
        sharedComponentProperties.pageRootProvider = pageRootProvider;

        final SharedComponentPropertiesBindingsValuesProvider sharedComponentPropertiesBindingsValuesProvider
                = new SharedComponentPropertiesBindingsValuesProvider();

        sharedComponentPropertiesBindingsValuesProvider.sharedComponentProperties = sharedComponentProperties;
        sharedComponentPropertiesBindingsValuesProvider.activate();
        sharedComponentPropertiesBindingsValuesProvider.checkAndSetLazyBindingsType(LazyLikeBindings.class);

        LazyLikeBindings lazyBindings = new LazyLikeBindings();
        lazyBindings.putAll(bindings);
        sharedComponentPropertiesBindingsValuesProvider.addBindings(lazyBindings);

        // confirm that the bindings is storing a marked Supplier, rather than a resource
        Object sharedPropsObject = lazyBindings.get(SharedComponentProperties.SHARED_PROPERTIES_RESOURCE);
        assertTrue(sharedPropsObject instanceof LazyLikeBindings.Supplier);
        assertEquals(SharedComponentPropertiesBindingsValuesProvider.SUPPLIER_PROXY_LABEL, sharedPropsObject.toString());
        // compare that the value returned by the supplier with the expected resource
        assertEquals(sharedPropsResource, ((LazyLikeBindings.Supplier) sharedPropsObject).get());

        // confirm that the bindings is storing a marked Supplier, rather than a resource
        Object globalPropsObject = lazyBindings.get(SharedComponentProperties.GLOBAL_PROPERTIES_RESOURCE);
        assertTrue(globalPropsObject instanceof LazyLikeBindings.Supplier);
        // compare that the value returned by the supplier with the expected resource
        assertEquals(globalPropsResource, ((LazyLikeBindings.Supplier) globalPropsObject).get());

        // confirm that the bindings is storing a marked Supplier, rather than a ValueMap
        Object sharedPropsVmObject = lazyBindings.get(SharedComponentProperties.SHARED_PROPERTIES);
        assertTrue(sharedPropsVmObject instanceof LazyLikeBindings.Supplier);
        // compare that the value returned by the supplier with the expected ValueMap
        assertEquals(sharedProps, ((LazyLikeBindings.Supplier) sharedPropsVmObject).get());

        // confirm that the bindings is storing a marked Supplier, rather than a ValueMap
        Object globalPropsVmObject = lazyBindings.get(SharedComponentProperties.GLOBAL_PROPERTIES);
        assertTrue(globalPropsVmObject instanceof LazyLikeBindings.Supplier);
        // compare that the value returned by the supplier with the expected ValueMap
        assertEquals(globalProps, ((LazyLikeBindings.Supplier) globalPropsVmObject).get());

        // confirm that the bindings is storing a marked Supplier, rather than a resource. Acquire this Supplier BEFORE
        // resetting the Global and Shared properties bindings to demonstrate that the same bindings instance
        // is also accessed lazily by the Merged props supplier.
        Object mergedPropsVmObject = lazyBindings.get(SharedComponentProperties.MERGED_PROPERTIES);
        assertTrue(mergedPropsVmObject instanceof LazyLikeBindings.Supplier);

        // reset the Global and Shared properties bindings to contain the supplied values that will be consumed by
        // the Merged properties supplier binding.
        lazyBindings.put(SharedComponentProperties.GLOBAL_PROPERTIES, globalProps);
        lazyBindings.put(SharedComponentProperties.SHARED_PROPERTIES, sharedProps);
        // NOW call the merged properties supplier function.
        ValueMap mergedProps = (ValueMap) ((LazyLikeBindings.Supplier) mergedPropsVmObject).get();

        // compare the contents of the ValueMap returned by the supplier with the expected key/values from the separate maps
        assertEquals("value", mergedProps.get("global", String.class));
        assertEquals("value", mergedProps.get("shared", String.class));
        assertEquals("value", mergedProps.get("local", String.class));
    }

    @Test
    public void addToLazyBindings_NonConformant() {
        final SharedComponentPropertiesImpl sharedComponentProperties = new SharedComponentPropertiesImpl();
        sharedComponentProperties.pageRootProvider = pageRootProvider;

        final SharedComponentPropertiesBindingsValuesProvider sharedComponentPropertiesBindingsValuesProvider
                = new SharedComponentPropertiesBindingsValuesProvider();


        sharedComponentPropertiesBindingsValuesProvider.sharedComponentProperties = sharedComponentProperties;
        sharedComponentPropertiesBindingsValuesProvider.activate();
        sharedComponentPropertiesBindingsValuesProvider.checkAndSetLazyBindingsType(SimpleBindings.class);

        // test that the wrapSupplier() method returns the value from the supplier, rather than a supplier itself
        assertEquals("immediate", sharedComponentPropertiesBindingsValuesProvider
                .wrapSupplier(() -> "immediate").toString());

        SimpleBindings lazyBindings = new SimpleBindings();
        lazyBindings.putAll(bindings);

        sharedComponentPropertiesBindingsValuesProvider.addBindings(lazyBindings);

        // confirm that the non-conformant bindings is storing the resource
        Object sharedPropsObject = lazyBindings.get(SharedComponentProperties.SHARED_PROPERTIES_RESOURCE);
        // compare that the value returned by the supplier with the expected resource
        assertEquals(sharedPropsResource, sharedPropsObject);

        // confirm that the non-conformant bindings is storing the resource
        Object globalPropsObject = lazyBindings.get(SharedComponentProperties.GLOBAL_PROPERTIES_RESOURCE);
        // compare that the value returned by the supplier with the expected resource
        assertEquals(globalPropsResource, globalPropsObject);

        // confirm that the non-conformant bindings is storing the ValueMap
        Object sharedPropsVmObject = lazyBindings.get(SharedComponentProperties.SHARED_PROPERTIES);
        // compare that the value returned by the supplier with the expected ValueMap
        assertEquals(sharedProps, sharedPropsVmObject);

        // confirm that the non-conformant bindings is storing the ValueMap
        Object globalPropsVmObject = lazyBindings.get(SharedComponentProperties.GLOBAL_PROPERTIES);
        // compare that the value returned by the supplier with the expected ValueMap
        assertEquals(globalProps, globalPropsVmObject);

        // confirm that the bindings is storing a marked Supplier, rather than a resource. Acquire this Supplier BEFORE
        // resetting the Global and Shared properties bindings to demonstrate that the same bindings instance
        // is also accessed lazily by the Merged props supplier.
        Object mergedPropsVmObject = lazyBindings.get(SharedComponentProperties.MERGED_PROPERTIES);
        ValueMap mergedProps = (ValueMap) mergedPropsVmObject;

        // compare the contents of the ValueMap returned by the supplier with the expected key/values from the separate maps
        assertEquals("value", mergedProps.get("global", String.class));
        assertEquals("value", mergedProps.get("shared", String.class));
        assertEquals("value", mergedProps.get("local", String.class));
    }

    @Test
    public void addToLazyBindings_SlingApiJar() throws Exception {
        try (final URLClassLoader slingApiClassLoader = new URLClassLoader(
                new URL[]{getClass().getClassLoader().getResource(REL_PATH_SLING_API_2_22_0)},
                getClass().getClassLoader())) {

            final SharedComponentPropertiesImpl sharedComponentProperties = new SharedComponentPropertiesImpl();
            sharedComponentProperties.pageRootProvider = pageRootProvider;

            final SharedComponentPropertiesBindingsValuesProvider sharedComponentPropertiesBindingsValuesProvider
                    = new SharedComponentPropertiesBindingsValuesProvider();
            // swap classloader
            sharedComponentPropertiesBindingsValuesProvider.swapLazyBindingsClassLoaderForTesting(slingApiClassLoader);
            sharedComponentPropertiesBindingsValuesProvider.sharedComponentProperties = sharedComponentProperties;
            // activate service to load classes
            sharedComponentPropertiesBindingsValuesProvider.activate();

            // test that the wrapSupplier() method returns the proxy supplier, rather than the supplied value
            assertEquals(SharedComponentPropertiesBindingsValuesProvider.SUPPLIER_PROXY_LABEL,
                    sharedComponentPropertiesBindingsValuesProvider.wrapSupplier(() -> "immediate").toString());

            // inject our own suppliers map for a side-channel to the suppliers
            final Map<String, Object> suppliers = new HashMap<>();
            Bindings lazyBindings = sharedComponentPropertiesBindingsValuesProvider
                    .getLazyBindingsType().getConstructor(Map.class).newInstance(suppliers);
            lazyBindings.putAll(bindings);
            final Class<? extends Supplier> supplierType = sharedComponentPropertiesBindingsValuesProvider.getSupplierType();

            sharedComponentPropertiesBindingsValuesProvider.addBindings(lazyBindings);

            // confirm that the bindings is storing a marked Supplier, rather than a resource
            Object sharedPropsObject = suppliers.get(SharedComponentProperties.SHARED_PROPERTIES_RESOURCE);
            assertTrue(supplierType.isInstance(sharedPropsObject));
            // compare that the value returned by the supplier with the expected resource
            assertEquals(sharedPropsResource, ((Supplier<?>) sharedPropsObject).get());

            // confirm that the bindings is storing a marked Supplier, rather than a resource
            Object globalPropsObject = suppliers.get(SharedComponentProperties.GLOBAL_PROPERTIES_RESOURCE);
            assertTrue(supplierType.isInstance(globalPropsObject));
            // compare that the value returned by the supplier with the expected resource
            assertEquals(globalPropsResource, ((Supplier<?>) globalPropsObject).get());

            // confirm that the bindings is storing a marked Supplier, rather than a ValueMap
            Object sharedPropsVmObject = suppliers.get(SharedComponentProperties.SHARED_PROPERTIES);
            assertTrue(supplierType.isInstance(sharedPropsVmObject));
            // compare that the value returned by the supplier with the expected ValueMap
            assertEquals(sharedProps, ((Supplier<?>) sharedPropsVmObject).get());

            // confirm that the bindings is storing a marked Supplier, rather than a ValueMap
            Object globalPropsVmObject = suppliers.get(SharedComponentProperties.GLOBAL_PROPERTIES);
            assertTrue(supplierType.isInstance(globalPropsVmObject));
            // compare that the value returned by the supplier with the expected ValueMap
            assertEquals(globalProps, ((Supplier<?>) globalPropsVmObject).get());

            // confirm that the bindings is storing a marked Supplier, rather than a resource. Acquire this Supplier BEFORE
            // resetting the Global and Shared properties bindings to demonstrate that the same bindings instance
            // is also accessed lazily by the Merged props supplier.
            Object mergedPropsVmObject = suppliers.get(SharedComponentProperties.MERGED_PROPERTIES);
            assertTrue(supplierType.isInstance(mergedPropsVmObject));
            // compare that the value returned by the supplier with the expected ValueMap
            ValueMap mergedProps = (ValueMap) ((Supplier<?>) mergedPropsVmObject).get();

            // compare the contents of the ValueMap returned by the supplier with the expected key/values from the separate maps
            assertEquals("value", mergedProps.get("global", String.class));
            assertEquals("value", mergedProps.get("shared", String.class));
            assertEquals("value", mergedProps.get("local", String.class));
        }
    }
}
