/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.wcm.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.day.cq.commons.Externalizer;

public class AuthorUIHelperImplTest {

    @Mock
    private Externalizer externalizer;

    @InjectMocks
    private AuthorUIHelperImpl authorUIHelper;

    private Map<String, String> osgiConfig;

    private static String pagePath = "/content/main/testPage";

    private static String assetPath = "/content/dam/testAsset.jpg";

    @Before
    public final void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        //set up config service
        osgiConfig = new HashMap<String, String>();

    }

    @Test
    public void testIsTouchUIDefault() {
        authorUIHelper.activate(osgiConfig);
        assertTrue(authorUIHelper.isTouchUI());
    }

    @Test
    public void testIsTouchUIClassic() {

        osgiConfig.put("isTouch", "false");
        authorUIHelper.activate(osgiConfig);
        assertFalse(authorUIHelper.isTouchUI());
    }

    @Test
    public void testGenerateEditPageLinkDefault() {
        String touchPageUrlRel = "/editor.html/content/main/testPage.html";
        String touchPageUrlAbs = "http://localhost:4502/editor.html/content/main/testPage.html";
        when(externalizer.authorLink(null, touchPageUrlRel)).thenReturn(touchPageUrlAbs);

        authorUIHelper.activate(osgiConfig);
        String relAuthorPageLink = authorUIHelper.generateEditPageLink(pagePath, false, null);
        String absAuthorPageLink = authorUIHelper.generateEditPageLink(pagePath, true, null);
        assertEquals(touchPageUrlRel, relAuthorPageLink);
        assertEquals(touchPageUrlAbs, absAuthorPageLink);
    }

    @Test
    public void testGenerateEditPageLinkOverride() {
        String touchPageUrlOverride = "/override.html/content/main/testPage.html";

        //override touch ui page editor
        osgiConfig.put("wcmEditorTouchURL", "/override.html");
        authorUIHelper.activate(osgiConfig);
        String overrideAuthorPageLink = authorUIHelper.generateEditPageLink(pagePath, false, null);
        assertEquals(touchPageUrlOverride, overrideAuthorPageLink);
    }

    @Test
    public void testGenerateEditPageLinkClassic() {
        String classicPageUrlRel = "/cf#/content/main/testPage.html";
        String classicPageUrlAbs = "http://localhost:4502/cf#/content/main/testPage.html";

        when(externalizer.authorLink(null, classicPageUrlRel)).thenReturn(classicPageUrlAbs);
        osgiConfig.put("isTouch", "false");
        authorUIHelper.activate(osgiConfig);

        String relAuthorPageLink = authorUIHelper.generateEditPageLink(pagePath, false, null);
        String absAuthorPageLink = authorUIHelper.generateEditPageLink(pagePath, true, null);
        assertEquals(classicPageUrlRel, relAuthorPageLink);
        assertEquals(classicPageUrlAbs, absAuthorPageLink);
    }

    @Test
    public void testGenerateEditPageLinkOverrideClassic() {
        final String classicPageUrlOverride = "/override.html/content/main/testPage.html";

        //override classic ui page editor
        osgiConfig.put("isTouch", "false");
        osgiConfig.put("wcmEditorClassicURL", "/override.html");

        authorUIHelper.activate(osgiConfig);
        String overrideAuthorPageLink = authorUIHelper.generateEditPageLink(pagePath, false, null);
        assertEquals(classicPageUrlOverride, overrideAuthorPageLink);
    }

    @Test
    public void testGenerateEditAssetLinkDefault() {
        String touchAssetUrlRel = "/assetdetails.html/content/dam/testAsset.jpg";
        String touchAssetUrlAbs = "http://localhost:4502/assetdetails.html/content/dam/testAsset.jpg";
        when(externalizer.authorLink(null, touchAssetUrlRel)).thenReturn(touchAssetUrlAbs);

        authorUIHelper.activate(osgiConfig);
        String relAuthorAssetLink = authorUIHelper.generateEditAssetLink(assetPath, false, null);
        String absAuthorAssetLink = authorUIHelper.generateEditAssetLink(assetPath, true, null);
        assertEquals(touchAssetUrlRel, relAuthorAssetLink);
        assertEquals(touchAssetUrlAbs, absAuthorAssetLink);
    }

    @Test
    public void testGenerateEditAssetLinkOverride() {
        String touchAssetUrlOverride = "/override.html/content/dam/testAsset.jpg";

        //override touch ui page editor
        osgiConfig.put("damEditorTouchURL", "/override.html");
        authorUIHelper.activate(osgiConfig);
        String overrideAuthorPageLink = authorUIHelper.generateEditAssetLink(assetPath, false, null);
        assertEquals(touchAssetUrlOverride, overrideAuthorPageLink);
    }

    @Test
    public void testGenerateEditAssetLinkClassic() {
        String classicAssetUrlRel = "/damadmin#/content/dam/testAsset.jpg";
        String classicAssetUrlAbs = "http://localhost:4502/damadmin#/content/dam/testAsset.jpg";

        when(externalizer.authorLink(null, classicAssetUrlRel)).thenReturn(classicAssetUrlAbs);
        osgiConfig.put("isTouch", "false");
        authorUIHelper.activate(osgiConfig);

        String relAuthorPageLink = authorUIHelper.generateEditAssetLink(assetPath, false, null);
        String absAuthorPageLink = authorUIHelper.generateEditAssetLink(assetPath, true, null);
        assertEquals(classicAssetUrlRel, relAuthorPageLink);
        assertEquals(classicAssetUrlAbs, absAuthorPageLink);
    }

    @Test
    public void testGenerateAssetPageLinkOverrideClassic() {
        final String classicAssetUrlOverride = "/override.html/content/dam/testAsset.jpg";

        //override classic ui asset editor
        osgiConfig.put("isTouch", "false");
        osgiConfig.put("damEditorClassicURL", "/override.html");

        authorUIHelper.activate(osgiConfig);
        String overrideAuthorPageLink = authorUIHelper.generateEditAssetLink(assetPath, false, null);
        assertEquals(classicAssetUrlOverride, overrideAuthorPageLink);
    }

    @After
    public final void tearDown() {
        Mockito.reset();
    }
}
