/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.reports.internal;

import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.github.jknack.handlebars.Handlebars;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;

@SlingServlet(resourceTypes = "acs-commons/components/report-builder/rendercondition")
public class ReportsRenderCondition extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws ServletException, IOException {
        request.setAttribute(RenderCondition.class.getName(), INSTANCE);
    }

    private static final RenderCondition INSTANCE = new RenderCondition() {
        @Override
        public boolean check() {
            try {
                new Handlebars();
                return true;
            } catch (NoClassDefFoundError e) {
                return false;
            }
        }
    };
}
