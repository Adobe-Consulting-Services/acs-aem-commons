package com.adobe.acs.commons.synth;

import com.adobe.acs.commons.synth.impl.SynthesizedResource;
import com.adobe.acs.commons.synth.impl.support.SyntheticRequestPathInfo;
import com.day.cq.commons.PathInfo;
import com.day.cq.wcm.api.WCMMode;
import org.apache.commons.collections4.EnumerationUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.tika.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SyntheticSlingRequestBuilderFactoryTest {

    private static final String DUMMY_PATH = "/some/path";
    private static final String RT_DUMMY = "acs-commons/components/some-component";

    private static final String DUMMY_QUERYSTRING = "someQueryParam=value&someQueryParam=bla&moreQueryParam=true";
    private static final Map<String,String[]> DUMMY_PARAM_MAP = new HashMap<>();
    private static final Map<String, Object> DUMMY_ATTRIBUTES = new HashMap<>();
    private static final Map<String, String> DUMMY_HEADERS = new HashMap<>();

    private static final String DUMMY_PATH_INFO_STRING = DUMMY_PATH + ".someselector.anotherselector.json/some/suffix/path";
    private static final RequestPathInfo DUMMY_PATH_INFO = new PathInfo(DUMMY_PATH_INFO_STRING);

    @Mock
    private ResourceResolver resourceResolver;

    private Resource resource;
    private SyntheticRequestPathInfo pathInfo;

    @InjectMocks
    private SyntheticSlingRequestBuilderFactory factory;

    private SyntheticSlingRequestBuilder systemUnderTest;

    static {
        DUMMY_ATTRIBUTES.put("Dummy1", "Value");
        DUMMY_ATTRIBUTES.put("Dummy2", "Value");
        DUMMY_ATTRIBUTES.put("Dummy3", "Value");

        DUMMY_HEADERS.put("Dummy4", "Value");
        DUMMY_HEADERS.put("Dummy5", "Value");
        DUMMY_HEADERS.put("Dummy6", "Value");

        DUMMY_PARAM_MAP.put("someQueryParam", new String[]{"value", "bla"});
        DUMMY_PARAM_MAP.put("moreQueryParam", new String[]{"true"});
    }


    @Before
    public void setUp() throws Exception {
        resource = new SynthesizedResource(resourceResolver, DUMMY_PATH, RT_DUMMY, Collections.emptyMap());
        pathInfo = new SyntheticRequestPathInfo(resourceResolver);
        systemUnderTest = factory.getBuilder(resourceResolver, resource);
    }

    @Test
    public void getResourceResolver() {
        SlingHttpServletRequest synthetic = systemUnderTest.build();
        assertSame(resourceResolver, synthetic.getResourceResolver());
    }

    @Test
    public void test_attributes() {
        SlingHttpServletRequest synthetic = systemUnderTest.withAttributes(DUMMY_ATTRIBUTES).build();
        DUMMY_ATTRIBUTES.forEach((key, value) -> {
            Object found = synthetic.getAttribute(key);
            assertEquals(value, found);
        });
        assertTrue(EnumerationUtils.toList(synthetic.getAttributeNames()).size() >= DUMMY_ATTRIBUTES.size());
    }

    @Test
    public void test_cookies(){
        Cookie cookie = new Cookie("someName", "someValue");
        SlingHttpServletRequest synthetic = systemUnderTest.withCookie(cookie).build();

        assertSame(cookie,synthetic.getCookie("someName"));
    }

    @Test
    public void test_queryParameter(){
        SlingHttpServletRequest synthetic = systemUnderTest.withQueryString(DUMMY_QUERYSTRING).build();


        assertEquals(DUMMY_QUERYSTRING, synthetic.getQueryString());
        Map<String,String[]> params = synthetic.getParameterMap();

        String[] someQueryParam = params.get("someQueryParam");
        ArrayUtils.isEquals(new String[]{"value", "bla"},someQueryParam);
        String[] moreQueryParam = params.get("moreQueryParam");
        assertEquals("true", moreQueryParam[0]);

    }

    @Test
    public void test_requestParameter(){

        SlingHttpServletRequest synthetic = systemUnderTest.withParameters(DUMMY_PARAM_MAP).build();

        RequestParameter someParam = synthetic.getRequestParameters("moreQueryParam")[0];
        assertEquals("moreQueryParam", someParam.getName());
        assertEquals("true", someParam.getString());

        RequestParameter[] params =  synthetic.getRequestParameters("someQueryParam");

        for(RequestParameter param: params){
            assertTrue(param.getString().equals("value") || param.getString().equals("bla"));
        }

        assertEquals(3, synthetic.getRequestParameterList().size());
    }

    @Test
    public void test_paramap(){

        SlingHttpServletRequest synthetic = systemUnderTest.withParameters(DUMMY_PARAM_MAP).build();
        assertEquals(DUMMY_QUERYSTRING, synthetic.getQueryString());
    }

    @Test
    public void test_pathinfo_manual(){
        final String selectorString = "someselector.anotherselector";
        final String suffixString = "/some/suffix/path";
        SlingHttpServletRequest synthetic = systemUnderTest
                .withExtension("json")
                .withSelectorString(selectorString)
                .withSuffix(suffixString)
                .build();

        Resource mockedSuffixResource = mock(Resource.class);
        when(resourceResolver.getResource(suffixString)).thenReturn(mockedSuffixResource);

        final RequestPathInfo requestPathInfo = synthetic.getRequestPathInfo();
        assertEquals("json", requestPathInfo.getExtension());
        assertEquals(selectorString, requestPathInfo.getSelectorString());
        assertEquals(DUMMY_PATH, requestPathInfo.getResourcePath());
        assertEquals(suffixString, requestPathInfo.getSuffix());
        assertEquals(mockedSuffixResource, synthetic.getRequestPathInfo().getSuffixResource());
        final String urlPath = DUMMY_PATH + "." + selectorString + ".json" + suffixString;
        assertEquals(urlPath, synthetic.getPathInfo() );
        assertEquals(urlPath, synthetic.getRequestURI());

    }

    @Test
    public void test_pathinfo_provided(){
        RequestPathInfo provided = new PathInfo(DUMMY_PATH_INFO_STRING);
        SlingHttpServletRequest synthetic = systemUnderTest
                .withPathInfoCopied(DUMMY_PATH_INFO)
                .build();

        final RequestPathInfo requestPathInfo = synthetic.getRequestPathInfo();
        assertEquals("json", requestPathInfo.getExtension());
        assertEquals("someselector.anotherselector", requestPathInfo.getSelectorString());
        assertEquals(DUMMY_PATH, requestPathInfo.getResourcePath());
        assertEquals(DUMMY_PATH_INFO_STRING, synthetic.getPathInfo() );
        assertEquals(DUMMY_PATH_INFO_STRING, synthetic.getRequestURI());
    }

    @Test
    public void test_post(){
        SlingHttpServletRequest synthetic = systemUnderTest
                .withMethod(SyntheticSlingRequestBuilder.Method.POST)
                .build();

        assertEquals("POST", synthetic.getMethod());
    }



    @Test
    public void test_wcm_edit(){
        SlingHttpServletRequest synthetic = systemUnderTest
                .withWCMMode(WCMMode.EDIT)
                .build();

        WCMMode actual = WCMMode.fromRequest(synthetic);
        assertEquals(actual, WCMMode.EDIT);
    }


    @Test
    public void test_locale(){
        SlingHttpServletRequest synthetic = systemUnderTest
                .withLocale(Locale.JAPANESE)
                .build();

        Locale actual = synthetic.getLocale();
        assertEquals(actual, Locale.JAPANESE);
    }

    //@Test
    public void test_with_payload() throws IOException {

        InputStream dummyPayload = getClass().getClassLoader().getResourceAsStream("dummyfile.json");
        String contentType = "application/json\";charset=\"utf-8";
        byte[] bytes = IOUtils.toByteArray(dummyPayload);

        SlingHttpServletRequest synthetic = systemUnderTest
                .withPayload(bytes, contentType)
                .build();

        assertEquals("application/json", synthetic.getContentType());
        assertEquals("utf-8", synthetic.getCharacterEncoding());
        IOUtils.contentEquals(dummyPayload, synthetic.getInputStream());

    }

    @Test
    public void test_with_headers(){

        DUMMY_HEADERS.forEach(systemUnderTest::withHeader);

        SlingHttpServletRequest synthetic = systemUnderTest.build();

        DUMMY_HEADERS.forEach((key, value) -> {
            String found = synthetic.getHeader(key);
            assertEquals(value, found);
        });
        assertTrue(EnumerationUtils.toList(synthetic.getHeaderNames()).size() >= DUMMY_ATTRIBUTES.size());
    }

    @Test
    public void test_with_serverinfo(){

        SlingHttpServletRequest synthetic = systemUnderTest
                .withScheme("https")
                .withServerName("adobe.com")
                .withServerPort(444)
                .withRemoteAddress("127.0.0.1")
                .withRemoteUser("userUser")
                .withRemotePort(412)
                .withPathInfoCopied(DUMMY_PATH_INFO)
                .build();

        assertEquals("https",synthetic.getScheme());
        assertEquals("adobe.com", synthetic.getServerName());
        assertEquals(444, synthetic.getServerPort());

        assertEquals("127.0.0.1",synthetic.getRemoteAddr());
        assertEquals("userUser", synthetic.getRemoteUser());
        assertEquals(412, synthetic.getRemotePort());
        assertEquals("https://adobe.com:444" + DUMMY_PATH_INFO_STRING, synthetic.getRequestURL().toString());

    }


    @Test(expected = UnsupportedOperationException.class)
    public void test_getContentLengthLong(){
        systemUnderTest.build().getContentLengthLong();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_changeSessionId(){
        systemUnderTest.build().changeSessionId();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_upgrade() throws IOException, ServletException {
        systemUnderTest.build().upgrade(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getDispatcherType(){
        systemUnderTest.build().getDispatcherType();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getAsyncContext(){
        systemUnderTest.build().getAsyncContext();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_isAsyncSupported(){
        systemUnderTest.build().isAsyncSupported();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_isAsyncStarted(){
        systemUnderTest.build().isAsyncStarted();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_startAsync(){
        systemUnderTest.build().startAsync(null,null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_startAsyncNoArg(){
        systemUnderTest.build().startAsync();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getServletContext(){
        systemUnderTest.build().getServletContext();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getPart() throws IOException, ServletException {
        systemUnderTest.build().getPart("");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getParts() throws IOException, ServletException {
        systemUnderTest.build().getParts();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_logout() throws ServletException {
        systemUnderTest.build().logout();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_login() throws ServletException {
        systemUnderTest.build().login("","");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_authenticate() throws IOException, ServletException {
        systemUnderTest.build().authenticate(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getRealPath(){
        systemUnderTest.build().getRealPath("");
    }

}