package com.adobe.acs.commons.designer;

public enum PageRegion {
    HEAD("head"),
    BODY("body");

    private String name;
    private PageRegion(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}