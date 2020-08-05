/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.mcp;

import com.adobe.acs.commons.mcp.form.DialogProvider;
import com.adobe.acs.commons.mcp.form.DialogProviderAnnotationProcessor;
import com.adobe.acs.commons.mcp.form.DialogResourceProvider;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.junit.SlingContextBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.Assert.*;

/**
 * Test various aspects of the Dialog Resource Provider service
 */
@SuppressWarnings("deprecation")
public class DialogResourceProviderFactoryTest {

    @Rule
    public SlingContext slingContext = new SlingContextBuilder()
            .registerSlingModelsFromClassPath(false)
            .resourceResolverType(ResourceResolverType.JCR_MOCK)
            .build();

    private static final String[] MODEL_CLASSES = {
        "com.adobe.acs.commons.mcp.model.SimpleModelOne",
        "com.adobe.acs.commons.mcp.model.SimpleModelTwo",
        "com.adobe.acs.commons.mcp.model.SimpleModelThree"
    };

    public Map<String, DialogResourceProvider> providers = new HashMap<>();

    @Before
    public void init() throws IOException, RuntimeException, ReflectiveOperationException {
        slingContext.addModelsForClasses(MODEL_CLASSES);
        for (String className : MODEL_CLASSES) {
            DialogResourceProvider provider = (DialogResourceProvider) Class.forName(DialogResourceProvider.getServiceClassName(className)).newInstance();
            slingContext.registerService(DialogResourceProvider.class, provider);
            providers.put(className, provider);
            provider.doActivate(slingContext.bundleContext());
        }
    }

    @Test(expected = ClassNotFoundException.class)
    public void testAnnotationProcessorNegativeDiscrimination() throws ClassNotFoundException {
        // This class should not exist and throw an error -- it doesn't provide a resource type and so it wouldn't be a candidate for a resource provider
        Class.forName(DialogResourceProvider.getServiceClassName(NoResourceTypeProvided.class.getCanonicalName()));
    }

    @Test
    public void testAnnotationProcessorPositiveDiscrimination() throws ClassNotFoundException {
        // This class provides a resource type so it should have a corresponding OSGi service
        Class.forName(DialogResourceProvider.getServiceClassName(ResourceTypeProvided.class.getCanonicalName()));
    }

    @Test
    public void testAnnotationProcessorPositiveDiscriminationForModels() throws ClassNotFoundException {
        // This class provides a resource type on the model annotation so it should have a corresponding OSGi service
        Class.forName(DialogResourceProvider.getServiceClassName(ResourceTypeProvidedByModel.class.getCanonicalName()));
    }

    @Test
    public void testResourceResolution() {
        // Quick parity check on test method
        assertFalse("Should not resolve non-existing resource", resourceExists("/this/does/not/exist"));
        // Check basic resource as well as "items" child.
        assertTrue("Should resolve model one dialog", resourceExists("/apps/test/model1/cq:dialog"));
        assertTrue("Should resolve model two dialog", resourceExists("/apps/test/model2/cq:dialog"));
        assertTrue("Should resolve model three dialog", resourceExists("/apps/test/model3/cq:dialog"));
        assertTrue("Should resolve model one dialog items", resourceExists("/apps/test/model1/cq:dialog/content/items"));
        assertTrue("Should resolve model two dialog items", resourceExists("/apps/test/model2/cq:dialog/content/items"));
        assertTrue("Should resolve model three dialog items", resourceExists("/apps/test/model3/cq:dialog/content/items"));
    }

    @Test
    public void testDeactivation() {
        assertTrue("Should resolve model one dialog", resourceExists("/apps/test/model1/cq:dialog"));
        providers.get("com.adobe.acs.commons.mcp.model.SimpleModelOne").doDeactivate();
        assertFalse("Should not resolve model one dialog", resourceExists("/apps/test/model1/cq:dialog"));
    }

    @Test
    public void testFactoryDoesNothing() {
        // The dialog provider factory is dead but still exists for backward compatiblity until 5.0
        // Assert that it does nothing.
        DialogResourceProviderFactory factory = new com.adobe.acs.commons.mcp.impl.DialogResourceProviderFactoryImpl();
        assertNull(factory.getActiveProviders());

        // These would normally throw an exception on null data if they did anything.
        factory.registerClass((Class) null);
        factory.registerClass((String) null);
        factory.unregisterClass((Class) null);
        factory.unregisterClass((String) null);
    }

    @Test
    public void testAnnotationProvider() {
        Compilation compilation = javac()
                .withProcessors(new DialogProviderAnnotationProcessor())
                .compile(JavaFileObjects.forSourceString("a.Example1", "package a; @com.adobe.acs.commons.mcp.form.DialogProvider public class Example1 {public String getResourceType(){return \"my.type\";}}"));
        assertThat(compilation).succeeded();
        compilation = javac()
                .withProcessors(new DialogProviderAnnotationProcessor())
                .compile(JavaFileObjects.forSourceString("a.Example2", "package a; @com.adobe.acs.commons.mcp.form.DialogProvider public class Example2 {public String resourceType=\"my.type\";}"));
        assertThat(compilation).succeeded();
        compilation = javac()
                .withProcessors(new DialogProviderAnnotationProcessor())
                .compile(JavaFileObjects.forSourceString("a.Example3", "package a; @org.apache.sling.models.annotations.Model(adaptables=org.apache.sling.api.resource.Resource.class, resourceType=\"my.type\") @com.adobe.acs.commons.mcp.form.DialogProvider public class Example3 {}"));
        assertThat(compilation).succeeded();
    }

    private boolean resourceExists(String path) {
        return slingContext.resourceResolver().getResource(path) != null;
    }

    @DialogProvider
    // This is annotated but does not provide a resource type, so a service should NOT be created
    public static class NoResourceTypeProvided {
    }

    @DialogProvider
    // This is annotated and provides a resource type, so a service SHOULD be created
    public static class ResourceTypeProvided {

        public static String getResourceType() {
            return "my.resource.type";
        }
    }

    @DialogProvider
    @Model(adaptables = Resource.class, resourceType = "my.resource.type")
    // This is annotated and provides a resource type via the model annotation, so a service SHOULD be created
    public static class ResourceTypeProvidedByModel {
    }
}
