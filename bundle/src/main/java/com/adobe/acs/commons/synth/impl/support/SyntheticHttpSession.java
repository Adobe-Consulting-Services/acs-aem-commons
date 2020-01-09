package com.adobe.acs.commons.synth.impl.support;

import org.apache.commons.collections.IteratorUtils;
import org.osgi.annotation.versioning.ConsumerType;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**

 * @since 2018-10-09
 */
@ConsumerType
public class SyntheticHttpSession implements HttpSession {
    
    private static final int DEFAULT_MAX_ACTIVE_INTERVAL = 1800;
    
    private final ServletContext servletContext = this.newMockServletContext();
    private final Map<String, Object> attributeMap = new HashMap<>();
    private final String sessionID = UUID.randomUUID().toString();
    private final long creationTime = System.currentTimeMillis();
    private boolean invalidated;
    private boolean isNew = true;
    private int maxActiveInterval = DEFAULT_MAX_ACTIVE_INTERVAL;

    protected SyntheticServletContext newMockServletContext() {
        return new SyntheticServletContext();
    }
    
    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public Object getAttribute(String name) {
        this.checkInvalidatedState();
        return this.attributeMap.get(name);
    }
    
    @Override
    public Enumeration<String> getAttributeNames() {
        this.checkInvalidatedState();
        return IteratorUtils.asEnumeration(this.attributeMap.keySet().iterator());
    }
    
    @Override
    public String getId() {
        return this.sessionID;
    }
    
    @Override
    public long getCreationTime() {
        this.checkInvalidatedState();
        return this.creationTime;
    }
    
    @Override
    public Object getValue(String name) {
        this.checkInvalidatedState();
        return this.getAttribute(name);
    }
    
    @Override
    public String[] getValueNames() {
        this.checkInvalidatedState();
        return this.attributeMap.keySet().toArray(new String[this.attributeMap.keySet().size()]);
    }
    
    @Override
    public void putValue(String name, Object value) {
        this.checkInvalidatedState();
        this.setAttribute(name, value);
    }
    
    @Override
    public void removeAttribute(String name) {
        this.checkInvalidatedState();
        this.attributeMap.remove(name);
    }
    
    @Override
    public void removeValue(String name) {
        this.checkInvalidatedState();
        this.attributeMap.remove(name);
    }
    
    @Override
    public void setAttribute(String name, Object value) {
        this.checkInvalidatedState();
        this.attributeMap.put(name, value);
    }
    
    @Override
    public void invalidate() {
        this.checkInvalidatedState();
        this.invalidated = true;
    }

    private void checkInvalidatedState() {
        if (this.invalidated) {
            throw new IllegalStateException("Session is already invalidated.");
        }
    }

    public boolean isInvalidated() {
        return this.invalidated;
    }
    
    @Override
    public boolean isNew() {
        this.checkInvalidatedState();
        return this.isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }
    
    @Override
    public long getLastAccessedTime() {
        this.checkInvalidatedState();
        return this.creationTime;
    }
    
    @Override
    public int getMaxInactiveInterval() {
        return this.maxActiveInterval;
    }
    
    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxActiveInterval = interval;
    }
    
    @Override
    public HttpSessionContext getSessionContext() {
        throw new UnsupportedOperationException();
    }
}
