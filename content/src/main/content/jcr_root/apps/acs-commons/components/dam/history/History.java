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
package apps.acs_commons.components.dam.history;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.sightly.pojo.Use;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import java.text.Format;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class History implements Use {

    private static final Format DATE_FORMAT = FastDateFormat.getDateTimeInstance(FastDateFormat.SHORT, FastDateFormat.FULL);

    private List<Map<String, Object>> history;

    private Agent formatAgent(String agent) {
        Agent result = new Agent();
        if (agent != null) {
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
        }
        return result;
    }

    @Override
    public void init(Bindings bindings) {
        SlingHttpServletRequest request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
        String itemPath = request.getParameter("item");

        if (itemPath != null) {
            ResourceResolver resourceResolver = request.getResourceResolver();
            Resource metadataResource = resourceResolver.getResource(itemPath + "/jcr:content/metadata");

            Resource historyResource = metadataResource.getChild("xmpMM:History");
            if (historyResource != null) {
                this.history = new ArrayList<Map<String, Object>>();
                for (Resource historyEntry : historyResource.getChildren()) {
                    ValueMap properties = historyEntry.getValueMap();
                    Date when = properties.get("stEvt:when", Date.class);
                    if (when != null) {
                        Map<String, Object> displayProperties = new HashMap<String, Object>();
                        displayProperties.put("action", properties.get("stEvt:action", "saved"));
                        displayProperties.put("agent", formatAgent(properties.get("stEvt:softwareAgent", String.class)));
                        displayProperties.put("when", DATE_FORMAT.format(when));
                        if ("/metadata".equals(properties.get("stEvt:changed", String.class))) {
                            displayProperties.put("metadataOnly", "true");
                        }
                        this.history.add(displayProperties);
                    }
                }
            }
        }

    }

    public List<Map<String, Object>> getHistory() {
        return history;
    }

    public boolean getHasContent() {
        return history != null && history.size() > 0;
    }

    public class Agent {
        public String software = "unknown";
        public String platform = "other";
    }

}