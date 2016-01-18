/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.wcm.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.day.cq.dam.api.Asset;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.osgi.PropertiesUtil;

import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
import com.day.cq.wcm.api.PageManager;

@Component(metatype = true, label = "ACS AEM Commons - Site Map Servlet", description = "Site Map Servlet",
        configurationFactory = true)
@Service
@SuppressWarnings("serial")
@Properties({ @Property(name = "sling.servlet.resourceTypes", unbounded = PropertyUnbounded.ARRAY,
        label = "Sling Resource Type", description = "Sling Resource Type for the Home Page component or components."),
        @Property(name = "sling.servlet.selectors", value = "sitemap", propertyPrivate = true),
        @Property(name = "sling.servlet.extensions", value = "xml", propertyPrivate = true),
        @Property(name = "sling.servlet.methods", value = "GET", propertyPrivate = true),
        @Property(
                name = "webconsole.configurationFactory.nameHint",
                value = "Site Map for: {externalizer.domain}, on resource types: [{sling.servlet.resourceTypes}]")
})
public final class SiteMapServlet extends SlingSafeMethodsServlet {
    
    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");

    private static final boolean DEFAULT_INCLUDE_LAST_MODIFIED = false;

    private static final String DEFAULT_EXTERNALIZER_DOMAIN = "publish";

    @Property(value = DEFAULT_EXTERNALIZER_DOMAIN, label = "Externalizer Domain",
            description = "Must correspond to a configuration of the Externalizer component.")
    private static final String PROP_EXTERNALIZER_DOMAIN = "externalizer.domain";

    @Property(boolValue = DEFAULT_INCLUDE_LAST_MODIFIED, label = "Include Last Modified",
            description = "If true, the last modified value will be included in the sitemap.")
    private static final String PROP_INCLUDE_LAST_MODIFIED = "include.lastmod";

    @Property(label = "Change Frequency Properties", unbounded = PropertyUnbounded.ARRAY,
            description = "The set of JCR property names which will contain the change frequency value.")
    private static final String PROP_CHANGE_FREQUENCY_PROPERTIES = "changefreq.properties";

    @Property(label = "Priority Properties", unbounded = PropertyUnbounded.ARRAY,
            description = "The set of JCR property names which will contain the priority value.")
    private static final String PROP_PRIORITY_PROPERTIES = "priority.properties";

    @Property(label = "DAM Folder Property",
            description = "The JCR property name which will contain DAM folders to include in the sitemap.")
    private static final String PROP_DAM_ASSETS_PROPERTY = "damassets.property";

    @Property(label = "DAM Asset MIME Types", unbounded = PropertyUnbounded.ARRAY,
            description = "MIME types allowed for DAM assets.")
    private static final String PROP_DAM_ASSETS_TYPES = "damassets.types";

    private static final String NS = "http://www.sitemaps.org/schemas/sitemap/0.9";

    @Reference
    private Externalizer externalizer;

    private String externalizerDomain;

    private boolean includeLastModified;

    private String[] changefreqProperties;

    private String[] priorityProperties;

    private String damAssetProperty;

    private List<String> damAssetTypes;

    @Activate
    protected void activate(Map<String, Object> properties) {
        this.externalizerDomain = PropertiesUtil.toString(properties.get(PROP_EXTERNALIZER_DOMAIN),
                DEFAULT_EXTERNALIZER_DOMAIN);
        this.includeLastModified = PropertiesUtil.toBoolean(properties.get(PROP_INCLUDE_LAST_MODIFIED), DEFAULT_INCLUDE_LAST_MODIFIED);
        this.changefreqProperties = PropertiesUtil.toStringArray(properties.get(PROP_CHANGE_FREQUENCY_PROPERTIES), new String[0]);
        this.priorityProperties = PropertiesUtil.toStringArray(properties.get(PROP_PRIORITY_PROPERTIES), new String[0]);
        this.damAssetProperty = PropertiesUtil.toString(properties.get(PROP_DAM_ASSETS_PROPERTY), "");
        this.damAssetTypes = Arrays.asList(PropertiesUtil.toStringArray(properties.get(PROP_DAM_ASSETS_TYPES), new String[0]));
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType(request.getResponseContentType());
        ResourceResolver resourceResolver = request.getResourceResolver();
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(request.getResource());

        XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
        try {
            XMLStreamWriter stream = outputFactory.createXMLStreamWriter(response.getWriter());
            stream.writeStartDocument("1.0");

            stream.writeStartElement("", "urlset", NS);
            stream.writeNamespace("", NS);

            // first do the current page
            write(page, stream, resourceResolver);

            for (Iterator<Page> children = page.listChildren(new PageFilter(), true); children.hasNext();) {
                write(children.next(), stream, resourceResolver);
            }

            if (damAssetTypes.size() > 0 && damAssetProperty.length() > 0) {
                for (Resource assetFolder : getAssetFolders(page, resourceResolver)) {
                    writeAssets(stream, assetFolder, resourceResolver);
                }
            }

            stream.writeEndElement();

            stream.writeEndDocument();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    private Collection<Resource> getAssetFolders(Page page, ResourceResolver resolver) {
        List<Resource> allAssetFolders = new ArrayList<Resource>();
        ValueMap properties = page.getProperties();
        String[] configuredAssetFolderPaths = properties.get(damAssetProperty, String[].class);
        if (configuredAssetFolderPaths != null) {
            // Sort to aid in removal of duplicate paths.
            Arrays.sort(configuredAssetFolderPaths);
            String prevPath = "#";
            for (String configuredAssetFolderPath : configuredAssetFolderPaths) {
                if (StringUtils.isNotBlank(configuredAssetFolderPath)) {
                    // Ensure that this folder is not a child folder of another
                    // configured folder, since it will already be included when
                    // the parent folder is traversed.
                    if (!configuredAssetFolderPath.equals(prevPath) && !StringUtils.startsWith(configuredAssetFolderPath, prevPath + "/")) {
                        Resource assetFolder = resolver.getResource(configuredAssetFolderPath);
                        if (assetFolder != null && assetFolder.isResourceType("sling:OrderedFolder")) {
                            prevPath = configuredAssetFolderPath;
                            allAssetFolders.add(assetFolder);
                        }
                    }
                }
            }
        }
        return allAssetFolders;
    }

    private void write(Page page, XMLStreamWriter stream, ResourceResolver resolver) throws XMLStreamException {
        stream.writeStartElement(NS, "url");


        String loc = externalizer.externalLink(resolver, externalizerDomain,
                String.format("%s.html", page.getPath()));
        writeElement(stream, "loc", loc);

        if (includeLastModified) {
            Calendar cal = page.getLastModified();
            if (cal != null) {
                writeElement(stream, "lastmod", DATE_FORMAT.format(cal));
            }
        }

        final ValueMap properties = page.getProperties();
        writeFirstPropertyValue(stream, "changefreq", changefreqProperties, properties);
        writeFirstPropertyValue(stream, "priority", priorityProperties, properties);

        stream.writeEndElement();
    }

    private void writeAsset(Asset asset, XMLStreamWriter stream, ResourceResolver resolver) throws XMLStreamException {
        stream.writeStartElement(NS, "url");


        String loc = externalizer.externalLink(resolver, externalizerDomain, asset.getPath());
        writeElement(stream, "loc", loc);

        if (includeLastModified) {
            long lastModified = asset.getLastModified();
            if (lastModified > 0) {
                writeElement(stream, "lastmod", DATE_FORMAT.format(lastModified));
            }
        }

        Resource contentResource = asset.adaptTo(Resource.class).getChild("jcr:content");
        if (contentResource != null) {
            final ValueMap properties = contentResource.getValueMap();
            writeFirstPropertyValue(stream, "changefreq", changefreqProperties, properties);
            writeFirstPropertyValue(stream, "priority", priorityProperties, properties);
        }

        stream.writeEndElement();
    }

    private void writeAssets(final XMLStreamWriter stream, final Resource assetFolder, final ResourceResolver resolver)
            throws XMLStreamException {
        for (Iterator<Resource> children = assetFolder.listChildren(); children.hasNext();) {
            Resource assetFolderChild = children.next();
            if (assetFolderChild.isResourceType("dam:Asset")) {
                Asset asset = assetFolderChild.adaptTo(Asset.class);

                if (damAssetTypes.contains(asset.getMimeType())) {
                    writeAsset(asset, stream, resolver);
                }
            } else if (assetFolderChild.isResourceType("sling:OrderedFolder")) {
                writeAssets(stream, assetFolderChild, resolver);
            }
        }
    }

    private void writeFirstPropertyValue(final XMLStreamWriter stream, final String elementName, final String[] propertyNames,
            final ValueMap properties) throws XMLStreamException {
        for (String prop : propertyNames) {
            String value = properties.get(prop, String.class);
            if (value != null) {
                writeElement(stream, elementName, value);
                break;
            }
        }
    }

    private void writeElement(final XMLStreamWriter stream, final String elementName, final String text) throws XMLStreamException {
        stream.writeStartElement(NS, elementName);
        stream.writeCharacters(text);
        stream.writeEndElement();
    }

}
