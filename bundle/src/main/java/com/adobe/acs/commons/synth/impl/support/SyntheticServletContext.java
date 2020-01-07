package com.adobe.acs.commons.synth.impl.support;

import org.osgi.annotation.versioning.ConsumerType;

import javax.servlet.*;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.descriptor.JspConfigDescriptor;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

/**

 * @since 2018-10-09
 */
@ConsumerType
public class SyntheticServletContext implements ServletContext {

    public String getMimeType(String file) {
        return "application/octet-stream";
    }

    public Object getAttribute(String name) {
        throw new UnsupportedOperationException();
    }

    public Enumeration<String> getAttributeNames() {
        throw new UnsupportedOperationException();
    }

    public ServletContext getContext(String uriPath) {
        throw new UnsupportedOperationException();
    }

    public String getContextPath() {
        throw new UnsupportedOperationException();
    }

    public String getInitParameter(String name) {
        throw new UnsupportedOperationException();
    }

    public Enumeration<String> getInitParameterNames() {
        throw new UnsupportedOperationException();
    }

    public int getMajorVersion() {
        throw new UnsupportedOperationException();
    }

    public int getMinorVersion() {
        throw new UnsupportedOperationException();
    }

    public RequestDispatcher getNamedDispatcher(String name) {
        throw new UnsupportedOperationException();
    }

    public String getRealPath(String pPath) {
        throw new UnsupportedOperationException();
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        throw new UnsupportedOperationException();
    }

    public URL getResource(String pPath) {
        throw new UnsupportedOperationException();
    }

    public InputStream getResourceAsStream(String path) {
        throw new UnsupportedOperationException();
    }

    public Set<String> getResourcePaths(String path) {
        throw new UnsupportedOperationException();
    }

    public String getServerInfo() {
        throw new UnsupportedOperationException();
    }

    public Servlet getServlet(String name) {
        throw new UnsupportedOperationException();
    }

    public String getServletContextName() {
        throw new UnsupportedOperationException();
    }

    public Enumeration<String> getServletNames() {
        throw new UnsupportedOperationException();
    }

    public Enumeration<Servlet> getServlets() {
        throw new UnsupportedOperationException();
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

    public void removeAttribute(String name) {
        throw new UnsupportedOperationException();
    }

    public void setAttribute(String name, Object object) {
        throw new UnsupportedOperationException();
    }

    public int getEffectiveMajorVersion() {
        throw new UnsupportedOperationException();
    }

    public int getEffectiveMinorVersion() {
        throw new UnsupportedOperationException();
    }

    public boolean setInitParameter(String name, String value) {
        throw new UnsupportedOperationException();
    }

    public Dynamic addServlet(String servletName, String className) {
        throw new UnsupportedOperationException();
    }

    public Dynamic addServlet(String servletName, Servlet servlet) {
        throw new UnsupportedOperationException();
    }

    public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        throw new UnsupportedOperationException();
    }

    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        throw new UnsupportedOperationException();
    }

    public ServletRegistration getServletRegistration(String servletName) {
        throw new UnsupportedOperationException();
    }

    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        throw new UnsupportedOperationException();
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

    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        throw new UnsupportedOperationException();
    }

    public FilterRegistration getFilterRegistration(String filterName) {
        throw new UnsupportedOperationException();
    }

    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        throw new UnsupportedOperationException();
    }

    public SessionCookieConfig getSessionCookieConfig() {
        throw new UnsupportedOperationException();
    }

    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        throw new UnsupportedOperationException();
    }

    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        throw new UnsupportedOperationException();
    }

    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        throw new UnsupportedOperationException();
    }

    public void addListener(String pClassName) {
        throw new UnsupportedOperationException();
    }

    public <T extends EventListener> void addListener(T listener) {
        throw new UnsupportedOperationException();
    }

    public void addListener(Class<? extends EventListener> listenerClass) {
        throw new UnsupportedOperationException();
    }

    public <T extends EventListener> T createListener(Class<T> clazz) {
        throw new UnsupportedOperationException();
    }

    public JspConfigDescriptor getJspConfigDescriptor() {
        throw new UnsupportedOperationException();
    }

    public ClassLoader getClassLoader() {
        throw new UnsupportedOperationException();
    }

    public void declareRoles(String... roleNames) {
        throw new UnsupportedOperationException();
    }

    public String getVirtualServerName() {
        throw new UnsupportedOperationException();
    }
}
