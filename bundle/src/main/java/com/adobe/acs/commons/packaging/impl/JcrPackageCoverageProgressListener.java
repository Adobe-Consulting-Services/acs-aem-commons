/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

package com.adobe.acs.commons.packaging.impl;

import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JcrPackageCoverageProgressListener implements ProgressTrackerListener {
    private final List<String> coverage = new ArrayList<String>();

    @Override
    public final void onMessage(final Mode mode, final String action, final String path) {
        coverage.add(path);
    }

    @Override
    public final void onError(final Mode mode, final String path, final Exception e) {
        // no need to track errors
    }

    public final List<String> getCoverage() {
        return Collections.unmodifiableList(coverage);
    }
}
