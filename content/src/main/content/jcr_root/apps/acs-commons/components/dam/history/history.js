/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2016 Adobe
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

/*global use: false, request: false, resolver: false, Packages: false, java: false, importClass: false */
use(function() {
    function formatAgent(agent) {
        var result = {};
        if (agent.contains("Adobe Photoshop")) {
            result.software = "photoshop";
        } else if (agent.contains("Adobe Illustrator")) {
            result.software = "illustrator";
        } else if (agent.contains("Adobe InDesign")) {
            result.software = "indesign";
        } else if (agent.contains("Adobe Bridge")) {
            result.software = "bridge";
        }
        if (agent.contains("Windows")) {
            result.platform = "windows";
        } else if (agent.contains("OSX") || agent.contains("Macintosh")) {
            result.platform = "apple";
        }
        return result;
    }
    function createHistoryEntry(res) {
        var properties = res.adaptTo(Packages.org.apache.sling.api.resource.ValueMap),
            when = properties.get("stEvt:when", java.util.Date),
            entry = {};

        if (when) {
            entry.action = properties.get("stEvt:action", "saved");
            entry.agent = formatAgent(properties.get("stEvt:softwareAgent", java.lang.String));
            entry.when = dateFormat.format(when);
            if ("/metadata" === properties.get("stEvt:changed", java.lang.String)) {
                entry.metadataOnly = "true";
            }
            result.history.push(entry);
        }
    }

    var result = {
            history : []
        },
        itemPath = request.getParameter("item"),
        dateFormat,
        historyResource,
        childrenIterator,
        FastDateFormat = Packages.org.apache.commons.lang3.time.FastDateFormat;

    dateFormat = FastDateFormat.getDateTimeInstance(FastDateFormat.SHORT, FastDateFormat.FULL);

    if (itemPath) {
        historyResource = resolver.getResource(itemPath + "/jcr:content/metadata/xmpMM:History");
        if (historyResource) {
            childrenIterator = resolver.listChildren(historyResource);
            while (childrenIterator.hasNext()) {
                createHistoryEntry(childrenIterator.next());
            }
        }
    }

    result.hasContent = result.history.length > 0;

    return result;
});