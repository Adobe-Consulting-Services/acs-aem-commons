/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2024 Adobe
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