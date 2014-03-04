package com.adobe.acs.commons.tabs;

import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 */
public abstract class Configurator
{
    protected ResourceResolver resolver = null;
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected Resource jcrResource;
    protected ValueMap valueMap;
    protected Session session = null;

    /**
     * 
     */
    public Configurator()
    {

    }

}
