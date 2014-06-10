package com.adobe.acs.commons.legacyurls.impl;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.jcr.Session;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DamUtil.class)
public class LegacyURLHandlerImplTest {

    @Mock
    QueryBuilder queryBuilder;

    @Mock
    Query query;

    @Mock
    SearchResult searchResult;

    @Mock
    Hit hit;

    List<Hit> hits = new ArrayList<Hit>();

    @Mock
    SlingHttpServletRequest request;

    @Mock
    SlingHttpServletResponse response;

    @Mock
    ResourceResolver resourceResolver;

    @Mock
    PageManager pageManager;

    @Mock
    Session session;

    @InjectMocks
    LegacyURLHandlerImpl legacyURLHandler;

    @Before
    public void setUp() throws Exception {
        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(pageManager);

        when(query.getResult()).thenReturn(searchResult);
        when(searchResult.getHits()).thenReturn(hits);
    }

    @Test
    public void testDoRedirect_Page() throws Exception {
        final String requestURI = "/some/old/url.htm";
        final String pagePath = "/content/redirect/to";

        Map<String, String> activateParams = new HashMap<String, String>();
        activateParams.put("property-name", "legacyURLs");
        activateParams.put("page-extension", ".html");
        activateParams.put("redirect-status-code", "301");

        when(resourceResolver.map(pagePath + ".html")).thenReturn(pagePath + ".html");
        when(request.getRequestURI()).thenReturn(requestURI);

        Resource resource = mock(Resource.class);
        Page page = mock(Page.class);

        hits.add(hit);

        when(hit.getResource()).thenReturn(resource);
        when(pageManager.getContainingPage(resource)).thenReturn(page);
        when(page.getPath()).thenReturn(pagePath);
        when(queryBuilder.createQuery(any(PredicateGroup.class), eq(session))).thenReturn(query);

        legacyURLHandler.activate(activateParams);

        final boolean result = legacyURLHandler.doRedirect(request, response);

        verify(response, times(1)).setStatus(301);
        verify(response, times(1)).sendRedirect("/content/redirect/to.html");
        verifyNoMoreInteractions(response);

        assertTrue(result);
    }

    @Test
    public void testDoRedirect_Asset() throws Exception {
        PowerMockito.mockStatic(DamUtil.class);

        final String requestURI = "/some/old/image.png";
        final String assetPath = "/content/dam/a/new/picture.jpg";

        Map<String, String> activateParams = new HashMap<String, String>();
        activateParams.put("property-name", "legacyURLs");
        activateParams.put("page-extension", ".html");
        activateParams.put("redirect-status-code", "301");

        when(request.getRequestURI()).thenReturn(requestURI);
        when(resourceResolver.map(assetPath)).thenReturn(assetPath);

        Resource resource = mock(Resource.class);
        Asset asset = mock(Asset.class);

        hits.add(hit);

        when(hit.getResource()).thenReturn(resource);
        when(DamUtil.isAsset(resource)).thenReturn(true);
        when(DamUtil.resolveToAsset(resource)).thenReturn(asset);
        when(asset.getPath()).thenReturn(assetPath);

        when(queryBuilder.createQuery(any(PredicateGroup.class), eq(session))).thenReturn(query);

        legacyURLHandler.activate(activateParams);

        final boolean result = legacyURLHandler.doRedirect(request, response);

        verify(response, times(1)).setStatus(301);
        verify(response, times(1)).sendRedirect(assetPath);
        verifyNoMoreInteractions(response);

        assertTrue(result);
    }

    @Test
    public void testDoRedirect_Resource() throws Exception {
        final String requestURI = "/some/old/url.htm";
        final String resourcePath = "/resource/to/redirect/to";


        when(resourceResolver.map(resourcePath)).thenReturn(resourcePath);
        when(request.getRequestURI()).thenReturn(requestURI);

        Resource resource = mock(Resource.class);

        hits.add(hit);

        when(hit.getResource()).thenReturn(resource);
        when(resource.getPath()).thenReturn(resourcePath);
        when(queryBuilder.createQuery(any(PredicateGroup.class), eq(session))).thenReturn(query);

        final boolean result = legacyURLHandler.doRedirect(request, response);

        verify(response, times(1)).setStatus(301);
        verify(response, times(1)).sendRedirect(resourcePath);
        verifyNoMoreInteractions(response);

        assertTrue(result);
    }

}