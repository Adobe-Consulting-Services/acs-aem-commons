package com.adobe.acs.commons.adobeio.types;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FilterTest {

	@Test
	public void testFilterWithNotNullValue() {
		Filter filter = new Filter("name", "value");
		
        assertEquals(filter.getFilter(), "name=value");

	}
	
}
