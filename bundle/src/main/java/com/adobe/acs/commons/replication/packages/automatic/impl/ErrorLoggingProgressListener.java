/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 - Adobe
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
package com.adobe.acs.commons.replication.packages.automatic.impl;

import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorLoggingProgressListener implements ProgressTrackerListener {

  private static final Logger log = LoggerFactory.getLogger(ErrorLoggingProgressListener.class);

  @Override
  public void onError(Mode mode, String path, Exception e) {
    String name = path;
    if (mode == Mode.PATHS) {
      name = path.substring(path.lastIndexOf('/') + 1);
    }
    log.warn("Error on {}", name, e);
  }

  @Override
  public void onMessage(Mode mode, String action, String path) {
    // NOOP
  }

}
