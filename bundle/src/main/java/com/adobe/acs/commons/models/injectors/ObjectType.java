package com.adobe.acs.commons.models.injectors;

/**
 * Enumeration which encapsulated the available objects.
 */
enum ObjectType {

    RESOURCE("resource"),
    RECOURCE_RESOLVER("resourceResolver"),
    COMPONENT_CONTEXT("componentContext"),
    PAGE_MANAGER("pageManager"),
    CURRENT_PAGE("currentPage"),
    RESOURCE_PAGE("resourcePage"),
    DESIGNER("designer"),
    CURRENT_DESIGN("currentDesign"),
    RESOURCE_DESIGN("resourceDesign"),
    CURRENT_STYLE("currentStyle"),
    SESSION("session"),
    XSS_API("xssApi");

    private String text;

    ObjectType(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public static ObjectType fromString(String text) {
        if (text != null) {
            for (ObjectType b : ObjectType.values()) {
                if (text.equalsIgnoreCase(b.text)) {
                    return b;
                }
            }
        }
        return null;
    }
}
