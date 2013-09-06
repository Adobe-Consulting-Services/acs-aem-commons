package com.adobe.acs.commons.designer;

import com.day.cq.wcm.api.designer.Design;

public interface DesignHtmlLibraryManager {
    public static final String RESOURCE_NAME = "_clientlibs";
    public static final String REL_PATH_CSS = RESOURCE_NAME + "/css";
    public static final String REL_PATH_JS = RESOURCE_NAME + "/js";

    public static final String PROP_HEAD_LIBS = "headLibs";
    public static final String PROP_BODY_START_LIBS = "bodyStartLibs";
    public static final String PROP_BODY_END_LIBS = "bodyEndLibs";

    public String getHeadLibs(Design design);
    public String getCssHeadLibs(Design design);
    public String getJsHeadLibs(Design design);

    public String getBodyStartLibs(Design design);
    public String getCssBodyStartLibs(Design design);
    public String getJsBodyStartLibs(Design design);

    public String getBodyEndLibs(Design design);
    public String getCssBodyEndLibs(Design design);
    public String getJsBodyEndLibs(Design design);
}
