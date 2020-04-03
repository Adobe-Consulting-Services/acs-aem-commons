/*
 * #%L
 * ACS AEM Commons Package
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
/*global use: false, sling: false, Packages: false, currentDesign: false, request: false */
use(function() {
    var obj = {},
        dhlmService = sling.getService(Packages.com.adobe.acs.commons.designer.DesignHtmlLibraryManager),
        HEAD = Packages.com.adobe.acs.commons.designer.PageRegion.HEAD,
        BODY = Packages.com.adobe.acs.commons.designer.PageRegion.BODY,
        MODE_ALL = "ALL",
        MODE_JS = "JS",
        MODE_CSS = "CSS",
        region = this.region.toUpperCase(),
        mode = this.mode ? this.mode.toUpperCase() : MODE_ALL;

    obj.include = function() {
        var sw = new Packages.java.io.StringWriter();

        switch (region) {
        case 'HEAD':
            if (MODE_ALL.equals(mode)) {
                dhlmService.writeIncludes(request, currentDesign, HEAD, sw);
            } else if (MODE_JS.equals(mode)) {
                dhlmService.writeJsInclude(request, currentDesign, HEAD, sw);
            } else if (MODE_CSS.equals(mode)) {
                dhlmService.writeCssInclude(request, currentDesign, HEAD, sw);
            }
            break;
        case 'BODY':
            dhlmService.writeJsInclude(request, currentDesign, BODY, sw);
            break;
        default:
            break;
        }

        return sw.toString();
    };

    return obj;
});