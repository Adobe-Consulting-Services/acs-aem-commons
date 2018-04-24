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
package com.adobe.acs.commons.reports.models;

import org.apache.sling.api.resource.Resource;

import com.adobe.acs.commons.reports.api.ReportException;
import com.adobe.acs.commons.reports.api.ReportExecutor;
import com.adobe.acs.commons.reports.api.ResultsPage;

public class MockReportExecutor implements ReportExecutor {

    @Override
    public String getDetails() throws ReportException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getParameters() throws ReportException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultsPage getAllResults() throws ReportException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultsPage getResults() throws ReportException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setConfiguration(Resource config) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setPage(int page) {
        // TODO Auto-generated method stub

    }

}
