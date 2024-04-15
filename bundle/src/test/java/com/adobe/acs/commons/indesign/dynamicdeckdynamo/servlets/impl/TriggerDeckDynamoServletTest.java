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
package com.adobe.acs.commons.indesign.dynamicdeckdynamo.servlets.impl;

import com.adobe.acs.commons.indesign.dynamicdeckdynamo.exception.DynamicDeckDynamoException;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.services.DynamicDeckService;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class TriggerDeckDynamoServletTest {

    private final AemContext context = new AemContext();

    @Mock
    private DynamicDeckService dynamicDeckService;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource resource;

    @InjectMocks
    private TriggerDeckDynamoServlet triggerDeckDynamoServlet;

    @BeforeEach
    void setUp() {
        context.load().json("/com/adobe/acs/commons/dynamicdeckdynamo/dynamicdeckResources.json", "/content");
        context.registerService(DynamicDeckService.class, dynamicDeckService);

        triggerDeckDynamoServlet = context.registerInjectActivateService(new TriggerDeckDynamoServlet());
    }

    @Test
    void missingSelectorTest() throws ServletException, IOException {
        context.request().setPathInfo("/content/some/en/en/some.json");

        triggerDeckDynamoServlet.doPost(context.request(), context.response());

        assertEquals("{}", context.response().getOutputAsString());
        assertEquals("application/json;charset=UTF-8", context.response().getContentType());
    }


    @ParameterizedTest
    @MethodSource("provideTypes")
    void testDoPostTypeSuccess(String type) throws ServletException, IOException, DynamicDeckDynamoException {
        context.requestPathInfo().setSelectorString("triggerDeckDynamo");

        requestParameterSetup(type);

        if ("QUERY".equals(type)) {
            when(dynamicDeckService.fetchAssetListFromQuery(anyString(), any())).thenReturn(Collections.singletonList(resource));
        } else if ("TAGS".equals(type)) {
            when(dynamicDeckService.fetchAssetListFromTags(anyString(), any())).thenReturn(Collections.singletonList(resource));
        } else if ("COLLECTION".equals(type)) {
            when(dynamicDeckService.fetchAssetListFromCollection(any(), any())).thenReturn(Collections.singletonList(resource));
        }

        when(dynamicDeckService.createDeck(anyString(), any(), any(), any(), any(), any())).thenReturn("/content/deck");

        triggerDeckDynamoServlet.doPost(context.request(), context.response());

        assertTrue(context.response().getOutputAsString().contains("Deck generation triggered successfully!"));
    }

    @ParameterizedTest
    @MethodSource("provideTypesFailed")
    void testDoPostWithTypeFailed(String type) throws ServletException, IOException, DynamicDeckDynamoException {
        context.requestPathInfo().setSelectorString("triggerDeckDynamo");

        if (type != null) {
            requestParameterSetup(type);
        }

        if ("QUERY".equals(type)) {
            when(dynamicDeckService.fetchAssetListFromQuery(anyString(), any())).thenReturn(Collections.emptyList());
        } else if ("TAGS".equals(type)) {
            when(dynamicDeckService.fetchAssetListFromTags(anyString(), any())).thenReturn(Collections.emptyList());
        } else if ("COLLECTION".equals(type)) {
            when(dynamicDeckService.fetchAssetListFromCollection(any(), any())).thenReturn(Collections.emptyList());
        }

        triggerDeckDynamoServlet.doPost(context.request(), context.response());

        assertEquals(500, context.response().getStatus());
    }

    private static Stream<String> provideTypesFailed() {
        return Stream.of("QUERY", "TAGS", "COLLECTION", null, "UNKNOWN");
    }

    private static Stream<String> provideTypes() {
        return Stream.of("QUERY", "TAGS", "COLLECTION");
    }

    @ParameterizedTest
    @MethodSource("provideTypes")
    void testDoPostMissingRequestParameters(String operationMode) throws ServletException, IOException {
        context.requestPathInfo().setSelectorString("triggerDeckDynamo");
        context.request().addRequestParameter("operationMode", operationMode);
        context.request().addRequestParameter("deckTitle", "test");
        context.request().addRequestParameter("destinationPath", "/content/dam/dynamic-deck-dynamo/destination");
        context.request().addRequestParameter("templatePath", "/content/dam/dynamic-deck-dynamo/templates/simple-template-2018");

        triggerDeckDynamoServlet.doPost(context.request(), context.response());

        assertEquals(500, context.response().getStatus());
    }

    private void requestParameterSetup(String operationMode) {
        context.request().addRequestParameter("deckTitle", "test");
        context.request().addRequestParameter("templatePath", "/content/dam/dynamic-deck-dynamo/templates/simple-template-2018");
        context.request().addRequestParameter("masterAssetPath", "/content/dam/dynamic-deck-dynamo/master-assets/get-outside-master-asset.jpg");
        context.request().addRequestParameter("destinationPath", "/content/dam/dynamic-deck-dynamo/destination");
        context.request().addRequestParameter("collectionPath", "/content/dam/dynamic-deck-dynamo/collectionPath");
        context.request().addRequestParameter("operationMode", operationMode);
        context.request().addRequestParameter("queryString", "queryString");
        context.request().addRequestParameter("tagValues", "tagValues");
    }
}