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
package apps.acs_commons.components.dam.color_swatches;

import com.adobe.acs.commons.dam.ColorConversion;
import com.adobe.cq.sightly.WCMUsePojo;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.sightly.pojo.Use;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class ColorSwatches implements Use {

    private static final Logger log = LoggerFactory.getLogger(ColorSwatches.class);

    private static final Map<String, String> STANDARD_PLATE_COLORS;

    private List<Map<String, String>> colorants;

    private LinkedHashMap<String, String> plateNames;

    private List<SwatchGroup> swatchGroups;

    private ColorConversion colorConversion;

    static {
        STANDARD_PLATE_COLORS = new HashMap<String, String>();
        STANDARD_PLATE_COLORS.put("Cyan", formatRGB(0, 255, 255));
        STANDARD_PLATE_COLORS.put("Magenta", formatRGB(255, 0, 255));
        STANDARD_PLATE_COLORS.put("Yellow", formatRGB(255, 255, 0));
        STANDARD_PLATE_COLORS.put("Black", formatRGB(0, 0, 0));
    }

    private String extractLAB(ValueMap properties) {
        ColorConversion.LAB lab = new ColorConversion.LAB(
                properties.get("xmpG:L", BigDecimal.ZERO).floatValue(),
                properties.get("xmpG:A", 0),
                properties.get("xmpG:B", 0)
        );

        ColorConversion.RGB rgb = colorConversion.toRGB(lab);

        return formatRGB(rgb.red, rgb.green, rgb.blue);
    }

    private String extractCMYK(ValueMap properties) {
        ColorConversion.CMYK cmyk = new ColorConversion.CMYK(
                properties.get("xmpG:cyan", BigDecimal.ZERO).intValue(),
                properties.get("xmpG:magenta", BigDecimal.ZERO).intValue(),
                properties.get("xmpG:yellow", BigDecimal.ZERO).intValue(),
                properties.get("xmpG:black", BigDecimal.ZERO).intValue()
        );

        ColorConversion.RGB rgb = colorConversion.toRGB(cmyk);

        return formatRGB(rgb.red, rgb.green, rgb.blue);
    }

    private String extractRGB(ValueMap properties) {
        return formatRGB(properties.get("xmpG:red", 0), properties.get("xmpG:green", 0), properties.get("xmpG:blue", 0));
    }

    private static String formatRGB(int red, int green, int blue) {
        return String.format("rgb(%s,%s,%s)", red, green, blue);
    }

    private Map<String, String> createColorant(Resource res) {
        Map<String, String> result = new HashMap<String, String>();
        ValueMap properties = res.getValueMap();
        String swatchName = properties.get("xmpG:swatchName", String.class);
        result.put("name", swatchName);
        result.put("type", properties.get("xmpG:type", "process").toLowerCase());
        String colorSpace = properties.get("xmpG:mode", "unknown").toLowerCase();
        result.put("colorSpace", colorSpace);
        String color = null;
        if (colorSpace.equals("rgb")) {
            color = extractRGB(properties);
        } else if (colorSpace.equals("cmyk")) {
            color = extractCMYK(properties);
        } else if (colorSpace.equals("lab")){
            color = extractLAB(properties);
        }
        if (color != null) {
            result.put("color", color);
            if (plateNames.containsKey(swatchName)) {
                plateNames.put(swatchName, color);
            }
        }
        return result;
    }

    private SwatchGroup createSwatchGroup(Resource res) {
        SwatchGroup sg = new SwatchGroup();
        ValueMap properties = res.getValueMap();
        sg.name = properties.get("xmpG:groupName", "Unknown");
        sg.type = properties.get("xmpG:groupType", 0l);
        Resource colorantsResource = res.getChild("xmpG:Colorants");
        if (colorantsResource != null) {
            sg.colorants = new ArrayList<Map<String, String>>();
            for (Resource colorantResource : colorantsResource.getChildren()) {
                sg.colorants.add(createColorant(colorantResource));
            }
        }
        return sg;
    }

    @Override
    public void init(Bindings bindings) {
        SlingScriptHelper scriptHelper = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
        colorConversion = scriptHelper.getService(ColorConversion.class);

        SlingHttpServletRequest request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
        String itemPath = request.getParameter("item");

        if (itemPath != null) {
            ResourceResolver resourceResolver = request.getResourceResolver();
            Resource metadataResource = resourceResolver.getResource(itemPath + "/jcr:content/metadata");
            ValueMap properties = metadataResource.getValueMap();

            String[] plateNamesProperty = properties.get("xmpTPg:PlateNames", String[].class);
            if (plateNamesProperty != null) {
                plateNames = new LinkedHashMap<String, String>();
                for (String plateName : plateNamesProperty) {
                    plateNames.put(plateName, STANDARD_PLATE_COLORS.get(plateName));
                }
            }

            Resource colorantsResource = metadataResource.getChild("xmpTPg:Colorants");
            if (colorantsResource != null) {
                this.colorants = new ArrayList<Map<String, String>>();
                for (Resource colorantResource : colorantsResource.getChildren()) {
                    this.colorants.add(createColorant(colorantResource));
                }
            } else {
                Resource swatchGroupsResource = metadataResource.getChild("xmpTPg:SwatchGroups");
                if (swatchGroupsResource != null) {
                    this.swatchGroups = new ArrayList<SwatchGroup>();
                    for (Resource swatchGroupResource : swatchGroupsResource.getChildren()) {
                        this.swatchGroups.add(createSwatchGroup(swatchGroupResource));
                    }
                }
            }
        }

    }

    public List<Map<String, String>> getColorants() {
        return colorants;
    }

    public List<SwatchGroup> getSwatchGroups() {
        return swatchGroups;
    }

    public Map<String, String> getPlateNames() {
        return plateNames;
    }

    public boolean getHasContent() {
        return (colorants != null && colorants.size() > 0) ||
               (swatchGroups != null && swatchGroups.size() > 0);
    }

    public static class SwatchGroup {
        public String name;
        public long type;
        public List<Map<String, String>> colorants;
    }


}