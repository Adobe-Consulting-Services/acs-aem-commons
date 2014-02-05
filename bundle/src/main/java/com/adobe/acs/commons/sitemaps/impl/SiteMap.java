package com.adobe.acs.commons.sitemaps.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.ValueMap;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;

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
        ValueMap map = page.adaptTo(ValueMap.class);
        String updationFreq = map.get("updationFreq", "daily");
        String priority = map.get("priority","0.5");
        String lastModifiedDate = getDateAsString( page.getLastModified().getTime(), "yyyy-MM-dd'T'hh:mm:ss XXX");

        return new SiteMap.LinkElement(page.getPath()+".html", lastModifiedDate, updationFreq, priority);
    }
    private String getDateAsString(Date date,String format) {
           String dateStr = "";
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
              
                dateStr = sdf.format(date);
            } catch (Exception e) {
               
            }
            return dateStr;
        }
 class LinkElement{
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
