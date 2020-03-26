package com.adobe.acs.commons.wcm.properties.shared.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.adobe.acs.commons.wcm.impl.PageRootProviderConfig;
import com.adobe.acs.commons.wcm.impl.PageRootProviderMultiImpl;
import com.day.cq.wcm.api.PageManager;
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

    private static final String LANGUAGE_ROOT_PAGE_PATH = "/content/sample-site/en";
    private static final String HOME_PAGE_PATH = "/content/sample-site/en/home";
    private static final String CONTENT_TEMPLATE_PATH = "/conf/sample-site/settings/wcm/templates/content-page";

    @BeforeEach
    private void setUp() {
        aemContext = new AemContext();

        aemContext.registerInjectActivateService(new PageRootProviderConfig(), "page.root.path", LANGUAGE_ROOT_PAGE_PATH);
        aemContext.registerInjectActivateService(new PageRootProviderMultiImpl());

        MockResourceResolverFactory mockResourceResolverFactory = mock(MockResourceResolverFactory.class);
        aemContext.registerService(ResourceResolverFactory.class, mockResourceResolverFactory);

        sharedPropertiesReferenceProvider = aemContext.registerInjectActivateService(new SharedComponentPropertiesReferenceProvider());

        aemContext.load().json(SHARED_PROPS_REFERENCE_PROVIDER_BASE + TEST_CONTENT_JSON, CONTENT_PATH);
        aemContext.load().json(SHARED_PROPS_REFERENCE_PROVIDER_BASE + APPS_CONTENT_JSON, APPS_COMPONENTS_PATH);
        aemContext.load().json(SHARED_PROPS_REFERENCE_PROVIDER_BASE + TEMPLATE_CONTENT_JSON, CONTENT_TEMPLATE_PATH);

    }


    @Test
    public void testFindReferences() {

        Resource resource = aemContext.currentResource(HOME_PAGE_PATH);

        List<Reference> references = sharedPropertiesReferenceProvider.findReferences(resource);
        assertEquals(2, references.size());
        assertEquals("English", references.get(0).getName());
    }

    @Test
    public void testNullPageManager() {
        Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn("/content/mock-resource");
        when(resource.getResourceResolver()).thenReturn(mock(ResourceResolver.class));

        List<Reference> references = sharedPropertiesReferenceProvider.findReferences(resource);
        assertEquals(0, references.size());
    }

    @Test
    public void testNullPage() {
        Resource resource = mock(Resource.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        when(resource.getPath()).thenReturn("/content/mock-resource");
        when(resource.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(mock(PageManager.class));

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

