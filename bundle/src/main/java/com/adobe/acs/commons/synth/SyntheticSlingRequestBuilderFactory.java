package com.adobe.acs.commons.synth;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


@Component(service = SyntheticSlingRequestBuilderFactory.class)
public class SyntheticSlingRequestBuilderFactory {

    @Reference
    private SyntheticRequestDispatcherFactory dispatcherFactory;

    /**
     * SyntheticSlingRequestBuilderFactory - produces a SyntheticSlingRequestBuilder
     * @see SyntheticSlingRequestBuilder
     * @param resourceResolver
     * @param targetResourcePath the target resource path of the synthetic sling request. a request always requires a target resource
     * @return
     */
    public SyntheticSlingRequestBuilder getBuilder(ResourceResolver resourceResolver, String targetResourcePath){
        return new SyntheticSlingRequestBuilder(dispatcherFactory, resourceResolver, targetResourcePath);
    }

    /**
     * SyntheticSlingRequestBuilderFactory - produces a SyntheticSlingRequestBuilder
     * @see SyntheticSlingRequestBuilder
     * @param resourceResolver
     * @param targetResource the target resource of the synthetic sling request. a request always requires a target resource
     * @return
     */
    public SyntheticSlingRequestBuilder getBuilder(ResourceResolver resourceResolver, Resource targetResource){
        return new SyntheticSlingRequestBuilder(dispatcherFactory, resourceResolver, targetResource);
    }
}