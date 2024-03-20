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
    AEM Workflow - Transient
   </strong>
  <div class="coral-Alert-message">
    The workflow will be executed as Transient using the AEM Workflow engine.
    AEM Workflow engine does not track Transient workflow (they are managed as a single Sling Job) thus, Bulk Workflow Manager cannot report on success or failures, but only how many payloads it provides to the AEM Workflow engine.
  </div>
</div>