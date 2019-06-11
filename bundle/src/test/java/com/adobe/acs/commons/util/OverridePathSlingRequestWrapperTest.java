package com.adobe.acs.commons.util;

import com.adobe.cq.sightly.WCMBindings;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.jcr.RepositoryException;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class OverridePathSlingRequestWrapperTest {
    private static final String RESOURCE_PATH = "/content/child";
    private static final String PAGE_PATH = "/content/childPage";
    private static final String PAGE_CHILD_RESOURCE_PATH = "/content/childPage/jcr:content/some/child";

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Before
    public void setup() throws RepositoryException {
        this.context.create().resource(RESOURCE_PATH, "prop", "resourceval");
        this.context.create().page(PAGE_PATH, "prop", "pageval");
        this.context.create().resource(PAGE_CHILD_RESOURCE_PATH, "prop", "childval");
    }

    @Test
    public void testGetAttributes() {
        this.context.currentResource(RESOURCE_PATH);

        this.context.request().setAttribute("testattr", "testval");
        OverridePathSlingRequestWrapper wrapper = new OverridePathSlingRequestWrapper(this.context.request(), PAGE_CHILD_RESOURCE_PATH);

        assertEquals("testval", wrapper.getAttribute("testattr"));

        Map<String, Object> originalBindings = (Map<String, Object>) this.context.request().getAttribute(SlingBindings.class.getName());
        Map<String, Object> wrappedBindings = (Map<String, Object>) wrapper.getAttribute(SlingBindings.class.getName());

        assertNotNull(originalBindings);
        assertNotNull(wrappedBindings);
        assertNotEquals(originalBindings, wrappedBindings);
        assertEquals(RESOURCE_PATH, ((Resource) originalBindings.get(SlingBindings.RESOURCE)).getPath());
        assertEquals(PAGE_CHILD_RESOURCE_PATH, ((Resource) wrappedBindings.get(SlingBindings.RESOURCE)).getPath());
    }

    @Test
    public void testBindingsForPage() {
        this.context.currentResource(RESOURCE_PATH);

        OverridePathSlingRequestWrapper pageResourceWrapper = new OverridePathSlingRequestWrapper(this.context.request(), PAGE_CHILD_RESOURCE_PATH);
        Map<String, Object> pageResourceBindings = (Map<String, Object>) pageResourceWrapper.getAttribute(SlingBindings.class.getName());
        assertNotNull(pageResourceBindings.get(WCMBindings.CURRENT_PAGE));
        assertEquals(PAGE_PATH, ((Page) pageResourceBindings.get(WCMBindings.CURRENT_PAGE)).getPath());

        OverridePathSlingRequestWrapper pageWrapper = new OverridePathSlingRequestWrapper(this.context.request(), PAGE_PATH);
        Map<String, Object> pageBindings = (Map<String, Object>) pageWrapper.getAttribute(SlingBindings.class.getName());
        assertNotNull(pageBindings.get(WCMBindings.CURRENT_PAGE));
        assertEquals(PAGE_PATH, ((Page) pageBindings.get(WCMBindings.CURRENT_PAGE)).getPath());

        OverridePathSlingRequestWrapper resourceWrapper = new OverridePathSlingRequestWrapper(this.context.request(), RESOURCE_PATH);
        Map<String, Object> resourceWrapperBindings = (Map<String, Object>) resourceWrapper.getAttribute(SlingBindings.class.getName());
        assertNull(resourceWrapperBindings.get(WCMBindings.CURRENT_PAGE));
    }

    @Test
    public void testBindingsForResource() {
        this.context.currentResource(RESOURCE_PATH);

        OverridePathSlingRequestWrapper pageResourceWrapper = new OverridePathSlingRequestWrapper(this.context.request(), PAGE_CHILD_RESOURCE_PATH);
        Map<String, Object> pageResourceBindings = (Map<String, Object>) pageResourceWrapper.getAttribute(SlingBindings.class.getName());
        assertNotNull(pageResourceBindings.get(SlingBindings.RESOURCE));
        assertEquals(PAGE_CHILD_RESOURCE_PATH, ((Resource) pageResourceBindings.get(SlingBindings.RESOURCE)).getPath());
        assertNotNull(pageResourceBindings.get(WCMBindings.PROPERTIES));
        assertEquals("childval", ((ValueMap) pageResourceBindings.get(WCMBindings.PROPERTIES)).get("prop"));

        OverridePathSlingRequestWrapper resourceWrapper = new OverridePathSlingRequestWrapper(this.context.request(), RESOURCE_PATH);
        Map<String, Object> resourceWrapperBindings = (Map<String, Object>) resourceWrapper.getAttribute(SlingBindings.class.getName());
        assertNotNull(resourceWrapperBindings.get(SlingBindings.RESOURCE));
        assertEquals(RESOURCE_PATH, ((Resource) resourceWrapperBindings.get(SlingBindings.RESOURCE)).getPath());
        assertNotNull(resourceWrapperBindings.get(WCMBindings.PROPERTIES));
        assertEquals("resourceval", ((ValueMap) resourceWrapperBindings.get(WCMBindings.PROPERTIES)).get("prop"));
    }

    @Test
    public void testRelativePath() {
        this.context.currentResource("/content");

        OverridePathSlingRequestWrapper resourceWrapper = new OverridePathSlingRequestWrapper(this.context.request(), "child");
        Map<String, Object> resourceWrapperBindings = (Map<String, Object>) resourceWrapper.getAttribute(SlingBindings.class.getName());
        assertNotNull(resourceWrapperBindings.get(WCMBindings.PROPERTIES));
        assertEquals("resourceval", ((ValueMap) resourceWrapperBindings.get(WCMBindings.PROPERTIES)).get("prop"));
    }

    @Test
    public void testResource() {
        this.context.currentResource(new NonExistingResource(this.context.resourceResolver(), "/content/bogus"));

        OverridePathSlingRequestWrapper pageResourceWrapper = new OverridePathSlingRequestWrapper(this.context.request(), PAGE_CHILD_RESOURCE_PATH);
        assertEquals(PAGE_CHILD_RESOURCE_PATH, pageResourceWrapper.getResource().getPath());

        OverridePathSlingRequestWrapper resourceWrapper = new OverridePathSlingRequestWrapper(this.context.request(), RESOURCE_PATH);
        assertEquals(RESOURCE_PATH, resourceWrapper.getResource().getPath());
    }
}
