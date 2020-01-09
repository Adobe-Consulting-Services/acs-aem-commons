package com.adobe.acs.commons.synth.impl.support;

import com.adobe.acs.commons.synth.SyntheticRequestDispatcherFactory;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.request.*;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ConsumerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;
import java.util.Map.Entry;

/**
 * SyntheticSlingHttpServletRequest - provides a synthetic sling http request
 * to be used to create internal requests without a reference to a request,
 * and also has the flexibility to set most variables / fields.
 */
@ConsumerType
public class SyntheticSlingHttpServletRequest extends SlingAdaptable implements SlingHttpServletRequest {
    
    private static final Logger LOG = LoggerFactory.getLogger(SyntheticSlingHttpServletRequest.class);
    
    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";
    private static final int HTTP_PORT = 80;
    private static final int HTTPS_PORT = 443;
    
    private static final int PARAM_VALUE_SEPARATOR_CODE = 61;
    
    private static final ResourceBundle EMPTY_RESOURCE_BUNDLE = new ListResourceBundle() {
        protected Object[][] getContents() {
            return new Object[0][0];
        }
    };
    
    private final ResourceResolver resourceResolver;
    private final RequestPathInfo requestPathInfo;
    private final CookieSupport cookieSupport;
    private final HeaderSupport headerSupport;

    private Map<String, Object> attributeMap = new HashMap<>();
    private Map<String, String[]> parameterMap = new LinkedHashMap<>();
    private HttpSession session;
    private Resource resource;
    private String authType;
    private String contextPath;
    private String queryString;
    private String scheme = HTTP_SCHEME;
    private String serverName = "localhost";
    private int serverPort = HTTP_PORT;
    private String servletPath = "";
    private String pathInfo;
    private String method = "GET";
    private String contentType;
    private String characterEncoding;
    private byte[] content;
    private String remoteUser;
    private String remoteAddr;
    private String remoteHost;
    private int remotePort;
    private Locale locale;
    private boolean getInputStreamCalled;
    private boolean getReaderCalled;
    private String responseContentType;
    private Enumeration<Locale> locales;
    private SyntheticRequestDispatcherFactory requestDispatcherFactory;

    public SyntheticSlingHttpServletRequest(ResourceResolver resourceResolver, Resource resource, SyntheticRequestPathInfo requestPathInfo, CookieSupport cookieSupport, HeaderSupport headerSupport) {
        this.cookieSupport = cookieSupport;
        this.headerSupport = headerSupport;
        this.locale = Locale.US;
        this.resourceResolver = resourceResolver;
        this.resource = resource;
        this.requestPathInfo = requestPathInfo;
    }
    
    private SyntheticHttpSession newMockHttpSession() {
        return new SyntheticHttpSession();
    }
    
    private SyntheticRequestPathInfo newMockRequestPathInfo() {
        return new SyntheticRequestPathInfo(this.resourceResolver);
    }
    
    public ResourceResolver getResourceResolver() {
        return this.resourceResolver;
    }
    
    public HttpSession getSession() {
        return this.getSession(true);
    }
    
    public HttpSession getSession(boolean create) {
        if (this.session == null && create) {
            this.session = this.newMockHttpSession();
        }
        
        return this.session;
    }
    
    public RequestPathInfo getRequestPathInfo() {
        return this.requestPathInfo;
    }
    
    public Object getAttribute(String name) {
        return this.attributeMap.get(name);
    }
    
    public Enumeration getAttributeNames() {
        return IteratorUtils.asEnumeration(this.attributeMap.keySet().iterator());
    }
    
    public void removeAttribute(String name) {
        this.attributeMap.remove(name);
    }
    
    public void setAttribute(String name, Object object) {
        this.attributeMap.put(name, object);
    }
    
    public Resource getResource() {
        return this.resource;
    }
    
    public void setResource(Resource resource) {
        this.resource = resource;
    }
    
    public String getParameter(String name) {
        String[] values = this.parameterMap.get(name);
        if (values != null && values.length > 0) {
            return values[0];
        }
        return null;
    }
    
    public Map<String, String[]> getParameterMap() {
        return this.parameterMap;
    }
    
    public Enumeration<String> getParameterNames() {
        return IteratorUtils.asEnumeration(this.parameterMap.keySet().iterator());
    }
    
    public String[] getParameterValues(String name) {
        return this.parameterMap.get(name);
    }
    
    public void setParameterMap(Map<String, String[]> parameterMap) {
        this.parameterMap.clear();
        Iterator<Map.Entry<String,String[]>> var2 = parameterMap.entrySet().iterator();
        
        while (var2.hasNext()) {
            Entry<String, String[]> entry = var2.next();
            String key = entry.getKey();
            String[] value = entry.getValue();
            this.parameterMap.put(key, value);
        }
        
        this.queryString = this.formatQueryString(this.parameterMap);
    }
    
    private String formatQueryString(Map<String, String[]> map) {
        StringBuilder querystring = new StringBuilder();
        Iterator<Entry<String,String[]>> var3 = map.entrySet().iterator();
        
        while (true) {
            Entry<String,String[]> entry;
            do {
                if (!var3.hasNext()) {
                    if (querystring.length() > 0) {
                        return querystring.toString();
                    }
                    
                    return null;
                }
                
                entry = var3.next();
            } while (entry.getValue() == null);
            
            String[] var5 = entry.getValue();
            int var6 = var5.length;
            try {
                for (String value : var5) {
                    if (querystring.length() != 0) {
                        querystring.append('&');
                    }

                    querystring.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()));
                    querystring.append('=');
                    if (value != null) {
                        querystring.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
                    }
                }
            } catch (UnsupportedEncodingException e) {
                // UTF-8 encoding should exist
                throw new AssertionError(e);
            }
        }
    }
    
    public Locale getLocale() {
        return this.locale;
    }
    
    public void setLocale(Locale loc) {
        this.locale = loc;
    }
    
    public String getContextPath() {
        return this.contextPath;
    }
    
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
    
    public void setQueryString(String queryString) {
        this.queryString = queryString;
        this.parameterMap = this.parseQueryString(queryString);
    }
    
    private Map<String, String[]> parseQueryString(String query) {
        Map<String, List<String>> queryPairs = new LinkedHashMap<>();
        String[] pairs = query.split("&");
        String[] var5 = pairs;
        int var6 = pairs.length;
        try {
            for (int var7 = 0; var7 < var6; ++var7) {
                String pair = var5[var7];
                int idx = pair.indexOf(PARAM_VALUE_SEPARATOR_CODE);
                String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name()) : pair;
                if (!queryPairs.containsKey(key)) {
                    queryPairs.put(key, new ArrayList<>());
                }
                
                String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name()) : null;
                queryPairs.get(key).add(value);
            }
        } catch (UnsupportedEncodingException e) {
            // UTF-8 encoding should exist
            throw new AssertionError(e);
        }
    
        Map<String, String[]> resultMap = new LinkedHashMap<>();
        for (Entry<String, List<String>> entry : queryPairs.entrySet()) {
            resultMap.put(entry.getKey(), entry.getValue().toArray(new String[0]));
        }
        
        return resultMap;
    }
    
    public String getQueryString() {
        return this.queryString;
    }
    
    public String getScheme() {
        return this.scheme;
    }
    
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }
    
    public String getServerName() {
        return this.serverName;
    }
    
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    
    public int getServerPort() {
        return this.serverPort;
    }
    
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
    
    public boolean isSecure() {
        return StringUtils.equals(HTTPS_SCHEME, this.getScheme());
    }
    
    public String getMethod() {
        return this.method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public long getDateHeader(String name) {
        return this.headerSupport.getDateHeader(name);
    }
    
    public String getHeader(String name) {
        return this.headerSupport.getHeader(name);
    }
    
    public Enumeration<String> getHeaderNames() {
        return HeaderSupport.toEnumeration(this.headerSupport.getHeaderNames());
    }
    
    public Enumeration<String> getHeaders(String name) {
        return HeaderSupport.toEnumeration(this.headerSupport.getHeaders(name));
    }
    
    public int getIntHeader(String name) {
        return this.headerSupport.getIntHeader(name);
    }
    
    public void addHeader(String name, String value) {
        this.headerSupport.addHeader(name, value);
    }
    
    public void addIntHeader(String name, int value) {
        this.headerSupport.addIntHeader(name, value);
    }
    
    public void addDateHeader(String name, long date) {
        this.headerSupport.addDateHeader(name, date);
    }
    
    public void setHeader(String name, String value) {
        this.headerSupport.setHeader(name, value);
    }
    
    public void setIntHeader(String name, int value) {
        this.headerSupport.setIntHeader(name, value);
    }
    
    public void setDateHeader(String name, long date) {
        this.headerSupport.setDateHeader(name, date);
    }
    
    public Cookie getCookie(String name) {
        return this.cookieSupport.getCookie(name);
    }
    
    public Cookie[] getCookies() {
        return this.cookieSupport.getCookies();
    }
    
    public void addCookie(Cookie cookie) {
        this.cookieSupport.addCookie(cookie);
    }
    
    public ResourceBundle getResourceBundle(Locale locale) {
        return this.getResourceBundle(null, locale);
    }
    
    public ResourceBundle getResourceBundle(String baseName, Locale locale) {
        return EMPTY_RESOURCE_BUNDLE;
    }
    
    public RequestParameter getRequestParameter(String name) {
        String value = this.getParameter(name);
        return value != null ? new SyntheticRequestParameter(name, value) : null;
    }
    
    public RequestParameterMap getRequestParameterMap() {
        SyntheticRequestParameterMap map = new SyntheticRequestParameterMap();
        Iterator<Entry<String, String[]>> var2 = this.getParameterMap().entrySet().iterator();
        
        while (var2.hasNext()) {
            Entry<String, String[]> entry = var2.next();
            map.put(entry.getKey(), this.getRequestParameters(entry.getKey()));
        }
        
        return map;
    }
    
    public RequestParameter[] getRequestParameters(String name) {
        String[] values = this.getParameterValues(name);
        if (values == null) {
            return null;
        } else {
            RequestParameter[] requestParameters = new RequestParameter[values.length];
            
            for (int i = 0; i < values.length; ++i) {
                requestParameters[i] = new SyntheticRequestParameter(name, values[i]);
            }
            
            return requestParameters;
        }
    }
    
    public List<RequestParameter> getRequestParameterList() {
        List<RequestParameter> params = new ArrayList<>();
        Iterator<RequestParameter[]> var2 = this.getRequestParameterMap().values().iterator();
        
        while (var2.hasNext()) {
            RequestParameter[] requestParameters = var2.next();
            params.addAll(Arrays.asList(requestParameters));
        }
        
        return params;
    }
    
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }
    
    public void setCharacterEncoding(String charset) {
        this.characterEncoding = charset;
    }
    
    public String getContentType() {
        if (this.contentType != null) {
            return this.contentType + (StringUtils.isNotBlank(this.characterEncoding) ? (";charset=" + this.characterEncoding) : "");
        }
        return null;
    }
    
    public void setContentType(String type) {
        this.contentType = type;
        if (StringUtils.contains(this.contentType, ";charset=")) {
            this.characterEncoding = StringUtils.substringAfter(this.contentType, ";charset=");
            this.contentType = StringUtils.substringBefore(this.contentType, ";charset=");
        }
        
    }
    
    public ServletInputStream getInputStream() {
        if (this.getReaderCalled) {
            throw new IllegalStateException();
        } else {
            this.getInputStreamCalled = true;
            return new ServletInputStream() {
                private final InputStream is;
                
                {
                    this.is = SyntheticSlingHttpServletRequest.this.content == null ? new ByteArrayInputStream(new byte[0]) :
                            new ByteArrayInputStream(SyntheticSlingHttpServletRequest.this.content);
                }
                
                public int read() throws IOException {
                    return this.is.read();
                }
                
                public boolean isReady() {
                    return true;
                }
                
                public boolean isFinished() {
                    throw new UnsupportedOperationException();
                }
                
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
    
    public int getContentLength() {
        return this.content == null ? 0 : this.content.length;
    }
    
    public void setContent(byte[] content) {
        this.content = content;
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        if (this.requestDispatcherFactory == null) {
            throw new IllegalStateException("Please provdide a MockRequestDispatcherFactory (setRequestDispatcherFactory).");
        } else {
            return this.requestDispatcherFactory.getRequestDispatcher(path, (RequestDispatcherOptions) null);
        }
    }

    public RequestDispatcher getRequestDispatcher(String path, RequestDispatcherOptions options) {
        if (this.requestDispatcherFactory == null) {
            throw new IllegalStateException("Please provdide a MockRequestDispatcherFactory (setRequestDispatcherFactory).");
        } else {
            return this.requestDispatcherFactory.getRequestDispatcher(path, options);
        }
    }

    public RequestDispatcher getRequestDispatcher(Resource resource) {
        if (this.requestDispatcherFactory == null) {
            throw new IllegalStateException("Please provdide a MockRequestDispatcherFactory (setRequestDispatcherFactory).");
        } else {
            return this.requestDispatcherFactory.getRequestDispatcher(resource, (RequestDispatcherOptions) null);
        }
    }

    public RequestDispatcher getRequestDispatcher(Resource resource, RequestDispatcherOptions options) {
        if (this.requestDispatcherFactory == null) {
            throw new IllegalStateException("Please provdide a MockRequestDispatcherFactory (setRequestDispatcherFactory).");
        } else {
            return this.requestDispatcherFactory.getRequestDispatcher(resource, options);
        }
    }

    public void setRequestDispatcherFactory(SyntheticRequestDispatcherFactory requestDispatcherFactory) {
        this.requestDispatcherFactory = requestDispatcherFactory;
    }
    
    public String getRemoteUser() {
        return this.remoteUser;
    }
    
    public void setRemoteUser(String remoteUser) {
        this.remoteUser = remoteUser;
    }
    
    public String getRemoteAddr() {
        return this.remoteAddr;
    }
    
    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }
    
    public String getRemoteHost() {
        return this.remoteHost;
    }
    
    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }
    
    public int getRemotePort() {
        return this.remotePort;
    }
    
    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }
    
    public String getServletPath() {
        return this.servletPath;
    }
    
    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }
    
    public String getPathInfo() {
        if (this.pathInfo != null) {
            return this.pathInfo;
        }
        if (StringUtils.isEmpty(this.requestPathInfo.getResourcePath())) {
            return null;
        }
        
        StringBuilder pathInfoBuilder = new StringBuilder();
        pathInfoBuilder.append(this.requestPathInfo.getResourcePath());
        if (StringUtils.isNotEmpty(this.requestPathInfo.getSelectorString())) {
            pathInfoBuilder.append('.');
            pathInfoBuilder.append(this.requestPathInfo.getSelectorString());
        }
        
        if (StringUtils.isNotEmpty(this.requestPathInfo.getExtension())) {
            pathInfoBuilder.append('.');
            pathInfoBuilder.append(this.requestPathInfo.getExtension());
        }
        
        if (StringUtils.isNotEmpty(this.requestPathInfo.getSuffix())) {
            pathInfoBuilder.append(this.requestPathInfo.getSuffix());
        }
        
        return pathInfoBuilder.toString();
    }
    
    public void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }
    
    public String getRequestURI() {
        StringBuilder requestUri = new StringBuilder();
        if (StringUtils.isNotEmpty(this.getServletPath())) {
            requestUri.append(this.getServletPath());
        }
        
        if (StringUtils.isNotEmpty(this.getPathInfo())) {
            requestUri.append(this.getPathInfo());
        }
        
        return StringUtils.isEmpty(requestUri) ? "/" : requestUri.toString();
    }
    
    public StringBuffer getRequestURL() {
        StringBuffer requestUrl = new StringBuffer();
        requestUrl.append(this.getScheme());
        requestUrl.append("://");
        requestUrl.append(this.getServerName());
        if ((StringUtils.equals(this.getScheme(), HTTP_SCHEME) && this.getServerPort() != HTTP_PORT) ||
                (StringUtils.equals(this.getScheme(), HTTPS_SCHEME) && this.getServerPort() != HTTPS_PORT)) {
            requestUrl.append(':');
            requestUrl.append(this.getServerPort());
        }
        
        requestUrl.append(this.getRequestURI());
        return requestUrl;
    }
    
    public String getAuthType() {
        return this.authType;
    }
    
    public void setAuthType(String authType) {
        this.authType = authType;
    }
    
    public RequestProgressTracker getRequestProgressTracker() {
        throw new UnsupportedOperationException();
    }
    
    public String getResponseContentType() {
        return this.responseContentType;
    }
    
    public void setResponseContentType(String responseContentType) {
        this.responseContentType = responseContentType;
    }
    
    public Enumeration<String> getResponseContentTypes() {
        return Collections.enumeration(Collections.singleton(this.responseContentType));
    }
    
    public String getPathTranslated() {
        throw new UnsupportedOperationException();
    }
    
    public String getRequestedSessionId() {
        throw new UnsupportedOperationException();
    }
    
    public Principal getUserPrincipal() {
        throw new UnsupportedOperationException();
    }
    
    public boolean isRequestedSessionIdFromCookie() {
        throw new UnsupportedOperationException();
    }
    
    public boolean isRequestedSessionIdFromURL() {
        throw new UnsupportedOperationException();
    }
    
    public boolean isRequestedSessionIdFromUrl() {
        throw new UnsupportedOperationException();
    }
    
    public boolean isRequestedSessionIdValid() {
        throw new UnsupportedOperationException();
    }
    
    public boolean isUserInRole(String role) {
        throw new UnsupportedOperationException();
    }
    
    public String getLocalAddr() {
        throw new UnsupportedOperationException();
    }
    
    public String getLocalName() {
        throw new UnsupportedOperationException();
    }
    
    public int getLocalPort() {
        throw new UnsupportedOperationException();
    }
    
    public void setLocales(Enumeration<Locale> locales) {
        this.locales = locales;
    }
    
    public Enumeration<Locale> getLocales() {
        if (locales != null) {
            return locales;
        }
        HashSet<Locale> set = new HashSet<>();
        if (locale != null) {
            set.add(locale);
        } else {
            locale = Locale.ENGLISH;
        }
        return Collections.enumeration(set);
    }
    
    public String getProtocol() {
        throw new UnsupportedOperationException();
    }
    
    public BufferedReader getReader() {
        if (this.getInputStreamCalled) {
            throw new IllegalStateException();
        } else {
            this.getReaderCalled = true;
            if (this.content == null) {
                return new BufferedReader(new StringReader(""));
            } else {
                String contentString;
                try {
                    if (this.characterEncoding == null) {
                        contentString = new String(this.content, Charset.defaultCharset());
                    } else {
                        contentString = new String(this.content, this.characterEncoding);
                    }
                } catch (UnsupportedEncodingException e) {
                    LOG.warn("Unsupported encoding '{}', using platform default", this.characterEncoding, e);
                    contentString = new String(this.content, Charset.defaultCharset());
                }
                
                return new BufferedReader(new StringReader(contentString));
            }
        }
    }
    
    public String getRealPath(String path) {
        throw new UnsupportedOperationException();
    }
    
    public boolean authenticate(HttpServletResponse response) {
        throw new UnsupportedOperationException();
    }
    
    public void login(String pUsername, String password) {
        throw new UnsupportedOperationException();
    }
    
    public void logout() throws ServletException {
        throw new UnsupportedOperationException();
    }
    
    public Collection<Part> getParts() {
        throw new UnsupportedOperationException();
    }
    
    public Part getPart(String name) {
        throw new UnsupportedOperationException();
    }
    
    public ServletContext getServletContext() {
        throw new UnsupportedOperationException();
    }
    
    public AsyncContext startAsync() {
        throw new UnsupportedOperationException();
    }
    
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
        throw new UnsupportedOperationException();
    }
    
    public boolean isAsyncStarted() {
        throw new UnsupportedOperationException();
    }
    
    public boolean isAsyncSupported() {
        throw new UnsupportedOperationException();
    }
    
    public AsyncContext getAsyncContext() {
        throw new UnsupportedOperationException();
    }
    
    public DispatcherType getDispatcherType() {
        throw new UnsupportedOperationException();
    }
    
    public String changeSessionId() {
        throw new UnsupportedOperationException();
    }
    
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }
    
    public long getContentLengthLong() {
        throw new UnsupportedOperationException();
    }
}
