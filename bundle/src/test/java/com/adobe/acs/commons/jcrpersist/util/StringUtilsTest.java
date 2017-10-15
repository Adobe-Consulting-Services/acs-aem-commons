package com.adobe.acs.commons.jcrpersist.util;

import org.junit.Assert;
import org.junit.Test;

public class StringUtilsTest {

	@Test
	public void testGetBoolean() {
		Assert.assertFalse(StringUtils.getBoolean(null));
		Assert.assertFalse(StringUtils.getBoolean(""));
		Assert.assertFalse(StringUtils.getBoolean("abc"));
		Assert.assertFalse(StringUtils.getBoolean("no"));

		Assert.assertTrue(StringUtils.getBoolean("yes"));
		Assert.assertTrue(StringUtils.getBoolean("yEs"));
		Assert.assertTrue(StringUtils.getBoolean("YES"));
		Assert.assertTrue(StringUtils.getBoolean("true"));
		Assert.assertTrue(StringUtils.getBoolean("tRuE"));
		Assert.assertTrue(StringUtils.getBoolean("TRUE"));
	}

	
}
