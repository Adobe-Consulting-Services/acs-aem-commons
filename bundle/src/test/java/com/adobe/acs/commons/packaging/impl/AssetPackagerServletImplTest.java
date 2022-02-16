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

package com.adobe.acs.commons.packaging.impl;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;

import com.adobe.acs.commons.packaging.PackageHelper;
import com.day.cq.wcm.api.NameConstants;

import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.impl.JcrPackageManagerImpl;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class AssetPackagerServletImplTest {

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

//    @InjectMocks
    @Mock
    Packaging packaging;

    @InjectMocks
    final AssetPackagerServletImpl assetPackagerServlet = spy(new AssetPackagerServletImpl());

    @InjectMocks
    final PackageHelperImpl packageHelper = new PackageHelperImpl();

    private MockSlingHttpServletRequest request;

    private MockSlingHttpServletResponse response;

    private static final String ROOT_PATH = "rootPath";
    private static final String PREVIEW = "preview";
    private static final String STATUS = "status";
    private static final String SUCCESS_MESSAGE = "success";
    private static final String FILTER_OBJECT = "filterSets";
    private static final String PACKAGE_PATH_PARAM = "path";
    private static final String PACKAGE_PATH = "/etc/packages/Assets/assets-1.0.0.zip";
    private static final String PACKAGER_CONTENT_PATH = "/etc/acs-commons/packagers/assets";
    private static final String PACKAGER_JCR_CONTENT_PATH = PACKAGER_CONTENT_PATH + "/" + NameConstants.NN_CONTENT;

    private final String[] previewResults = {"/content/we-retail/language-masters/en/experience",
            "/content/dam/we-retail/en/experiences/48-hours-of-wilderness/48-hours-of-wilderness-1.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-01.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/majestic-rainbow.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/northern-lights.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/camp-fire.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/camp-tent.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-02.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-ready-to-go.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/fjord-waves.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/arctic-surfing-in-lofoten",
            "/content/dam/we-retail/en/activities/hiking-camping/trekker-ama-dablam.jpg"};

    private final String[] assetExclusionResults = {"/content/we-retail/language-masters/en/experience",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-01.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/majestic-rainbow.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/northern-lights.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/camp-fire.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/camp-tent.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-02.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-ready-to-go.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/fjord-waves.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/arctic-surfing-in-lofoten",
            "/content/dam/we-retail/en/activities/hiking-camping/trekker-ama-dablam.jpg"};

    private final String[] pageExclusionResults = {"/content/we-retail/language-masters/en/experience",
            "/content/dam/we-retail/en/experiences/48-hours-of-wilderness/48-hours-of-wilderness-1.jpg",
            "/content/dam/we-retail/en/activities/hiking-camping/trekker-ama-dablam.jpg"};

    private final String[] assetPrefixResults = {"/content/we-retail/language-masters/en/experience",
            "/content/dam/we-retail/en/experiences/48-hours-of-wilderness/48-hours-of-wilderness-1.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-01.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/majestic-rainbow.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/northern-lights.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/camp-fire.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/camp-tent.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-02.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-ready-to-go.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/fjord-waves.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/arctic-surfing-in-lofoten"};

    private final String[] multipleAssetExclusionResults = {"/content/we-retail/language-masters/en/experience",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/majestic-rainbow.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/northern-lights.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/camp-fire.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/camp-tent.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-wave-02.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/surfer-ready-to-go.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/fjord-waves.jpg",
            "/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/arctic-surfing-in-lofoten",
            "/content/dam/we-retail/en/activities/hiking-camping/trekker-ama-dablam.jpg"};

    private final String[] multiplePageExclusionResults = {"/content/we-retail/language-masters/en/experience",
            "/content/dam/we-retail/en/activities/hiking-camping/trekker-ama-dablam.jpg"};

    @Before
    public void setup() {
        context.load().json(getClass().getResourceAsStream("AssetPackagerServletContent.json"), "/content/we-retail/language-masters/en/experience");
        context.load().json(getClass().getResourceAsStream("AssetPackagerServletDamContent.json"), "/content/dam/we-retail/en");
        context.load().json(getClass().getResourceAsStream("AssetPackagerServletPackagesContent.json"), "/etc/packages");

        request = new MockSlingHttpServletRequest(context.resourceResolver(), context.bundleContext());
        response = context.response();

        doReturn(packageHelper).when(assetPackagerServlet).getPackageHelper();
        context.registerService(PackageHelper.class, packageHelper);
        when(packaging.getPackageManager(any(Session.class))).thenReturn(new JcrPackageManagerImpl(context.resourceResolver().adaptTo(Session.class), new String[0]));
    }

    @Test
    public void testPreview() throws Exception {
        context.load().json(getClass().getResourceAsStream("AssetPackagerServletConfiguration.json"), PACKAGER_CONTENT_PATH);
        request.setResource(context.resourceResolver().getResource(PACKAGER_JCR_CONTENT_PATH));
        Map<String, Object> params = new HashMap<>();
        params.put(PREVIEW, true);
        request.setParameterMap(params);
        assetPackagerServlet.doPost(request, response);
        JSONObject responseJSON = new JSONObject(response.getOutputAsString());
        assertEquals(responseJSON.getString(STATUS), PREVIEW);
        JSONArray filterSets = responseJSON.getJSONArray(FILTER_OBJECT);
        assertEquals(filterSets.length(), previewResults.length);
        for (int i = 0; i < previewResults.length; i++) {
            assertEquals(filterSets.getJSONObject(i).getString(ROOT_PATH), previewResults[i]);
        }
    }

    @Test
    public void testAssetExclusion() throws Exception {
        context.load().json(getClass().getResourceAsStream("AssetPackagerServletConfiguration2.json"), PACKAGER_CONTENT_PATH);
        request.setResource(context.resourceResolver().getResource(PACKAGER_JCR_CONTENT_PATH));
        Map<String, Object> params = new HashMap<>();
        params.put(PREVIEW, false);
        request.setParameterMap(params);
        assetPackagerServlet.doPost(request, response);
        JSONObject responseJSON = new JSONObject(response.getOutputAsString());
        assertEquals(responseJSON.getString(STATUS), SUCCESS_MESSAGE);
        assertEquals(responseJSON.getString(PACKAGE_PATH_PARAM), PACKAGE_PATH);
        JSONArray filterSets = responseJSON.getJSONArray(FILTER_OBJECT);
        assertEquals(filterSets.length(), assetExclusionResults.length);
        for (int i = 0; i < assetExclusionResults.length; i++) {
            assertEquals(filterSets.getJSONObject(i).getString(ROOT_PATH), assetExclusionResults[i]);
        }
    }

    @Test
    public void testPageExclusion() throws Exception {
        context.load().json(getClass().getResourceAsStream("AssetPackagerServletConfiguration3.json"), PACKAGER_CONTENT_PATH);
        request.setResource(context.resourceResolver().getResource(PACKAGER_JCR_CONTENT_PATH));
        Map<String, Object> params = new HashMap<>();
        params.put(PREVIEW, false);
        request.setParameterMap(params);
        assetPackagerServlet.doPost(request, response);
        JSONObject responseJSON = new JSONObject(response.getOutputAsString());
        assertEquals(responseJSON.getString(STATUS), SUCCESS_MESSAGE);
        assertEquals(responseJSON.getString(PACKAGE_PATH_PARAM), PACKAGE_PATH);
        JSONArray filterSets = responseJSON.getJSONArray(FILTER_OBJECT);
        assertEquals(filterSets.length(), pageExclusionResults.length);
        for (int i = 0; i < pageExclusionResults.length; i++) {
            assertEquals(filterSets.getJSONObject(i).getString(ROOT_PATH), pageExclusionResults[i]);
        }
    }

    @Test
    public void testAssetPrefix() throws Exception {
        context.load().json(getClass().getResourceAsStream("AssetPackagerServletConfiguration4.json"), PACKAGER_CONTENT_PATH);
        request.setResource(context.resourceResolver().getResource(PACKAGER_JCR_CONTENT_PATH));
        Map<String, Object> params = new HashMap<>();
        params.put(PREVIEW, false);
        request.setParameterMap(params);
        assetPackagerServlet.doPost(request, response);
        JSONObject responseJSON = new JSONObject(response.getOutputAsString());
        assertEquals(responseJSON.getString(STATUS), SUCCESS_MESSAGE);
        assertEquals(responseJSON.getString(PACKAGE_PATH_PARAM), PACKAGE_PATH);
        JSONArray filterSets = responseJSON.getJSONArray(FILTER_OBJECT);
        assertEquals(filterSets.length(), assetPrefixResults.length);
        for (int i = 0; i < assetPrefixResults.length; i++) {
            assertEquals(filterSets.getJSONObject(i).getString(ROOT_PATH), assetPrefixResults[i]);
        }
    }

    @Test
    public void testMultipleAssetExclusions() throws Exception {
        context.load().json(getClass().getResourceAsStream("AssetPackagerServletConfiguration5.json"), PACKAGER_CONTENT_PATH);
        request.setResource(context.resourceResolver().getResource(PACKAGER_JCR_CONTENT_PATH));
        Map<String, Object> params = new HashMap<>();
        params.put(PREVIEW, false);
        request.setParameterMap(params);
        assetPackagerServlet.doPost(request, response);
        JSONObject responseJSON = new JSONObject(response.getOutputAsString());
        assertEquals(responseJSON.getString(STATUS), SUCCESS_MESSAGE);
        assertEquals(responseJSON.getString(PACKAGE_PATH_PARAM), PACKAGE_PATH);
        JSONArray filterSets = responseJSON.getJSONArray(FILTER_OBJECT);
        assertEquals(filterSets.length(), multipleAssetExclusionResults.length);
        for (int i = 0; i < multipleAssetExclusionResults.length; i++) {
            assertEquals(filterSets.getJSONObject(i).getString(ROOT_PATH), multipleAssetExclusionResults[i]);
        }
    }

    @Test
    public void testMultiplePageExclusions() throws Exception {
        context.load().json(getClass().getResourceAsStream("AssetPackagerServletConfiguration6.json"), PACKAGER_CONTENT_PATH);
        request.setResource(context.resourceResolver().getResource(PACKAGER_JCR_CONTENT_PATH));
        Map<String, Object> params = new HashMap<>();
        params.put(PREVIEW, false);
        request.setParameterMap(params);
        assetPackagerServlet.doPost(request, response);
        JSONObject responseJSON = new JSONObject(response.getOutputAsString());
        assertEquals(responseJSON.getString(STATUS), SUCCESS_MESSAGE);
        assertEquals(responseJSON.getString(PACKAGE_PATH_PARAM), PACKAGE_PATH);
        JSONArray filterSets = responseJSON.getJSONArray(FILTER_OBJECT);
        assertEquals(filterSets.length(), multiplePageExclusionResults.length);
        for (int i = 0; i < multiplePageExclusionResults.length; i++) {
            assertEquals(filterSets.getJSONObject(i).getString(ROOT_PATH), multiplePageExclusionResults[i]);
        }
    }
}
