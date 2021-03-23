package com.adobe.acs.commons.redirects.ui;

public class CaConfig {
    String path;
    String name;

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    CaConfig(String path, String name){
        this.path = path;
        this.name = name;
    }
}
