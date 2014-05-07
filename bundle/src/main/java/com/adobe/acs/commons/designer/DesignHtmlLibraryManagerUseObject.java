/*
 * #%L
 * ACS AEM Tools Bundle
 * %%
 * Copyright (C) 2013 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.designer;

import java.io.IOException;
import java.io.StringWriter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;

import com.day.cq.wcm.api.designer.Design;

/**
 * Object for use in Sightly scripts to output Client Library references based on the current design.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class DesignHtmlLibraryManagerUseObject {

    private static final String MODE_ALL = "ALL";

    private static final String MODE_JS = "JS";

    private static final String MODE_CSS = "CSS";

    private SlingHttpServletRequest request;

    @Inject
    private DesignHtmlLibraryManager dhlm;

    @Inject
    @Default(values = MODE_ALL)
    private String mode;

    @Inject
    @Named("region")
    private String regionName;

    @Inject
    private Design currentDesign;

    private PageRegion region;

    /**
     * Construct a new object.
     * 
     * @param request the current request
     */
    public DesignHtmlLibraryManagerUseObject(SlingHttpServletRequest request) {
        this.request = request;
    }

    @PostConstruct
    private void init() {
        this.region = PageRegion.valueOf(regionName.toUpperCase());
    }

    /**
     * Output the CSS and JS references.
     * 
     * @return the HTML string
     * 
     * @throws IOException if the HTML can't be generated
     */
    public String include() throws IOException {
        StringWriter sw = new StringWriter();

        switch (region) {
        case HEAD:
            if (MODE_ALL.equalsIgnoreCase(mode)) {
                dhlm.writeIncludes(request, currentDesign, region, sw);
            } else if (MODE_JS.equalsIgnoreCase(mode)) {
                dhlm.writeJsInclude(request, currentDesign, region, sw);
            } else if (MODE_CSS.equalsIgnoreCase(mode)) {
                dhlm.writeCssInclude(request, currentDesign, region, sw);
            }
            break;
        case BODY:
            dhlm.writeJsInclude(request, currentDesign, region, sw);
            break;
        default:
        }

        return sw.toString();
    }
}
