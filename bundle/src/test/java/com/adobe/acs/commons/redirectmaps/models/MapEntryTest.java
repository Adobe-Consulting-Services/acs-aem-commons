
package com.adobe.acs.commons.redirectmaps.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.commons.testing.sling.MockResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class MapEntryTest {

	private Resource mockResource = null;
	
	private static final Logger log = LoggerFactory.getLogger(MapEntryTest.class);

	@Before
	public void init() {
		log.info("init");
		MockResourceResolver mockResourceResolver = new MockResourceResolver();
		mockResource = new MockResource(mockResourceResolver,
				"/etc/acs-commons/redirect-maps/redirectmap1/jcr:content/redirects/2_123",
				"acs-commons/components/utilities/redirects");
	}

	@Test
	public void testInvalidMapEntry() {
		
		log.info("testInvalidMapEntry");
		String source = "/vanity 2";
		MapEntry invalid = new MapEntry(mockResource, source, mockResource.getPath());
		
		log.info("Asserting that entry is invalid");
		assertFalse(invalid.isValid());
		
		log.info("Asserting that matches expected values");
		assertEquals(invalid.getResource(),mockResource);
		assertEquals(invalid.getSource(), source);
		assertEquals(invalid.getTarget(), mockResource.getPath());
		
		log.info("Test successful!");
	}

	@Test
	public void testValidMapEntry() {
		
		log.info("testValidMapEntry");
		String source = "/vanity-2";
		MapEntry valid = new MapEntry(mockResource, source, mockResource.getPath());
		
		log.info("Asserting that entry is valid");
		assertTrue(valid.isValid());
		
		log.info("Asserting that matches expected values");
		assertEquals(valid.getResource(),mockResource);
		assertEquals(valid.getSource(), source);
		assertEquals(valid.getTarget(), mockResource.getPath());
		
		log.info("Test successful!");
	}
}
