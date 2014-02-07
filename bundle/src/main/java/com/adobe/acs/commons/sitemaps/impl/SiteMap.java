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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.ValueMap;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;

import static com.adobe.acs.commons.sitemaps.impl.SiteMapConstants.DEFAULT_DATE_FORMAT;
import static com.adobe.acs.commons.sitemaps.impl.SiteMapConstants.PAGE_PROP_UPD_FREQ;
import static com.adobe.acs.commons.sitemaps.impl.SiteMapConstants.PRIORITY;
import static com.adobe.acs.commons.sitemaps.impl.SiteMapConstants.PAGE_PROP_PRIORITY_DEFAULT;
import static com.adobe.acs.commons.sitemaps.impl.SiteMapConstants.PAGE_PROP_UPD_FREQ_DEFAULT;
import static com.adobe.acs.commons.sitemaps.impl.SiteMapConstants.DEFAULT_LINK_EXTENSION;

public class SiteMap implements Iterable<SiteMap.LinkElement> {

    private final Page page;
    private final PageFilter filter;
    private boolean considerPageFilter;
    
    public SiteMap(Page page,PageFilter filter, boolean considerPageFilter){
        this.page = page;
        this.filter = filter;
        this.considerPageFilter = considerPageFilter;
    }
    @Override
    public Iterator<SiteMap.LinkElement> iterator() {
        List<SiteMap.LinkElement> links  = new ArrayList<SiteMap.LinkElement>();
        getLinksInSite(links, page);      
        return links.iterator();
    }
    private void getLinksInSite(List<SiteMap.LinkElement> links, Page rootPage){
        links.add(getSiteMapLinkElement(rootPage));
        for(Iterator<Page> iter =getChildren(rootPage);iter.hasNext();){
            Page child = iter.next();
            getLinksInSite(links,child);
        }
    }
    private Iterator<Page> getChildren(Page rootPage){
        if(considerPageFilter){
            return rootPage.listChildren(filter);
        }
        return rootPage.listChildren();
    }
    private SiteMap.LinkElement getSiteMapLinkElement(Page page){
        ValueMap map = page.getProperties();
        String updationFreq = map.get(PAGE_PROP_UPD_FREQ, PAGE_PROP_UPD_FREQ_DEFAULT);
        String priority = map.get(PRIORITY,PAGE_PROP_PRIORITY_DEFAULT);
        String lastModifiedDate = getDateAsString( page.getLastModified().getTime(), DEFAULT_DATE_FORMAT);

        return new SiteMap.LinkElement(page.getPath()+"."+DEFAULT_LINK_EXTENSION, lastModifiedDate, updationFreq, priority);
    }
    private String getDateAsString(Date date,String format) {
           
           return new SimpleDateFormat(format).format(date);
     
        }
 static class LinkElement{
    private String link;
    private String lastModifiedDate;
    private String updationFreq;
    private String priority;
    public LinkElement(String link, String lastModifiedDate, String updationFreq, String priority){
        this.link =  link;
        this.lastModifiedDate =  lastModifiedDate;
        this.updationFreq = updationFreq;
        this.priority = priority;
    }
    public String getLink() {
        return link;
    }
    public String getLastModifiedDate() {
        return lastModifiedDate;
    }
    public String getUpdationFreq() {
        return updationFreq;
    }
    public String getPriority() {
        return priority;
    }
    
}
}
