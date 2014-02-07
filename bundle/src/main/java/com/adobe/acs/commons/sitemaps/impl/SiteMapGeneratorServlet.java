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

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.adobe.acs.commons.sitemaps.SiteMapGenerator;

@SuppressWarnings("serial")
@Component(metatype = true, label = "ACS sitemap xml servlet")
@Service
@Properties({
        @Property(name = "sling.servlet.paths", value = SiteMapConstants.SERVLET_PATH),
        @Property(name = "sling.servlet.methods", value = SiteMapConstants.SERVLET_REQUEST_METHOD),
        @Property(name = "sling.servlet.extensions", value = SiteMapConstants.SERVLET_URL_EXTENSION) })
public class SiteMapGeneratorServlet extends SlingSafeMethodsServlet {
    private static final Logger log = LoggerFactory
            .getLogger(SiteMapGeneratorServlet.class);

    @Reference(name = "siteMapGenerators", referenceInterface = SiteMapGenerator.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Map<String, SiteMapGenerator> siteMapGenerators = new TreeMap<String, SiteMapGenerator>();

    private TransformerFactory transformerFactory ;
    
    protected void bindSiteMapGenerators(
            final SiteMapGenerator siteMapGenerator,
            final Map<String, Object> props) {
        synchronized (this.siteMapGenerators) {
            this.siteMapGenerators.put(PropertiesUtil.toString(
                    props.get(SiteMapConstants.DOMAIN_NAME), "localhost:4502"),
                    siteMapGenerator);
        }
    }

    protected void unbindSiteMapGenerators(
            final SiteMapGenerator siteMapGenerator,
            final Map<String, Object> props) {
        synchronized (this.siteMapGenerators) {
            this.siteMapGenerators.remove(PropertiesUtil.toString(
                    props.get(SiteMapConstants.DOMAIN_NAME),"localhost:4502"));
        }
    }

    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) {
        String currentDomain = request.getHeader("Host");
       Document siteMapDocument = this.siteMapGenerators.get(currentDomain).getSiteMap(request.getResourceResolver());
       printSiteMap(siteMapDocument, response);
    }
    
    private void printSiteMap(Document document, SlingHttpServletResponse response){
        try{
            response.setContentType("application/xml");
            response.setCharacterEncoding("UTF-8");
            getTransformer().transform(new DOMSource(document), new StreamResult(response.getWriter()));
        }catch(IOException ex){
            log.error(ex.getMessage(),ex);
        } catch (TransformerException e) {
            log.error(e.getMessage(),e);
        }
    }
    
    private Transformer getTransformer(){
        try{
            Transformer transformer = transformerFactory.newTransformer();
            return transformer;
        }catch(TransformerConfigurationException e){
            log.error(e.getMessage(),e);
        }
        return null;
    }
    @Activate
    @Modified
    protected void activate( final Map<String, Object> properties ){
     
        transformerFactory = TransformerFactory.newInstance();
      
    }
}
