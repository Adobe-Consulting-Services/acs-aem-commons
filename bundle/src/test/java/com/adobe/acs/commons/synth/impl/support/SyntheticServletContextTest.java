package com.adobe.acs.commons.synth.impl.support;

import org.apache.poi.ss.formula.functions.Even;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class SyntheticServletContextTest {

    private final SyntheticServletContext systemUnderTest = new SyntheticServletContext();

    @Mock
    private Filter filter;


    @Mock
    private Servlet servlet;

    @Test
    public void test_mimetype(){
        assertEquals("application/octet-stream", systemUnderTest.getMimeType(""));
    }
    @Test(expected = UnsupportedOperationException.class)
    public void test_getAttribute(){
        systemUnderTest.getAttribute(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getAttributeNames(){
        systemUnderTest.getAttributeNames();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getContext(){
        systemUnderTest.getContext(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getContextPath(){
        systemUnderTest.getContextPath();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getInitParameter(){
        systemUnderTest.getInitParameter(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getInitParameterNames(){
        systemUnderTest.getInitParameterNames();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getMajorVersion(){
        systemUnderTest.getMajorVersion();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getMinorVersion(){
        systemUnderTest.getMinorVersion();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getNamedDispatcher(){
        systemUnderTest.getNamedDispatcher(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getRealPath(){
        systemUnderTest.getRealPath(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getRequestDispatcher(){
        systemUnderTest.getRequestDispatcher(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getResource(){
        systemUnderTest.getResource(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getResourceAsStream(){
        systemUnderTest.getResourceAsStream(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getResourcePaths(){
        systemUnderTest.getResourcePaths(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getServerInfo(){
        systemUnderTest.getServerInfo();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getServlet(){
        systemUnderTest.getServlet(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getServletContextName(){
        systemUnderTest.getServletContextName();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getServletNames(){
        systemUnderTest.getServletNames();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getServlets(){
        systemUnderTest.getServlets();
    }

    public void log(String msg) {
        throw new UnsupportedOperationException();
    }

    public void log(Exception exception, String msg) {
        throw new UnsupportedOperationException();
    }

    public void log(String msg, Throwable throwable) {
        throw new UnsupportedOperationException();
    }


    @Test(expected = UnsupportedOperationException.class)
    public void test_log1(){
        systemUnderTest.log(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_log2(){
        systemUnderTest.log("", null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_log3(){
        systemUnderTest.log(null, "");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_removeAttribute(){
        systemUnderTest.removeAttribute(null);
    }


    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        throw new UnsupportedOperationException();
    }

    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        throw new UnsupportedOperationException();
    }

    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        throw new UnsupportedOperationException();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_addServlet1(){
        systemUnderTest.addServlet("", "");
    }


    @Test(expected = UnsupportedOperationException.class)
    public void test_addServlet2(){
        systemUnderTest.addServlet("", servlet);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_addServlet3(){
        systemUnderTest.addServlet("", Servlet.class);
    }

    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        throw new UnsupportedOperationException();
    }

    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        throw new UnsupportedOperationException();
    }

    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        throw new UnsupportedOperationException();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_addFilter1(){
        systemUnderTest.addFilter("", "");
    }


    @Test(expected = UnsupportedOperationException.class)
    public void test_addFilter2(){
        systemUnderTest.addFilter("",filter);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_addFilter3(){
        systemUnderTest.addFilter("", Filter.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_setAttribute(){
        systemUnderTest.setAttribute("", null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getEffectiveMajorVersion(){
        systemUnderTest.getEffectiveMajorVersion();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getEffectiveMinorVersion(){
        systemUnderTest.getEffectiveMinorVersion();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_setInitParameter(){
        systemUnderTest.setInitParameter(null,null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_createServlet() throws ServletException {
        systemUnderTest.createServlet(Servlet.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getServletRegistration(){
        systemUnderTest.getServletRegistration("");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getServletRegistrations(){
        systemUnderTest.getServletRegistrations();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_createFilter() throws ServletException {
        systemUnderTest.createFilter(Filter.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getFilterRegistration(){
        systemUnderTest.getFilterRegistration(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getSessionCookieConfig(){
        systemUnderTest.getSessionCookieConfig();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_setSessionTrackingModes(){
        systemUnderTest.setSessionTrackingModes(Collections.emptySet());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getDefaultSessionTrackingModes(){
        systemUnderTest.getDefaultSessionTrackingModes();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getEffectiveSessionTrackingModes(){
        systemUnderTest.getEffectiveSessionTrackingModes();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_addListener(){
        systemUnderTest.addListener("");
    }


    @Test(expected = UnsupportedOperationException.class)
    public void test_addListener2(){
        systemUnderTest.addListener(new EventListener() {
        });
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_addListener3(){
        systemUnderTest.addListener(EventListener.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_createListener(){
        systemUnderTest.createListener(EventListener.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getJspConfigDescriptor(){
        systemUnderTest.getJspConfigDescriptor();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getClassLoader(){
        systemUnderTest.getClassLoader();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_declareRoles(){
        systemUnderTest.declareRoles("");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getVirtualServerName(){
        systemUnderTest.getVirtualServerName();
    }
}