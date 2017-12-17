package com.adobe.acs.commons.jcrpersist.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class AssertUtilsTest {
	
	@Test
	public void testIsEmptyString() {
		// strings
		assertTrue(AssertUtils.isEmpty((String) null));
		assertFalse(AssertUtils.isNotEmpty((String) null));

		assertTrue(AssertUtils.isEmpty(""));
		assertFalse(AssertUtils.isNotEmpty(""));

		assertTrue(AssertUtils.isNotEmpty(" "));
		assertFalse(AssertUtils.isEmpty(" "));

		assertTrue(AssertUtils.isNotEmpty(" abc"));
		assertFalse(AssertUtils.isEmpty(" abc"));
	}
	
	@Test
	public void testIsEmptyArray() {
		assertTrue(AssertUtils.isEmpty((String[]) null));

		assertTrue(AssertUtils.isEmpty((Object[]) null));
		assertTrue(AssertUtils.isEmpty((Object[]) new Object[] { }));
		assertFalse(AssertUtils.isEmpty((Object[]) new Object[] { new Object() }));

		assertFalse(AssertUtils.isEmpty(new String[] { "", "" }));
	}

}
