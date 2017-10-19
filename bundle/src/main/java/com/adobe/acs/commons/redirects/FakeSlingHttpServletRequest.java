/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.adobe.acs.commons.redirects;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;

/**
 * Mock request object. This does not do anything useful, it just returns the
 * constructor parameter <code>secure</code> in the <code>isSecure</code>
 * method.
 */
public class FakeSlingHttpServletRequest implements SlingHttpServletRequest {

	private Resource resource;

	private String method;

	private final String scheme;
	private final String server;
	private final int port;

	private boolean secure = false;

	public static final String RESOURCE_TYPE = "foo/bar";

	private ResourceResolver resolver;

	public FakeSlingHttpServletRequest(ResourceResolver resolver, String scheme, String server, int port) {
		this.resource = new SyntheticResource(null, "", RESOURCE_TYPE);
		this.scheme = scheme;
		this.server = server;
		this.port = port;

		setMethod(null);
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public void setMethod(String method) {
		this.method = (method == null) ? "GET" : method.toUpperCase();
	}

	public Cookie getCookie(String name) {
		return null;
	}

	public RequestDispatcher getRequestDispatcher(String path, RequestDispatcherOptions options) {
		return null;
	}

	public RequestDispatcher getRequestDispatcher(Resource resource, RequestDispatcherOptions options) {
		return null;
	}

	public RequestDispatcher getRequestDispatcher(Resource resource) {
		return null;
	}

	public RequestParameter getRequestParameter(String name) {
		return null;
	}

	public RequestParameterMap getRequestParameterMap() {
		return null;
	}

	public RequestParameter[] getRequestParameters(String name) {
		return null;
	}

	public RequestPathInfo getRequestPathInfo() {
		return null;
	}

	public RequestProgressTracker getRequestProgressTracker() {
		return null;
	}

	public Resource getResource() {
		return resource;
	}

	public ResourceBundle getResourceBundle(Locale locale) {
		return null;
	}

	public ResourceBundle getResourceBundle(String baseName, Locale locale) {
		return null;
	}

	public ResourceResolver getResourceResolver() {
		return resolver;
	}

	public String getResponseContentType() {
		return null;
	}

	public Enumeration<String> getResponseContentTypes() {
		return null;
	}

	public String getAuthType() {
		return null;
	}

	public String getContextPath() {
		return "";
	}

	public Cookie[] getCookies() {
		return null;
	}

	public long getDateHeader(String name) {
		return 0;
	}

	public String getHeader(String name) {
		return null;
	}

	public Enumeration<String> getHeaderNames() {
		return null;
	}

	public Enumeration<String> getHeaders(String name) {
		return null;
	}

	public int getIntHeader(String name) {
		return 0;
	}

	public String getMethod() {
		return method;
	}

	public String getPathInfo() {
		return null;
	}

	public String getPathTranslated() {
		return null;
	}

	public String getQueryString() {
		return null;
	}

	public String getRemoteUser() {
		return null;
	}

	public String getRequestURI() {
		return null;
	}

	public StringBuffer getRequestURL() {
		return null;
	}

	public String getRequestedSessionId() {
		return null;
	}

	public String getServletPath() {
		return null;
	}

	public HttpSession getSession() {
		return null;
	}

	public HttpSession getSession(boolean create) {
		return null;
	}

	public Principal getUserPrincipal() {
		return null;
	}

	public boolean isRequestedSessionIdFromCookie() {
		return false;
	}

	public boolean isRequestedSessionIdFromURL() {
		return false;
	}

	public boolean isRequestedSessionIdFromUrl() {
		return false;
	}

	public boolean isRequestedSessionIdValid() {
		return false;
	}

	public boolean isUserInRole(String role) {
		return false;
	}

	public Object getAttribute(String name) {
		return null;
	}

	public Enumeration<String> getAttributeNames() {
		return null;
	}

	public String getCharacterEncoding() {
		return null;
	}

	public int getContentLength() {
		return 0;
	}

	public String getContentType() {
		return null;
	}

	public ServletInputStream getInputStream() {
		return null;
	}

	public String getLocalAddr() {
		return null;
	}

	public String getLocalName() {
		return null;
	}

	public int getLocalPort() {
		return 0;
	}

	public Locale getLocale() {
		return null;
	}

	public Enumeration<Locale> getLocales() {
		return null;
	}

	public String getParameter(String name) {
		return null;
	}

	public Map<String, String[]> getParameterMap() {
		return null;
	}

	public Enumeration<String> getParameterNames() {
		return null;
	}

	public String[] getParameterValues(String name) {
		return null;
	}

	public String getProtocol() {
		return null;
	}

	public BufferedReader getReader() {
		return null;
	}

	public String getRealPath(String path) {
		return null;
	}

	public String getRemoteAddr() {
		return null;
	}

	public String getRemoteHost() {
		return null;
	}

	public int getRemotePort() {
		return 0;
	}

	public RequestDispatcher getRequestDispatcher(String path) {
		return null;
	}

	public String getScheme() {
		return scheme;
	}

	public String getServerName() {
		return server;
	}

	public int getServerPort() {
		return port;
	}

	public boolean isSecure() {
		return this.secure;
	}

	public void removeAttribute(String name) {

	}

	public void setAttribute(String name, Object o) {

	}

	public void setCharacterEncoding(String env) {

	}

	public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
		return null;
	}

	@Override
	public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void login(String username, String password) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void logout() throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Part getPart(String name) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletContext getServletContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
			throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAsyncStarted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAsyncSupported() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AsyncContext getAsyncContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DispatcherType getDispatcherType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<RequestParameter> getRequestParameterList() {
		// TODO Auto-generated method stub
		return null;
	}
}
