package com.adobe.acs.commons.wcm.properties.shared.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.adobe.acs.commons.wcm.impl.PageRootProviderConfig;
import com.adobe.acs.commons.wcm.impl.PageRootProviderMultiImpl;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.Template;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.api.reference.Reference;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.util.List;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

@ExtendWith(AemContextExtension.class)
class SharedComponentPropertiesReferenceProviderTest {

    SharedComponentPropertiesReferenceProvider sharedPropertiesReferenceProvider;
    private AemContext aemContext;

    private static final String CONTENT_PATH = "/content/sample-site";
    private static final String SHARED_PROPS_REFERENCE_PROVIDER_BASE = "/com/adobe/acs/commons/wcm/shared/impl/shared-properties-reference-provider";
    private static final String APPS_COMPONENTS_PATH = "/apps/sample-site/components";
    private static final String TEST_CONTENT_JSON = "/test-content.json";
    private static final String APPS_CONTENT_JSON = "/apps-content.json";
    private static final String TEMPLATE_CONTENT_JSON = "/template-content.json";

    private static final String ENGLISH_PAGE_PATH = "/content/sample-site/en";
    private static final String HOME_PAGE_PATH = "/content/sample-site/en/home";
    private static final String CONTENT_TEMPLATE_PATH = "/conf/sample-site/settings/wcm/templates/content-template";

    private Resource resource;
    private ResourceResolver resourceResolver;
    private PageManager pageManager;
    private Resource componentResource;

    @BeforeEach
    private void setUp() {
        aemContext = new AemContext();

        aemContext.registerInjectActivateService(new PageRootProviderConfig(), "page.root.path", "/content/sample-site/en");
        aemContext.registerInjectActivateService(new PageRootProviderMultiImpl());
        aemContext.registerService(ResourceResolverFactory.class, new MockResourceResolverFactory());
        sharedPropertiesReferenceProvider = aemContext.registerInjectActivateService(new SharedComponentPropertiesReferenceProvider());

        aemContext.load().json(SHARED_PROPS_REFERENCE_PROVIDER_BASE + TEST_CONTENT_JSON, CONTENT_PATH);
        aemContext.load().json(SHARED_PROPS_REFERENCE_PROVIDER_BASE + APPS_CONTENT_JSON, APPS_COMPONENTS_PATH);
        aemContext.load().json(SHARED_PROPS_REFERENCE_PROVIDER_BASE + TEMPLATE_CONTENT_JSON, CONTENT_TEMPLATE_PATH);

        resource = Mockito.mock(Resource.class);
        resourceResolver = Mockito.mock(ResourceResolver.class);
        pageManager = Mockito.mock(PageManager.class);
        Page page = aemContext.currentPage(HOME_PAGE_PATH);
        Page rootPage = aemContext.currentPage(ENGLISH_PAGE_PATH);
        Resource templateResource = aemContext.currentResource(CONTENT_TEMPLATE_PATH);
        componentResource = Mockito.mock(Resource.class);
        ResourceResolver componentResolver = Mockito.mock(ResourceResolver.class);
        Component component = Mockito.mock(Component.class);
        Resource sharedDialog = Mockito.mock(Resource.class);

        when(resourceResolver.getSearchPath()).thenReturn(new String[]{"/apps/", "/libs/"});

        when(resource.getPath()).thenReturn("/content/sample-site/en/jcr:content/root/header");
        when(resource.getResourceResolver()).thenReturn(resourceResolver);

        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(resourceResolver.getResource("/apps/sample-site/components/structure/header")).thenReturn(componentResource);
        when(resourceResolver.getResource(CONTENT_TEMPLATE_PATH)).thenReturn(templateResource);

        when(pageManager.getContainingPage(resource)).thenReturn(page);
        when(pageManager.getPage(CONTENT_TEMPLATE_PATH)).thenReturn(page);

        when(pageManager.getPage(ENGLISH_PAGE_PATH)).thenReturn(rootPage);

        when(componentResource.adaptTo(Component.class)).thenReturn(component);
        when(componentResource.getResourceResolver()).thenReturn(componentResolver);
        when(componentResource.getPath()).thenReturn("/content/sample-site/en");
        when(componentResource.getResourceResolver()
            .getResource(componentResource.getPath() + "/dialogshared")).thenReturn(sharedDialog);

    }


    @Test
    public void testFindReferences() {
        Page page = aemContext.currentPage(HOME_PAGE_PATH);
        Page templatePage = Mockito.spy(page);
        when(pageManager.getContainingPage(resource)).thenReturn(templatePage);

        Template template = Mockito.mock(Template.class);
        Resource templateResource = Mockito.mock(Resource.class);
        when(templatePage.getTemplate()).thenReturn(template);
        when(templateResource.adaptTo(Template.class)).thenReturn(template);
        when(resourceResolver.getResource(templatePage.getTemplate().getPath())).thenReturn(aemContext.currentResource(CONTENT_TEMPLATE_PATH));


        Component component = Mockito.mock(Component.class);
        Resource textResource = aemContext.currentResource("/apps/sample-site/components/content/text");
        Resource spyTextResource = Mockito.spy(textResource);
        when(resourceResolver.getResource("/apps/sample-site/components/content/text")).thenReturn(spyTextResource);
        when(spyTextResource.adaptTo(Component.class)).thenReturn(component);

        Resource titleResource = aemContext.currentResource("/apps/sample-site/components/content/title");
        Resource spyTitleResource = Mockito.spy(titleResource);
        when(resourceResolver.getResource("/apps/sample-site/components/content/title")).thenReturn(spyTitleResource);
        when(spyTitleResource.adaptTo(Component.class)).thenReturn(component);

        Resource headerResource = aemContext.currentResource("/apps/sample-site/components/structure/header");
        Resource spyHeaderResource = Mockito.spy(headerResource);
        when(resourceResolver.getResource("/apps/sample-site/components/structure/header")).thenReturn(spyHeaderResource);
        when(spyHeaderResource.adaptTo(Component.class)).thenReturn(component);

        Resource footerResource = aemContext.currentResource("/apps/sample-site/components/structure/footer");
        Resource spyFooterResource = Mockito.spy(footerResource);
        when(resourceResolver.getResource("/apps/sample-site/components/structure/footer")).thenReturn(spyFooterResource);
        when(spyFooterResource.adaptTo(Component.class)).thenReturn(component);

        List<Reference> references = sharedPropertiesReferenceProvider.findReferences(resource);
        assertEquals(2, references.size());
        assertEquals("English", references.get(0).getName());
    }

    @Test
    public void testNullPageManager() {
        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(null);

        List<Reference> references = sharedPropertiesReferenceProvider.findReferences(resource);
        assertEquals(0, references.size());
    }

    @Test
    public void testNullPage() {
        when(pageManager.getContainingPage(resource)).thenReturn(null);

        List<Reference> references = sharedPropertiesReferenceProvider.findReferences(resource);
        assertEquals(0, references.size());
    }

    @Test
    public void testNullComponentResource() {
        when(resourceResolver.getResource("/apps/project/components/react/header"))
            .thenReturn(null);

        List<Reference> references = sharedPropertiesReferenceProvider.findReferences(resource);
        assertEquals(0, references.size());
    }

    @Test
    public void testNullComponent() {
        when(componentResource.adaptTo(Component.class)).thenReturn(null);

        List<Reference> references = sharedPropertiesReferenceProvider.findReferences(resource);
        assertEquals(0, references.size());
    }

    @Test
    public void testNullSharedDialog() {
        when(componentResource.getResourceResolver()
            .getResource(componentResource.getPath() + "/dialogshared")).thenReturn(null);

        List<Reference> references = sharedPropertiesReferenceProvider.findReferences(resource);
        assertEquals(0, references.size());
    }

    @Test
    public void testNonContentReferences() {
        Resource resource = aemContext.currentResource(CONTENT_TEMPLATE_PATH);

        List<Reference> references = sharedPropertiesReferenceProvider.findReferences(resource);
        assertEquals(0, references.size());
    }

}

