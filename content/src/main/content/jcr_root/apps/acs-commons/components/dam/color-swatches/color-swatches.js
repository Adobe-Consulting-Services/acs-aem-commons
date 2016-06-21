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

/*global use: false, request: false, Packages: false, java: false */
use(function() {
    function extractPlateNames(itemPath) {
        var metadataResource = resolver.getResource(itemPath + "/jcr:content/metadata"),
            metadataProperties = metadataResource.adaptTo(Packages.org.apache.sling.api.resource.ValueMap),
            plateNamesProperty = metadataProperties.get("xmpTPg:PlateNames"),
            i;

        if (plateNamesProperty) {
            result.plateNames = {};
            for (i = 0; i < plateNamesProperty.length; i++) {
                result.plateNames[plateNamesProperty[i]] = STANDARD_PLATE_COLORS[plateNamesProperty[i]];
            }
        }
    }

    function parseColorant(res) {
        var colorant = {},
            properties = res.adaptTo(Packages.org.apache.sling.api.resource.ValueMap),
            swatchName = properties["xmpG:swatchName"],
            colorSpace = "" +properties.get("xmpG:mode", "unknown").toLowerCase(); // ensure a JS string

        colorant.name = swatchName;
        colorant.type = properties.get("xmpG:type", "process").toLowerCase();
        colorant.colorSpace = colorSpace;

        switch (colorSpace) {
            case 'rgb':
                colorant.color = extractRGB(properties);
                break;
            case 'cmyk':
                colorant.color = extractCMYK(properties);
                break;
            case 'lab':
                colorant.color = extractLAB(properties);
                break;
        }

        if (result.plateNames && colorant.color && Object.keys(result.plateNames).indexOf(swatchName) >= 0) {
            result.plateNames[swatchName] = colorant.color;
        }
        return colorant;
    }

    function parseSwatchGroup(res) {
        var swatchGroup = {},
            properties = res.adaptTo(Packages.org.apache.sling.api.resource.ValueMap),
            colorantsResource,
            childrenIterator;

        swatchGroup.name = properties.get("xmpG:groupName", "Unknown");
        swatchGroup.type = properties.get("xmpG:groupType", new java.lang.Long(0));
        swatchGroup.colorants = [];

        colorantsResource = res.getChild("xmpG:Colorants");
        if (colorantsResource) {
            childrenIterator = resolver.listChildren(colorantsResource);
            while (childrenIterator.hasNext()) {
                swatchGroup.colorants.push(parseColorant(childrenIterator.next()));
            }
        }

        return swatchGroup;
    }

    function extractLAB(properties) {
        var lab = new Packages.com.adobe.acs.commons.dam.ColorConversion.LAB(
                properties.get("xmpG:L", java.math.BigDecimal.ZERO).floatValue(),
                properties.get("xmpG:A", 0),
                properties.get("xmpG:B", 0)
            ),
            rgb = colorConversion.toRGB(lab);

        return formatRGB(rgb.red, rgb.green, rgb.blue);
    }

    function extractCMYK(properties) {
        var cmyk = new Packages.com.adobe.acs.commons.dam.ColorConversion.CMYK(
                properties.get("xmpG:cyan", java.math.BigDecimal.ZERO).intValue(),
                properties.get("xmpG:magenta", java.math.BigDecimal.ZERO).intValue(),
                properties.get("xmpG:yellow", java.math.BigDecimal.ZERO).intValue(),
                properties.get("xmpG:black", java.math.BigDecimal.ZERO).intValue()
            ),
            rgb = colorConversion.toRGB(cmyk);

        return formatRGB(rgb.red, rgb.green, rgb.blue);
    }

    function extractRGB(properties) {
        return formatRGB(properties.get("xmpG:red", 0), properties.get("xmpG:green", 0), properties.get("xmpG:blue", 0));
    }

    function formatRGB(red, green, blue) {
        return "rgb(" + Math.round(red) + "," + Math.round(green) + "," + Math.round(blue) + ")";
    }

    var result = {},
        itemPath = request.getParameter("item"),
        STANDARD_PLATE_COLORS = {
            Cyan : formatRGB(0, 255, 255),
            Magenta : formatRGB(255, 0, 255),
            Yellow : formatRGB(255, 255, 0),
            Black : formatRGB(255, 255, 255)
        },
        colorConversion = sling.getService(Packages.com.adobe.acs.commons.dam.ColorConversion),
        colorantsResource,
        swatchGroupsResource,
        childrenIterator;

    if (itemPath) {
        extractPlateNames(itemPath);

        colorantsResource = resolver.getResource(itemPath + "/jcr:content/metadata/xmpTPg:Colorants");
        if (colorantsResource) {
            result.colorants = [];
            childrenIterator = resolver.listChildren(colorantsResource);
            while (childrenIterator.hasNext()) {
                result.colorants.push(parseColorant(childrenIterator.next()));
            }
        } else {
            swatchGroupsResource = resolver.getResource(itemPath + "/jcr:content/metadata/xmpTPg:SwatchGroups");
            if (swatchGroupsResource) {
                result.swatchGroups = [];
                childrenIterator = resolver.listChildren(swatchGroupsResource);
                while (childrenIterator.hasNext()) {
                    result.swatchGroups.push(parseSwatchGroup(childrenIterator.next()));
                }
            }
        }

    }

    result.hasContent = (result.swatchGroups && result.swatchGroups.length > 0) ||
        (result.colorants && result.colorants.length > 0);

    return result;
});