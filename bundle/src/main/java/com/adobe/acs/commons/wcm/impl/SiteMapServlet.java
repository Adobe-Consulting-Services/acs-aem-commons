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

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_EXTENSIONS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_SELECTORS;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.day.cq.commons.Externalizer;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
import com.day.cq.wcm.api.PageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Servlet.class,
factory = "com.adobe.acs.commons.wcm.impl.SiteMapServlet", 
configurationPolicy = ConfigurationPolicy.REQUIRE,
property = 
{ SLING_SERVLET_RESOURCE_TYPES + "=sling/servlet/default",
  SLING_SERVLET_EXTENSIONS + "=xml", 
  SLING_SERVLET_METHODS + "=GET",
  SLING_SERVLET_SELECTORS + "=sitemap",
  "webconsole.configurationFactory.nameHint" + "=" + "Site Map for: {externalizer.domain}, on resource types: [{sling.servlet.resourceTypes}]"})
@Designate(ocd=SiteMapServlet.Config.class)
@SuppressWarnings("serial")
public final class SiteMapServlet extends SlingSafeMethodsServlet {

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");

    private static final boolean DEFAULT_INCLUDE_LAST_MODIFIED = false;

    private static final boolean DEFAULT_INCLUDE_INHERITANCE_VALUE = false;

    private static final String DEFAULT_EXTERNALIZER_DOMAIN = "publish";

    private static final boolean DEFAULT_EXTENSIONLESS_URLS = false;

    private static final boolean DEFAULT_REMOVE_TRAILING_SLASH = false;
    
    @ObjectClassDefinition(name = "ACS AEM Commons - Site Map Servlet", description = "Page and Asset Site Map Servlet")
    public @interface Config {
        String PROP_EXTERNALIZER_DOMAIN = "externalizer.domain";

        String PROP_INCLUDE_LAST_MODIFIED = "include.lastmod";

        String PROP_CHANGE_FREQUENCY_PROPERTIES = "changefreq.properties";

        String PROP_PRIORITY_PROPERTIES = "priority.properties";

        String PROP_DAM_ASSETS_PROPERTY = "damassets.property";

        String PROP_DAM_ASSETS_TYPES = "damassets.types";

        String PROP_EXCLUDE_FROM_SITEMAP_PROPERTY = "exclude.property";

        String PROP_INCLUDE_INHERITANCE_VALUE = "include.inherit";

        String PROP_EXTENSIONLESS_URLS = "extensionless.urls";

        String PROP_REMOVE_TRAILING_SLASH = "remove.slash";

        String PROP_CHARACTER_ENCODING_PROPERTY = "character.encoding";
    
        @AttributeDefinition( name = "Sling Resource Type", description = "Sling Resource Type for the Home Page component or components.")
        String[] sling_servlet_resourceType();
    
        @AttributeDefinition(defaultValue = DEFAULT_EXTERNALIZER_DOMAIN, name = "Externalizer Domain", description = "Must correspond to a configuration of the Externalizer component.")
        String externalizer_domain();

        @AttributeDefinition(defaultValue = ""+DEFAULT_INCLUDE_LAST_MODIFIED, name = "Include Last Modified", description = "If true, the last modified value will be included in the sitemap.")
        boolean include_lastmod();

        @AttributeDefinition(name = "Change Frequency Properties", description = "The set of JCR property names which will contain the change frequency value.")
        String[] changefreq_properties();

        @AttributeDefinition(name = "Priority Properties", description = "The set of JCR property names which will contain the priority value.")
        String[] priority_properties();

        @AttributeDefinition(name = "DAM Folder Property", description = "The JCR property name which will contain DAM folders to include in the sitemap.")
        String damassets_property();

        @AttributeDefinition(name = "DAM Asset MIME Types", description = "MIME types allowed for DAM assets.")
        String[] damassets_types();

        @AttributeDefinition(defaultValue = NameConstants.PN_HIDE_IN_NAV, name = "Exclude from Sitemap Property", description = "The boolean [cq:Page]/jcr:content property name which indicates if the Page should be hidden from the Sitemap. Default value: hideInNav")
        String exclude_property();

        @AttributeDefinition(defaultValue = ""+DEFAULT_INCLUDE_INHERITANCE_VALUE, name = "Include Inherit Value", description = "If true searches for the frequency and priority attribute in the current page if null looks in the parent.")
        boolean include_inherit();

        @AttributeDefinition(defaultValue = ""+DEFAULT_EXTENSIONLESS_URLS, name = "Extensionless URLs", description = "If true, page links included in sitemap are generated without .html extension and the path is included with a trailing slash, e.g. /content/geometrixx/en/.")
        boolean extensionless_urls();

        @AttributeDefinition(defaultValue = ""+DEFAULT_REMOVE_TRAILING_SLASH, name = "Remove Trailing Slash from Extensionless URLs", description = "Only relevant if Extensionless URLs is selected.  If true, the trailing slash is removed from extensionless page links, e.g. /content/geometrixx/en.")
        boolean remove_slash();

        @AttributeDefinition(name = "Character Encoding", description = "If not set, the container's default is used (ISO-8859-1 for Jetty)")
        String character_encoding();
    }

    private static final String NS = "http://www.sitemaps.org/schemas/sitemap/0.9";

    @Reference
    private Externalizer externalizer;

    private String externalizerDomain;

    private boolean includeInheritValue;

    private boolean includeLastModified;

    private String[] changefreqProperties;

    private String[] priorityProperties;

    private String damAssetProperty;

    private List<String> damAssetTypes;

    private String excludeFromSiteMapProperty;

    private String characterEncoding;

    private boolean extensionlessUrls;

    private boolean removeTrailingSlash;

    @Activate
    protected void activate(Config config) {
        this.externalizerDomain = config.externalizer_domain();
        this.includeLastModified = config.include_lastmod();
        this.includeInheritValue = config.include_inherit();
        this.changefreqProperties = config.changefreq_properties() == null ?
                ArrayUtils.EMPTY_STRING_ARRAY : config.changefreq_properties();
        this.priorityProperties = config.priority_properties() == null ?
                ArrayUtils.EMPTY_STRING_ARRAY : config.priority_properties();
        this.damAssetProperty = config.damassets_property() == null ?
                StringUtils.EMPTY : config.damassets_property();
        this.damAssetTypes = config.damassets_types() == null ?
                Collections.EMPTY_LIST : Arrays.asList(config.damassets_types());
        this.excludeFromSiteMapProperty = config.exclude_property();
        this.characterEncoding = config.character_encoding();
        this.extensionlessUrls = config.extensionless_urls();
        this.removeTrailingSlash = config.remove_slash();
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType(request.getResponseContentType());
        if (StringUtils.isNotEmpty(this.characterEncoding)) {
            response.setCharacterEncoding(characterEncoding);
        }
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
            for (Iterator<Page> children = page.listChildren(new PageFilter(false, true), true); children.hasNext();) {
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
                // Ensure that this folder is not a child folder of another
                // configured folder, since it will already be included when
                // the parent folder is traversed.
                if (StringUtils.isNotBlank(configuredAssetFolderPath) && !configuredAssetFolderPath.equals(prevPath)
                        && !StringUtils.startsWith(configuredAssetFolderPath, prevPath + "/")) {
                    Resource assetFolder = resolver.getResource(configuredAssetFolderPath);
                    if (assetFolder != null) {
                        prevPath = configuredAssetFolderPath;
                        allAssetFolders.add(assetFolder);
                    }
                }
            }
        }
        return allAssetFolders;
    }

    @SuppressWarnings("squid:S1192")
    private void write(Page page, XMLStreamWriter stream, ResourceResolver resolver) throws XMLStreamException {
        if (isHidden(page)) {
            return;
        }
        stream.writeStartElement(NS, "url");
        String loc = "";

        if (!extensionlessUrls) {
            loc = externalizer.externalLink(resolver, externalizerDomain, String.format("%s.html", page.getPath()));
        } else {
            String urlFormat = removeTrailingSlash ? "%s" : "%s/";
            loc = externalizer.externalLink(resolver, externalizerDomain, String.format(urlFormat, page.getPath()));
        }

        writeElement(stream, "loc", loc);

        if (includeLastModified) {
            Calendar cal = page.getLastModified();
            if (cal != null) {
                writeElement(stream, "lastmod", DATE_FORMAT.format(cal));
            }
        }

        if (includeInheritValue) {
            HierarchyNodeInheritanceValueMap hierarchyNodeInheritanceValueMap = new HierarchyNodeInheritanceValueMap(
                    page.getContentResource());
            writeFirstPropertyValue(stream, "changefreq", changefreqProperties, hierarchyNodeInheritanceValueMap);
            writeFirstPropertyValue(stream, "priority", priorityProperties, hierarchyNodeInheritanceValueMap);
        } else {
            ValueMap properties = page.getProperties();
            writeFirstPropertyValue(stream, "changefreq", changefreqProperties, properties);
            writeFirstPropertyValue(stream, "priority", priorityProperties, properties);
        }

        stream.writeEndElement();
    }

    private boolean isHidden(final Page page) {
        return page.getProperties().get(this.excludeFromSiteMapProperty, false);
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

        Resource contentResource = asset.adaptTo(Resource.class).getChild(JcrConstants.JCR_CONTENT);
        if (contentResource != null) {
            if (includeInheritValue) {
                HierarchyNodeInheritanceValueMap hierarchyNodeInheritanceValueMap = new HierarchyNodeInheritanceValueMap(
                        contentResource);
                writeFirstPropertyValue(stream, "changefreq", changefreqProperties, hierarchyNodeInheritanceValueMap);
                writeFirstPropertyValue(stream, "priority", priorityProperties, hierarchyNodeInheritanceValueMap);
            } else {
                ValueMap properties = contentResource.getValueMap();
                writeFirstPropertyValue(stream, "changefreq", changefreqProperties, properties);
                writeFirstPropertyValue(stream, "priority", priorityProperties, properties);
            }
        }

        stream.writeEndElement();
    }

    private void writeAssets(final XMLStreamWriter stream, final Resource assetFolder, final ResourceResolver resolver)
            throws XMLStreamException {
        for (Iterator<Resource> children = assetFolder.listChildren(); children.hasNext();) {
            Resource assetFolderChild = children.next();
            if (assetFolderChild.isResourceType(DamConstants.NT_DAM_ASSET)) {
                Asset asset = assetFolderChild.adaptTo(Asset.class);

                if (damAssetTypes.contains(asset.getMimeType())) {
                    writeAsset(asset, stream, resolver);
                }
            } else {
                writeAssets(stream, assetFolderChild, resolver);
            }
        }
    }

    private void writeFirstPropertyValue(final XMLStreamWriter stream, final String elementName,
            final String[] propertyNames, final ValueMap properties) throws XMLStreamException {
        for (String prop : propertyNames) {
            String value = properties.get(prop, String.class);
            if (value != null) {
                writeElement(stream, elementName, value);
                break;
            }
        }
    }

    @SuppressWarnings("squid:S1144")
    private void writeFirstPropertyValue(final XMLStreamWriter stream, final String elementName,
            final String[] propertyNames, final InheritanceValueMap properties) throws XMLStreamException {
        for (String prop : propertyNames) {
            String value = properties.get(prop, String.class);
            if (value == null) {
                value = properties.getInherited(prop, String.class);
            }
            if (value != null) {
                writeElement(stream, elementName, value);
                break;
            }
        }
    }

    private void writeElement(final XMLStreamWriter stream, final String elementName, final String text)
            throws XMLStreamException {
        stream.writeStartElement(NS, elementName);
        stream.writeCharacters(text);
        stream.writeEndElement();
    }

}
