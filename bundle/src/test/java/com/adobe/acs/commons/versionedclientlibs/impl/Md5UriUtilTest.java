/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2017 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
