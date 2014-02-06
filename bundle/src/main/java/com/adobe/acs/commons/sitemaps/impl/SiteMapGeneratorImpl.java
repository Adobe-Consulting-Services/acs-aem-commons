/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.sitemaps.impl;

import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.adobe.acs.commons.sitemaps.SiteMapGenerator;
import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
import com.day.cq.wcm.api.PageManager;

@SuppressWarnings("serial")
@Component(metatype = true, configurationFactory=true, label = "ACS sitemap Generator",policy=ConfigurationPolicy.REQUIRE)
@Service
public class SiteMapGeneratorImpl implements SiteMapGenerator {
    private static final Logger log = LoggerFactory
            .getLogger(SiteMapGeneratorImpl.class);
    private static final String XML_NS_ATTR_XSI_KEY="xmlns:xsi";
    private static final String XML_NS_ATTR_XSI_VAL="http://www.w3.org/2001/XMLSchema-instance";
    private static final String XML_SCHEMA_LOC_ATTR_KEY="xsi:schemaLocation";
    private static final String XML_SCHEMA_LOC_ATTR_VAL="http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd";
    private static final String XML_NS_ATTR_KEY="xmlns";
    private static final String XML_NS_ATTR_VAL="http://www.sitemaps.org/schemas/sitemap/0.9";
    
    @Property(name =SiteMapConstants.SITE_ROOT_PATH, label="Root Path", description="Site Root path for sitemap", propertyPrivate = false, value = "/content")
    public static final String SITE_ROOT_PATH = SiteMapConstants.SITE_ROOT_PATH;
    
    @Property(name = SiteMapConstants.DOMAIN_NAME,label="Domain Name", description="Domain Name of site", propertyPrivate = false, value = "www.google.com")
    public static final String DOMAIN_NAME = SiteMapConstants.DOMAIN_NAME;

    @Property(name = SiteMapGeneratorImpl.CONSIDER_IS_HIDE_IN_NAVIGATION_FILTER_ENABLED, boolValue = true, label = "Consider is Hide in Navigation Page Filter", description = "Check to enable the page navigation filter")
    public static final String CONSIDER_IS_HIDE_IN_NAVIGATION_FILTER_ENABLED="considerishideinnavigationfilter.enabled";
    
    private String siteRootPath;
    private String domainName;
    private boolean considerPageFilter;
    
    


    @Reference
    private Externalizer externalizer;

    @Override
    public Document getSiteMap(ResourceResolver resolver){
        PageManager pageManager = resolver.adaptTo(PageManager.class);
        Page rootPage = pageManager.getPage(siteRootPath);
        PageFilter pageFilter = new PageFilter();
       Iterator<SiteMap.LinkElement> linksIterator = new SiteMap(rootPage, pageFilter, considerPageFilter).iterator();
       Document siteMap = generateWebSiteMap(resolver,linksIterator);
       return siteMap;
    }
    
    private Document generateWebSiteMap(ResourceResolver resolver, Iterator<SiteMap.LinkElement> linksIterator) {
        Document siteMapDocument = createSiteMapDocument();
        Element urlSetElement = createUrlSetElement(siteMapDocument);
        while(linksIterator.hasNext()
                ){
            SiteMap.LinkElement linkElement =  linksIterator.next();
            urlSetElement.appendChild(createUrlElement(resolver, linkElement , siteMapDocument));
        }
        siteMapDocument.appendChild(urlSetElement);
        return siteMapDocument;
    }
    
    private Element createUrlElement(ResourceResolver resolver, SiteMap.LinkElement linkElement, Document siteMapDocument){
        Element url = createElement("url", siteMapDocument);
        url.appendChild(createLocElement(externalizer.externalLink(resolver,domainName,linkElement.getLink()), siteMapDocument));
        url.appendChild(createLastmodElement(linkElement.getLastModifiedDate(), siteMapDocument));
        url.appendChild(createFrequencyElement(linkElement.getUpdationFreq(), siteMapDocument));
        url.appendChild(createPriorityElement(linkElement.getPriority(), siteMapDocument));
        return url;
    }
    private Element createLastmodElement(String lastModDate, Document siteMapDocument){
        Element lastmodEl = createElement("lastmod", siteMapDocument);
        lastmodEl.appendChild(siteMapDocument.createTextNode(lastModDate));
        return lastmodEl;
    }
    private Element createFrequencyElement(String updationFrequency, Document siteMapDocument){
        Element freq = createElement("changefreq", siteMapDocument);
        freq.appendChild(siteMapDocument.createTextNode(updationFrequency));
        return freq;
    }
    private Element createPriorityElement(String priority, Document siteMapDocument){
        Element priEl = createElement("priority", siteMapDocument);
        priEl.appendChild(siteMapDocument.createTextNode(priority));
        return priEl;
    }
    private Element createLocElement(String link, Document siteMapDocument){
        Element loc = createElement("loc", siteMapDocument);
        loc.appendChild(siteMapDocument.createTextNode(link));
        return loc;
    }
    private Element createUrlSetElement(Document siteMapDocument){
        Element urlSet = createElement("urlset", siteMapDocument);
        urlSet.setAttribute(XML_NS_ATTR_XSI_KEY,XML_NS_ATTR_XSI_VAL);
        urlSet.setAttribute(XML_SCHEMA_LOC_ATTR_KEY,XML_SCHEMA_LOC_ATTR_VAL);
        urlSet.setAttribute(XML_NS_ATTR_KEY,XML_NS_ATTR_VAL);
        return urlSet;
    }
    private Element createElement(String elementName, Document siteMapDocument){
        return  siteMapDocument.createElement(elementName);
    }
    private Document createSiteMapDocument(){
        try {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder  docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        return doc;
        } catch (ParserConfigurationException e) {
            log.error(e.getMessage(),e);
           return null;
        }
        
    }
    

    
    @Activate
    @Modified
    protected void activate( final Map<String, Object> properties ){
        siteRootPath = PropertiesUtil.toString(properties.get(SITE_ROOT_PATH),"");
        domainName = PropertiesUtil.toString(properties.get(DOMAIN_NAME),"");
        considerPageFilter=PropertiesUtil.toBoolean(properties.get(CONSIDER_IS_HIDE_IN_NAVIGATION_FILTER_ENABLED),false);
    }
    

    
}

