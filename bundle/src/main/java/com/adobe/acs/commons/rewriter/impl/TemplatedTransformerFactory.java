package com.adobe.acs.commons.rewriter.impl;

import com.adobe.acs.commons.properties.PropertyAggregatorService;

import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = TransformerFactory.class, property = {
        "pipeline.type=templated-transformer"
})
public class TemplatedTransformerFactory implements TransformerFactory {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Reference
    private PropertyAggregatorService propertyAggregatorService;

    @Override
    public Transformer createTransformer() {
        log.trace("Templated Transformer");
        return new TemplatedTransformer(propertyAggregatorService);
    }
}
