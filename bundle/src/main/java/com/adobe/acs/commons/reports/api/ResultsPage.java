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

import java.util.List;
import java.util.Objects;

/**
 * Simple POJO for representing a page of results.
 */
public final class ResultsPage {

  private final List<Object> results;
  private final int pageSize;
  private final int page;

  public ResultsPage(List<Object> results, int pageSize, int page) {
    this.results = results;
    this.pageSize = pageSize;
    this.page = page;
  }

  public List<Object> getResults() {
    return results;
  }

  public int getResultsStart() {
    return page != -1 ? (pageSize * page) + 1 : 1;
  }

  public int getResultsEnd() {
    return page != -1 ? (pageSize * page) + results.size() : results.size();
  }

  public int getNextPage() {
    return (results.size() == pageSize && page != -1) ? page + 1 : -1;
  }

  public int getPreviousPage() {
    return page > 0 ? page - 1 : -1;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof ResultsPage)) {
      return false;
    }

    final ResultsPage that = (ResultsPage) o;

    return pageSize == that.pageSize
           && page == that.page
           && Objects.equals(results, that.results);
  }

  @Override
  public int hashCode() {

    return Objects.hash(results, pageSize, page);
  }
}
