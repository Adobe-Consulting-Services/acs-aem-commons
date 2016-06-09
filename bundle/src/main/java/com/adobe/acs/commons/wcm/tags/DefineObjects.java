package com.adobe.acs.commons.wcm.tags;

import org.apache.sling.api.scripting.SlingBindings;

/**
 * Created by derek on 6/9/16.
 */
public class DefineObjects extends com.day.cq.wcm.tags.DefineObjectsTag {

    public static final String DEFAULT_DUMMY_PROPERTY_NAME = "dummyProperty";

    private String dummyProperty = "dummyProperty";

    public DefineObjects() {

    }

    public int doEndTag() {
        SlingBindings bindings = (SlingBindings)this.pageContext.getRequest().getAttribute(SlingBindings.class.getName());
        this.pageContext.setAttribute(this.dummyProperty, bindings.get(DEFAULT_DUMMY_PROPERTY_NAME));
        return super.doEndTag();
    }

    public void setDummyProperty(String dummyProperty) {
        this.dummyProperty = dummyProperty;
    }
}
