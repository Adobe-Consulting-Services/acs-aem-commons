package com.adobe.acs.commons.synth.impl.support;

import javax.servlet.http.Cookie;
import java.util.LinkedHashMap;
import java.util.Map;


public class CookieSupport {
    private Map<String, Cookie> cookies = new LinkedHashMap();

    public CookieSupport() {
    }

    public void addCookie(Cookie cookie) {
        this.cookies.put(cookie.getName(), cookie);
    }

    public Cookie getCookie(String name) {
        return (Cookie)this.cookies.get(name);
    }

    public Cookie[] getCookies() {
        return this.cookies.isEmpty() ? null : (Cookie[])this.cookies.values().toArray(new Cookie[this.cookies.size()]);
    }

    public void reset() {
        this.cookies.clear();
    }
}
