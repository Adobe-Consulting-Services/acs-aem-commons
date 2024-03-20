<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2016 Adobe
  ~ %%
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~ #L%
  --%>
<div class="coral-Alert coral-Alert--info coral-Alert--large">
  <i class="coral-Alert-typeIcon coral-Icon coral-Icon--sizeS coral-Icon--infoCircle"></i>
  <strong class="coral-Alert-title">
    Synthetic Workflow - Multi-threaded
   </strong>
  <div class="coral-Alert-message">
    Payloads will be collected and put under
    <a target="_blank" href="https://adobe-consulting-services.github.io/acs-aem-commons/features/synthetic-workflow.html">Synthetic Workflow</a> in parallel via the <a target="_blank" href="https://adobe-consulting-services.github.io/acs-aem-commons/features/fast-action-manager.html">ACS Commons Fast Action Manager</a> APIs.
    Synthetic Workflow does not engage the AEM Workflow Engine for management of work, and thus avoids the overhead of Sling Jobs.
    This is typically the fastest and robust mechanism for applying (Synthetic) Workflow at scale.
  </div>
</div>