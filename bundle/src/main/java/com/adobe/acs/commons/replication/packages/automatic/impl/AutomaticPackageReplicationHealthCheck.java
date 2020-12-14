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

import com.adobe.acs.commons.replication.packages.automatic.AutomaticPackageReplicator;

import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


@Component(service = HealthCheck.class, immediate = true, property = {
    HealthCheck.NAME + "=Automatic Package Replication", HealthCheck.TAGS + "=replication",
    HealthCheck.TAGS + "=packages" })
public class AutomaticPackageReplicationHealthCheck implements HealthCheck {

  @Reference
  private AutomaticPackageReplicator automaticPackageReplicator;

  @Override
  public Result execute() {
    ResultLog log = ((AutomaticPackageReplicatorImpl) automaticPackageReplicator).getResultLog();
    if(!log.iterator().hasNext()){
      return new Result(Result.Status.OK, "No Automatic Package Replications run");
    }
    return new Result(log);
  }
}
