package com.adobe.acs.commons.wcm.vanity.impl;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockRequestDispatcherFactory;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.RequestDispatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VanityURLServiceImplTest {

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    public MockSlingHttpServletRequest request;
    public MockSlingHttpServletResponse response;

    @Mock
    QueryBuilder queryBuilder;

    @Mock
    Query query;

    @Mock
    SearchResult searchResult;

    private List<Hit> hits;

    @Mock
    RequestDispatcher requestDispatcher;

    @InjectMocks
    VanityURLServiceImpl vanityURLService = new VanityURLServiceImpl();


    @Before
    public void setUp() throws Exception {
        request = new MockSlingHttpServletRequest(context.resourceResolver(), context.bundleContext());
        response = new MockSlingHttpServletResponse();

        when(queryBuilder.createQuery(any(PredicateGroup.class), any(Session.class))).thenReturn(query);
        when(query.getResult()).thenReturn(searchResult);
        when(searchResult.getTotalMatches()).thenReturn(1L);
        when(searchResult.hasMore()).thenReturn(false);
        hits = new ArrayList<>();
        hits.add(hit);
        when(searchResult.getHits()).thenReturn(hits);

        context.build().resource("/content")
                .resource("sample")
                .resource("vanity", "sling:vanityPath", "/my-vanity");
    }

    @Test
    public void dispatch() throws Exception {
        MockRequestDispatcherFactory requestDispatcherFactory = new MockRequestDispatcherFactory() {
            @Override
            public RequestDispatcher getRequestDispatcher(String path, RequestDispatcherOptions options) {
                return requestDispatcher;
            }

            @Override
            public RequestDispatcher getRequestDispatcher(Resource resource, RequestDispatcherOptions options) {
                return requestDispatcher;
            }
        };
        request.setRequestDispatcherFactory(requestDispatcherFactory);
        request.setServletPath("/content/sample/my-vanity.html");

        assertTrue(vanityURLService.dispatch(request, response));
        verify(requestDispatcher, times(1)).forward(any(ExtensionlessRequestWrapper.class), eq(response));
    }

    @Test
    public void dispatch_NoMapping() throws Exception {
        request.setServletPath("/my-vanity");

        assertFalse(vanityURLService.dispatch(request, response));
        verify(requestDispatcher, times(0)).forward(any(ExtensionlessRequestWrapper.class), eq(response));
    }

    @Test
    public void dispatch_Loop() throws Exception {
        request.setAttribute("acs-aem-commons__vanity-check-loop-detection", true);

        assertFalse(vanityURLService.dispatch(request, response));
        verify(requestDispatcher, times(0)).forward(any(ExtensionlessRequestWrapper.class), eq(response));
    }

    private Hit hit = new Hit() {
        @Override
        public long getIndex() {
            return 0;
        }

        @Override
        public Map<String, String> getExcerpts() throws RepositoryException {
            return null;
        }

        @Override
        public String getExcerpt() throws RepositoryException {
            return null;
        }

        @Override
        public Resource getResource() throws RepositoryException {
            return null;
        }

        @Override
        public Node getNode() throws RepositoryException {
            return null;
        }

        @Override
        public String getPath() throws RepositoryException {
            return "/content/sample/my-vanity";
        }

        @Override
        public ValueMap getProperties() throws RepositoryException {
            return null;
        }

        @Override
        public String getTitle() throws RepositoryException {
            return null;
        }

        @Override
        public double getScore() throws RepositoryException {
            return 0;
        }
    };
}