/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 - Adobe
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
package com.adobe.acs.commons.mcp.model;

import com.adobe.acs.commons.fam.Failure;
import java.util.Date;
import javax.inject.Inject;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

/**
 * Persisted failure information.
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ArchivedProcessFailure {
    @Inject
    public Date time;
    
    @Inject
    public String error;
    
    @Inject
    public String nodePath;
    
    @Inject
    public String stackTrace;
    
    public static ArchivedProcessFailure adapt(Failure source) {
        ArchivedProcessFailure dest = new ArchivedProcessFailure();
        if (source.getException() != null) {
            dest.time = source.getTime().getTime();
            dest.error = source.getException().getMessage();
            dest.stackTrace = ExceptionUtils.getStackTrace(source.getException());
        }
        dest.nodePath = source.getNodePath();
        return dest;
    }
}
