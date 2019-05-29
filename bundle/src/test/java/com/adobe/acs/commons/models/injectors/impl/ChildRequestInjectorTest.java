package com.adobe.acs.commons.models.injectors.impl;

import com.adobe.acs.commons.models.injectors.annotation.impl.ChildRequestAnnotationProcessorFactory;
import com.adobe.acs.commons.models.injectors.annotation.impl.SharedValueMapValueAnnotationProcessorFactory;
import com.adobe.acs.commons.models.injectors.impl.model.TestModelChildRequest;
import com.adobe.acs.commons.models.injectors.impl.model.impl.TestModelChildRequestChildImpl;
import com.adobe.acs.commons.models.injectors.impl.model.impl.TestModelChildRequestImpl;
import com.adobe.acs.commons.models.injectors.impl.model.impl.TestSharedValueMapValueModelImpl;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.jcr.RepositoryException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ChildRequestInjectorTest {
    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    private ResourceResolver resourceResolver;

    private TestModelChildRequest testModel;

    @Before
    public void setup() throws RepositoryException {
        this.context.registerInjectActivateService(new ChildRequestAnnotationProcessorFactory());
        this.context.registerInjectActivateService(new ChildRequestInjector());
        this.context.addModelsForClasses(TestModelChildRequestImpl.class);
        this.context.addModelsForClasses(TestModelChildRequestChildImpl.class);

        Resource testResource = this.context.create().resource("/content/child", "prop", "val1");
        this.context.create().resource("/content/childList/1", "prop", "val2");
        this.context.create().resource("/content/childList/2", "prop", "val3");

        this.context.currentResource("/content");
    }

    @Test
    public void testInjectedChildModelFromRequest() {
        testModel = this.context.request().adaptTo(TestModelChildRequest.class);
        assertEquals("val1", testModel.getChildModel().getProp());
        assertEquals("/content/child", testModel.getChildModel().getResource().getPath());
        assertEquals("/content/child", testModel.getChildModel().getRequest().getResource().getPath());
    }

    @Test
    public void testInjectedChildModelFromResource() {
        testModel = this.context.currentResource().adaptTo(TestModelChildRequest.class);
        assertEquals("val1", testModel.getChildModel().getProp());
        assertEquals("/content/child", testModel.getChildModel().getResource().getPath());
        assertNull("/content/child", testModel.getChildModel().getRequest());
    }

    @Test
    public void testInjectedChildModelListFromRequest() {
        testModel = this.context.request().adaptTo(TestModelChildRequest.class);

        assertEquals(2, testModel.getChildModelList().size());

        assertEquals("/content/childList/1", testModel.getChildModelList().get(0).getResource().getPath());
        assertEquals("/content/childList/1", testModel.getChildModelList().get(0).getRequest().getResource().getPath());
        assertEquals("val2", testModel.getChildModelList().get(0).getProp());

        assertEquals("/content/childList/2", testModel.getChildModelList().get(1).getResource().getPath());
        assertEquals("/content/childList/2", testModel.getChildModelList().get(1).getRequest().getResource().getPath());
        assertEquals("val3", testModel.getChildModelList().get(1).getProp());
    }

    @Test
    public void testInjectedChildModelListFromResource() {
        testModel = this.context.currentResource().adaptTo(TestModelChildRequest.class);

        assertEquals(2, testModel.getChildModelList().size());

        assertEquals("/content/childList/1", testModel.getChildModelList().get(0).getResource().getPath());
        assertNull(testModel.getChildModelList().get(0).getRequest());
        assertEquals("val2", testModel.getChildModelList().get(0).getProp());

        assertEquals("/content/childList/2", testModel.getChildModelList().get(1).getResource().getPath());
        assertNull(testModel.getChildModelList().get(1).getRequest());
        assertEquals("val3", testModel.getChildModelList().get(1).getProp());
    }

    @Test
    public void testInjectedChildResourceFromRequest() {
        testModel = this.context.request().adaptTo(TestModelChildRequest.class);
        assertEquals("val1", testModel.getChildResource().getValueMap().get("prop", String.class));
    }

    @Test
    public void testInjectedChildResourceFromResource() {
        testModel = this.context.currentResource().adaptTo(TestModelChildRequest.class);
        assertEquals("val1", testModel.getChildResource().getValueMap().get("prop", String.class));
    }

    @Test
    public void testInjectedChildResourceListFromRequest() {
        testModel = this.context.request().adaptTo(TestModelChildRequest.class);
        assertEquals(2, testModel.getChildResourceList().size());
        assertEquals("val2", testModel.getChildResourceList().get(0).getValueMap().get("prop", String.class));
        assertEquals("val3", testModel.getChildResourceList().get(1).getValueMap().get("prop", String.class));
    }

    @Test
    public void testInjectedChildResourceListFromResource() {
        testModel = this.context.currentResource().adaptTo(TestModelChildRequest.class);
        assertEquals(2, testModel.getChildResourceList().size());
        assertEquals("val2", testModel.getChildResourceList().get(0).getValueMap().get("prop", String.class));
        assertEquals("val3", testModel.getChildResourceList().get(1).getValueMap().get("prop", String.class));
    }
}
