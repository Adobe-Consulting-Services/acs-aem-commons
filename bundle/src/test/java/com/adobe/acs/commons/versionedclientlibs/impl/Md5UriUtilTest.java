package com.adobe.acs.commons.versionedclientlibs.impl;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class Md5UriUtilTest {

  @Test
  public void getMD5FromURI_withMin() throws Exception {
    assertThat(Md5UriUtil.getMD5FromURI(
      "/etc/designs/geometrixx/clientlib-landing.min.459053195dbe3e59ecc1b07120374f2c.js"),
      is("459053195dbe3e59ecc1b07120374f2c"));
  }

  @Test
  public void getMD5FromURI_withOutMin() throws Exception {
    assertThat(Md5UriUtil.getMD5FromURI("/etc/designs/geometrixx/clientlib-landing.459053195dbe3e59ecc1b07120374f2c.js"),
      is("459053195dbe3e59ecc1b07120374f2c"));
  }

  @Test
  public void getMD5FromURI_withOutMD5() throws Exception {
    assertThat(Md5UriUtil.getMD5FromURI("/etc/designs/geometrixx/clientlib-landing.min.js"), is(""));
  }

  @Test
  public void getMD5FromURI_withBrol() throws Exception {
    assertThat(Md5UriUtil.getMD5FromURI("/etc/designs/geometrixx/clientlib-landing.min.brol.js"), is("brol"));
  }

  @Test
  public void getMD5FromURI_withBrol_withOutMin() throws Exception {
    assertThat(Md5UriUtil.getMD5FromURI("/etc/designs/geometrixx/clientlib-landing.brol.js"), is("brol"));
  }

  @Test
  public void cleanURI_withMin() throws Exception {
    assertThat(Md5UriUtil.cleanURI("/etc/designs/geometrixx/clientlib-landing.min.459053195dbe3e59ecc1b07120374f2c.js"),
      is("/etc/designs/geometrixx/clientlib-landing.js"));
  }

  @Test
  public void cleanURI_withOutMin() throws Exception {
    assertThat(Md5UriUtil.cleanURI("/etc/designs/geometrixx/clientlib-landing.459053195dbe3e59ecc1b07120374f2c.js"),
      is("/etc/designs/geometrixx/clientlib-landing.js"));
  }

  @Test
  public void cleanURI_withOutMD5() throws Exception {
    assertThat(Md5UriUtil.cleanURI("/etc/designs/geometrixx/clientlib-landing.min.js"),
      is("/etc/designs/geometrixx/clientlib-landing.js"));
  }

  @Test
  public void cleanURI_withBrol() throws Exception {
    assertThat(Md5UriUtil.cleanURI("/etc/designs/geometrixx/clientlib-landing.min.brol.js"),
      is("/etc/designs/geometrixx/clientlib-landing.js"));
  }

  @Test
  public void cleanURI_withBrol_withOutMin() throws Exception {
    assertThat(Md5UriUtil.cleanURI("/etc/designs/geometrixx/clientlib-landing.brol.js"),
      is("/etc/designs/geometrixx/clientlib-landing.js"));
  }

  @Test
  public void cleanURI_tooShort() {
    assertThat(Md5UriUtil.cleanURI("/etc/designs/geometrixx.js"), is("/etc/designs/geometrixx.js"));
  }

}
