package com.adobe.acs.commons.granite.ui.components.include;

import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;

import static com.adobe.acs.commons.granite.ui.components.include.IncludeDecoratorFilterImpl.NAMESPACE;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;

@RunWith(MockitoJUnitRunner.class)
public class IncludeDecoratorFilterImplTest {

    @Mock
    FilterChain filterChain;

    @Captor
    ArgumentCaptor<SlingHttpServletRequest> argumentCaptor;

    IncludeDecoratorFilterImpl systemUnderTest;

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);


    @Before
    public void setUp() throws Exception {

        InputStream inputStream = getClass().getResourceAsStream("filter-test.json");
        context.load().json(inputStream, "/apps/tab");
        context.currentResource("/apps/tab/items/column/items/include");

        systemUnderTest = new IncludeDecoratorFilterImpl();


    }

    @Test
    public void test() throws IOException, ServletException {

        Mockito.doAnswer(invocationOnMock -> {

            SlingHttpServletRequest captured = invocationOnMock.getArgumentAt(0, SlingHttpServletRequest.class);
            assertEquals("block1", captured.getAttribute(NAMESPACE));
            return null;

        }).when(filterChain).doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));

        systemUnderTest.doFilter(context.request(), context.response(), filterChain);

        assertTrue("namespace is removed after the filter is performed", context.request().getAttribute(NAMESPACE) == null);

    }
}