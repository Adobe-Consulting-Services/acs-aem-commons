/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.util.impl;

import com.adobe.acs.commons.util.impl.Activator;
import org.apache.sling.testing.mock.osgi.context.ContextCallback;
import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;

public final class ActivatorHelper {

  private final Activator activator = new Activator();

  public ContextCallback<OsgiContextImpl> afterSetup() {
    return (OsgiContextImpl ctx) -> activator.start(ctx.bundleContext());
  }

  public ContextCallback<OsgiContextImpl> beforeTeardown() {
    return (OsgiContextImpl ctx) -> activator.stop(ctx.bundleContext());
  }
}
