package com.adobe.acs.commons.solr.impl;

import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;

import com.adobe.acs.commons.solr.SolrMetadataBuilder;
import com.day.cq.dam.api.Asset;
import com.day.cq.wcm.api.Page;

public class SolrContentBuilderTest {

    SolrContentBuilder scb;
    
    // a mocked pageResource
    Resource pageResource; 

    Resource assetResource;

    SolrMetadataBuilder pageBuilder = null;
    SolrMetadataBuilder assetBuilder = null;

    @Before
    public void setup() {
	scb = new SolrContentBuilder();

	Page page = mock(Page.class);
	when(page.getTitle()).thenReturn("Hello World");
	ValueMap vm = mock(ValueMap.class);
	when(vm.get("sling:resourceType")).thenReturn(
		"hello/world/resourceType");
	when(page.getProperties()).thenReturn(vm);
	pageResource = mock(Resource.class);
	when(pageResource.adaptTo(Page.class)).thenReturn(page);

	Asset asset = mock(Asset.class);
	when(asset.getMimeType()).thenReturn("text/text");
	assetResource = mock(Resource.class);
	when(assetResource.adaptTo(Asset.class)).thenReturn(asset);


    }
    
    protected void addDefaultMetadataBuilders() {

	// PageDefaultMetadataBuilder
	Map<String, Object> properties = new HashMap<String, Object>();
	properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE - 1);

	pageBuilder = new DefaultPageMetadataBuilder();
	assetBuilder = new DefaultAssetMetadataBuilder();

	scb.bindMetadataBuilder(pageBuilder, properties);
	scb.bindMetadataBuilder(assetBuilder, properties);

    }

    @Test
    public void testNoDefaultMetadataBuilderAvailableByDefault() {
	SolrMetadataBuilder builder = scb.getBuilderForResource(pageResource);
	Assert.assertEquals(builder, null);
	builder = scb.getBuilderForResource(assetResource);
	Assert.assertEquals(builder, null);
    }

    @Test
    public void testWithDefaultMetadataBuilders() {
	addDefaultMetadataBuilders();
	SolrMetadataBuilder builder = scb.getBuilderForResource(pageResource);
	Assert.assertTrue(builder == pageBuilder);

	builder = scb.getBuilderForResource(assetResource);
	Assert.assertTrue(builder == assetBuilder);

    }




}
