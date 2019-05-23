package com.adobe.acs.commons.rewriter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import com.adobe.acs.commons.properties.PropertyAggregatorService;
import com.adobe.acs.commons.properties.util.TemplateReplacementUtil;
import com.adobe.granite.rest.Constants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

@Component(service = TransformerFactory.class, property = {
        "pipeline.type=templated-transformer"
    })
public class TemplatedTransformer implements Transformer, TransformerFactory {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private ContentHandler contentHandler;
    private Map<String, Object> properties;

    @Reference
    private PropertyAggregatorService propertyAggregatorService;

    private PropertyAggregatorService localService;

    public TemplatedTransformer() {}

    public TemplatedTransformer(PropertyAggregatorService propertyAggregatorService) {
        this.localService = propertyAggregatorService;
    }

    @Override
    public Transformer createTransformer() {
        log.trace("Templated Transformer");
        return new TemplatedTransformer(propertyAggregatorService);
    }

    @Override
    public void init(ProcessingContext processingContext, ProcessingComponentConfiguration processingComponentConfiguration) throws IOException {
        SlingHttpServletRequest request = processingContext.getRequest();
        PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);

        // Get the combined properties via service
        Page page = pageManager.getContainingPage(request.getResource());
        if (page != null) {
            properties = localService.getProperties(page);
        }
    }

    @Override
    public void startElement(String uri, String localName, String quaName, Attributes atts) throws SAXException {
        if (shouldRun() && localName.equals("a")) {
            AttributesImpl newAttrs = new AttributesImpl(atts);
            for (int i = 0; i < newAttrs.getLength(); i++) {
                String currentAttribute = decode(newAttrs.getValue(i));
                if (TemplateReplacementUtil.hasPlaceholder(currentAttribute)) {

                    // Get the current placeholder in the string
                    String placeholder = TemplateReplacementUtil.getPlaceholder(currentAttribute);

                    // Transform it to the key in the property map
                    String key = TemplateReplacementUtil.getKey(placeholder);

                    // If the placeholder key is in the map then replace it
                    if (properties.containsKey(key)) {
                        String replaceValue = (String) properties.get(key);
                        newAttrs.setValue(i, currentAttribute.replace(placeholder, encode(replaceValue)));
                    }
                }
            }
            contentHandler.startElement(uri, localName, quaName, newAttrs);
        } else {
            contentHandler.startElement(uri, localName, quaName, atts);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        String currentString = new String(ch);
        int placeLength = length;

        if (shouldRun()) {
            String currentPlace = currentString.substring(start, start + length);

            // Get the current placeholders in the string
            final List<String> placeholders = TemplateReplacementUtil.getPlaceholders(currentPlace);

            for (String placeholder : placeholders) {
                // Transform it to the key in the property map
                final String key = TemplateReplacementUtil.getKey(placeholder);

                // If the placeholder key is in the map then replace it
                if (properties.containsKey(key)) {
                    final String replaceValue = String.valueOf(properties.get(key));
                    final String replace = currentPlace.replace(placeholder, replaceValue);
                    placeLength = replace.length();
                    currentString = currentString.replace(currentPlace, replace);
                    currentPlace = replace;
                }
            }
        }

        contentHandler.characters(currentString.toCharArray(), start, placeLength);
    }

    private boolean shouldRun() {
        return localService.isEnabled() && properties != null;
    }

    private String decode(String input) {
        try {
            input = URLDecoder.decode(input, Constants.DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            log.error("Error decoding object");
        }
        return input;
    }

    private String encode(String input) {
        try {
            input = URLEncoder.encode(input, Constants.DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            log.error("Error encoding object");
        }
        return input;
    }

    @Override
    public void endElement(String uri, String localName, String quaName) throws SAXException {
        contentHandler.endElement(uri, localName, quaName);
    }

    @Override
    public void setContentHandler(ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    @Override
    public void dispose() {

    }

    @Override
    public void setDocumentLocator(Locator locator) {
        contentHandler.setDocumentLocator(locator);
    }

    @Override
    public void startDocument() throws SAXException {
        contentHandler.startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        contentHandler.endDocument();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        contentHandler.startPrefixMapping(prefix, uri);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        contentHandler.endPrefixMapping(prefix);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        contentHandler.ignorableWhitespace(ch, start, length);
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        contentHandler.processingInstruction(target, data);
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        contentHandler.skippedEntity(name);
    }
}
