/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.reports.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportExceptionTest {

  private static final Logger log = LoggerFactory.getLogger(ReportExceptionTest.class);
  
  private static final String MESSAGE="Hello World";
  private static final Exception CAUSE = new Exception();

  @Test
  public void testReportException() {
    log.info("testReportException");
    
    ReportException rem = new ReportException(MESSAGE);
    assertEquals(MESSAGE, rem.getMessage());
    assertNull(rem.getCause());
    

    ReportException reme = new ReportException(MESSAGE, CAUSE);
    assertEquals(MESSAGE, reme.getMessage());
    assertNotNull(reme.getCause());
    assertEquals(CAUSE, reme.getCause());
    
    log.info("Test successful!");
  }

}
