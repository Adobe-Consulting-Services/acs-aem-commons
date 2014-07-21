package com.adobe.acs.commons.cors;


import java.net.URI;
import java.net.URISyntaxException;

public class Origin {
    private String originStr;
    private String host;
    private int port;
    private String scheme;

    public Origin(String origin) {

        if (!origin.startsWith("http")) {
            origin = "http://" + origin;
        }
        this.originStr = origin;
        try {
            URI uri = new URI(origin);
            this.host = uri.getHost();
            this.port = uri.getPort();
            this.scheme = uri.getScheme();
        } catch (URISyntaxException e) {

        }
    }

    public String getOriginStr() {
        return this.originStr;
    }

    public String getHost() {
        return this.host;
    }

    public String getScheme() {
        return this.scheme;
    }

    public int getPort() {
        return this.getPort();
    }

    @Override
    public int hashCode() {
        return this.originStr.hashCode();
    }

    @Override
    public String toString() {
        return this.originStr;
    }

    @Override
    public boolean equals(Object object) {
        return object != null && object instanceof Origin && this.toString().equals(object.toString());
    }
}
