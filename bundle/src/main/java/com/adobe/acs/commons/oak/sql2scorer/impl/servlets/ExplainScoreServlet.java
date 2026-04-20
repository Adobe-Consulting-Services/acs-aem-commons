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
package com.adobe.acs.commons.oak.sql2scorer.impl.servlets;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.Optional;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.jackrabbit.util.ISO8601;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * This servlet submits a JCR-SQL2 query with an additional oak:scoreExplanation column that contains the explanation of
 * the Lucene fulltext score, where applicable. Also executes the "explain" command to provide the index plan used in the
 * same JSON response.
 * <p>
 * The query result is returned as rows, not nodes, which means jcr:path must actually be selected in the query if it is
 * to be returned as a column.
 * <p>
 * This servlet is intended to be used as a tool for development and exploration of oak:index definitions, and is not
 * designed for running arbitrary queries, since the oak:scoreExplanation column will add considerable cost to each
 * operation.
 * <p>
 * The response JSON object contains 4 properties:
 * - stmt: the JCR-SQL2 query statement, as executed
 * - plan: the 'explain' plan, which identifies the index used and the filter expression
 * - cols: an array of column names specified in the executed query statement
 * - rows: a 2-dimensional array of query result rows, as in '.rows[result index][column index]'. Each row has as many
 * array elements as there are elements in the .cols array. A non-existing value for a row in a particular
 * column will be represented by the json {@code null} value.
 * <p>
 * The first column will always be 'oak:scoreExplanation', i.e. .cols[0] === 'oak:scoreExplanation', in order to make it
 * possible to treat the score explanation as a special case in the UI.
 * <p>
 * The first element of each row, therefore, contains the text of the score explanation, which is a pre-formatted string
 * with new lines and a 2-space shift-width indent. If a fulltext score is not computed for the query,
 * i.e. because the statement did not use the 'contains()' function or because the plan selected a non-lucene index, the
 * first element will contain the JSON null value.
 * <p>
 * For example, if you were to submit the following query:
 * <p>
 * select [jcr:path] from [cq:Page] where contains(*, 'we-retail')
 * <p>
 * Your plan might be something like:
 * <p>
 * [cq:Page] as [cq:Page] /* lucene:cqPageLucene(/oak:index/cqPageLucene) +:fulltext:we +:fulltext:retail
 * ft:("we-retail") where contains([cq:Page].[*], 'we-retail')
 * <p>
 * You might get an explanation similar to this for the first result:
 *
 * <pre>
 * 4.255377 = (MATCH) sum of:
 *   2.0704787 = (MATCH) weight(:fulltext:we in 80137) [DefaultSimilarity], result of:
 *     2.0704787 = score(doc=80137,freq=2.0 = termFreq=2.0
 * ), product of:
 *       0.6975356 = queryWeight, product of:
 *         4.1977777 = idf(docFreq=6362, maxDocs=155754)
 *         0.16616783 = queryNorm
 *       2.968277 = fieldWeight in 80137, product of:
 *         1.4142135 = tf(freq=2.0), with freq of:
 *           2.0 = termFreq=2.0
 *         4.1977777 = idf(docFreq=6362, maxDocs=155754)
 *         0.5 = fieldNorm(doc=80137)
 *   2.1848981 = (MATCH) weight(:fulltext:retail in 80137) [DefaultSimilarity], result of:
 *     2.1848981 = score(doc=80137,freq=2.0 = termFreq=2.0
 * ), product of:
 *       0.7165501 = queryWeight, product of:
 *         4.312207 = idf(docFreq=5674, maxDocs=155754)
 *         0.16616783 = queryNorm
 *       3.049191 = fieldWeight in 80137, product of:
 *         1.4142135 = tf(freq=2.0), with freq of:
 *           2.0 = termFreq=2.0
 *         4.312207 = idf(docFreq=5674, maxDocs=155754)
 *         0.5 = fieldNorm(doc=80137)
 * </pre>
 */
@Component(property = {
        "sling.servlet.resourceTypes=" + ExplainScoreServlet.RT_SQL2SCORER,
        "sling.servlet.methods=POST",
        "sling.servlet.selectors=sql2scorer",
        "sling.servlet.extensions=json"
}, service = Servlet.class)
public class ExplainScoreServlet extends SlingAllMethodsServlet {
    static final String RT_SQL2SCORER = "acs-commons/components/utilities/sql2scorer";
    static final String P_STATEMENT = "statement";
    static final String P_LIMIT = "limit";
    static final String P_OFFSET = "offset";
    static final String KEY_STMT = "stmt";
    static final String KEY_PLAN = "plan";
    static final String KEY_COLS = "cols";
    static final String KEY_ROWS = "rows";
    static final String KEY_ERROR = "error";

    @Override
    protected void doPost(@NotNull final SlingHttpServletRequest request,
                          @NotNull final SlingHttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json;charset=utf-8");

        final long limit = ofNullable(request.getParameter(P_LIMIT))
                .map(Long::valueOf).orElse(10L);
        final long offset = ofNullable(request.getParameter(P_OFFSET))
                .map(Long::valueOf).orElse(0L);

        // require a query statement
        final String rawStatement = request.getParameter(P_STATEMENT);
        if (rawStatement == null) {
            JsonObject obj = new JsonObject();
            obj.addProperty(KEY_ERROR, "please submit a valid JCR-SQL2 query in the `statement` parameter.");
            response.getWriter().write(obj.toString());
            return;
        }

        try {
            final Session session = request.getResourceResolver().adaptTo(Session.class);
            if (session == null || !session.isLive()) {
                throw new RepositoryException("failed to get a live JCR session from the request");
            }

            final QueryManager qm = session.getWorkspace().getQueryManager();

            // validate the syntax of the raw statement.
            Query rawQuery = qm.createQuery(rawStatement, Query.JCR_SQL2);
            rawQuery.setLimit(1L);
            rawQuery.execute();

            // we can then expect the first SELECT to exist and be the instance we want to replace. (case-insensitive)
            final String statement = rawStatement.replaceFirst("(?i)SELECT",
                    "SELECT [oak:scoreExplanation],");

            // create a new query using our enhanced statement
            final Query query = qm.createQuery(statement, Query.JCR_SQL2);

            // set limit and offset
            query.setLimit(limit);
            query.setOffset(offset);

            // prepare Gson with QueryExecutingTypeAdapter
            Gson json = new GsonBuilder().registerTypeHierarchyAdapter(Query.class,
                    new QueryExecutingTypeAdapter(qm)).create();

            // execute the query and write the response.
            response.getWriter().write(json.toJson(query));
        } catch (final InvalidQueryException e) {
            JsonObject obj = new JsonObject();
            obj.addProperty(KEY_ERROR, "please submit a valid JCR-SQL2 query in the `statement` parameter: "
                    + e.getMessage());
            response.getWriter().write(obj.toString());
        } catch (final RepositoryException e) {
            JsonObject obj = new JsonObject();
            obj.addProperty(KEY_ERROR, e.getMessage());
            response.getWriter().write(obj.toString());
        }
    }

    class QueryExecutingTypeAdapter extends TypeAdapter<Query> {
        final QueryManager qm;

        QueryExecutingTypeAdapter(final QueryManager qm) {
            this.qm = qm;
        }

        @Override
        public void write(final JsonWriter jsonWriter, final Query query) throws IOException {
            try {
                final QueryResult queryResult = query.execute();
                final Query explainQuery = qm.createQuery("explain " + query.getStatement(),
                        Query.JCR_SQL2);
                final QueryResult explainResult = explainQuery.execute();
                final Optional<Row> planRow = ofNullable(explainResult.getRows())
                        .filter(RowIterator::hasNext).map(RowIterator::nextRow);

                jsonWriter.beginObject();
                jsonWriter.name(KEY_STMT).value(query.getStatement());
                if (planRow.isPresent()) {
                    jsonWriter.name(KEY_PLAN).value(planRow.get().getValue("plan").getString());
                }
                jsonWriter.name(KEY_COLS);
                jsonWriter.beginArray();
                for (String column : queryResult.getColumnNames()) {
                    jsonWriter.value(column);
                }
                jsonWriter.endArray();
                jsonWriter.name(KEY_ROWS);
                jsonWriter.beginArray();
                for (RowIterator rowIt = queryResult.getRows(); rowIt.hasNext(); ) {
                    final Row row = rowIt.nextRow();
                    jsonWriter.beginArray();
                    for (Value value : row.getValues()) {
                        writeValue(jsonWriter, value);
                    }
                    jsonWriter.endArray();
                }
                jsonWriter.endArray();
                jsonWriter.endObject();
            } catch (final RepositoryException e) {
                throw new IOException(e);
            }
        }

        @Override
        public Query read(final JsonReader jsonReader) {
            throw new UnsupportedOperationException("not implemented");
        }

        void writeValue(JsonWriter writer, Value v) throws IOException, RepositoryException {
            if (v == null) {
                writer.nullValue();
            } else {
                switch (v.getType()) {
                    case PropertyType.BINARY:
                        writer.value("(binary value)");
                        break;
                    case PropertyType.BOOLEAN:
                        writer.value(v.getBoolean());
                        break;
                    case PropertyType.DATE:
                        writer.value(ISO8601.format(v.getDate()));
                        break;
                    case PropertyType.LONG:
                        writer.value(v.getLong());
                        break;
                    case PropertyType.DECIMAL:
                    case PropertyType.DOUBLE:
                        writer.value(v.getDecimal().toPlainString());
                        break;
                    default:
                        writer.value(v.getString());
                }
            }
        }
    }
}
