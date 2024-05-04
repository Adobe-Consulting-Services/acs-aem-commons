package com.adobe.acs.commons.indesign.dynamicdeckdynamo.services.impl;

import com.adobe.acs.commons.indesign.dynamicdeckdynamo.exception.DynamicDeckDynamoException;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.services.DynamicDeckConfigurationService;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.services.XMLGeneratorService;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.JobManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertThrows;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class DynamicDeckServiceImplTest {

    private final AemContext context = new AemContext();

    private ResourceResolver resourceResolver;

    @Mock
    private DynamicDeckConfigurationService configurationService;

    @Mock
    private XMLGeneratorService xmlGeneratorService;

    @Mock
    private JobManager jobManager;

    @InjectMocks
    private DynamicDeckServiceImpl dynamicDeckService;

    @BeforeEach
    void setUp() {
        context.load().json("/com/adobe/acs/commons/dynamicdeckdynamo/dynamicdeckResources.json", "/content");
        resourceResolver = context.resourceResolver();

        context.registerService(DynamicDeckConfigurationService.class, configurationService);
        context.registerService(XMLGeneratorService.class, xmlGeneratorService);
        context.registerService(JobManager.class, jobManager);
        dynamicDeckService = context.registerInjectActivateService(new DynamicDeckServiceImpl());
    }

    @Test
    void shouldThrowExceptionInCaseOfTemplateAbsents() {
        context.request().setPathInfo("/content/some/en/en/some.json");
        List<Resource> assetResourceList = Collections.singletonList(Mockito.mock(Resource.class));
        Resource masterAssetResource = resourceResolver.getResource("/content/dam/dynamic-deck-dynamo/master-assets");
        Resource templateFolderResource = resourceResolver.getResource("/content/dam/dynamic-deck-dynamo/templates/simple-template-2018");
        Resource destinationFolderResource = resourceResolver.getResource("/content/dam/dynamic-deck-dynamo/destination");

        assertThrows(DynamicDeckDynamoException.class, () ->
                dynamicDeckService.createDeck("deckTitle", masterAssetResource, assetResourceList, templateFolderResource,
                        destinationFolderResource, resourceResolver));
    }

    @Test
    void shouldThrowExceptionInCaseOfDamAssetResourceNull() {
        context.request().setPathInfo("/content/some/en/en/some.json");
        List<Resource> assetResourceList = Collections.singletonList(Mockito.mock(Resource.class));
        Resource masterAssetResource = resourceResolver.getResource("/content/dam/dynamic-deck-dynamo/master-assets");
        Resource templateFolderResource = resourceResolver.getResource("/content/dam/dynamic-deck-dynamo/templates/simple-template-2019");
        Resource destinationFolderResource = resourceResolver.getResource("/content/dam/dynamic-deck-dynamo/destination");

        assertThrows(DynamicDeckDynamoException.class, () ->
                dynamicDeckService.createDeck("deckTitle", masterAssetResource, assetResourceList, templateFolderResource,
                        destinationFolderResource, resourceResolver));
    }
}