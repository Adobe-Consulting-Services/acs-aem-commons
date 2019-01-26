/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

package com.adobe.acs.commons.workflow.bulk.removal.impl;

import com.adobe.acs.commons.util.InfoWriter;
import com.adobe.acs.commons.workflow.bulk.removal.WorkflowInstanceRemover;
import com.adobe.acs.commons.workflow.bulk.removal.WorkflowRemovalException;
import com.adobe.acs.commons.workflow.bulk.removal.WorkflowRemovalForceQuitException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component(
        factory = "WorkflowInstanceRemoverScheduler",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = Runnable.class,
        property = {
                "scheduler.concurrent" + "=" + "false",
                "webconsole.configurationFactory.nameHint" + "=" + "Runs at '{scheduler.expression}' on models [{workflow.models}] with status [{workflow.statuses}]"
        }
)
@Designate(ocd = WorkflowInstanceRemoverScheduler.Config.class)
public class WorkflowInstanceRemoverScheduler implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(WorkflowInstanceRemoverScheduler.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private WorkflowInstanceRemover workflowInstanceRemover;

    private static final String SERVICE_NAME = "workflow-remover";
    private static final Map<String, Object> AUTH_INFO;

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    private List<String> statuses = new ArrayList<String>();

    @ObjectClassDefinition(
            name = "ACS AEM Commons - Workflow Instance Remover - Scheduled Service"
    )
    public @interface Config {

        String DEFAULT_SCHEDULER_EXPRESSION = "0 1 0 ? * *";
        String STATUS_COMPLETED = "COMPLETED";
        String STATUS_ABORTED = "ABORTED";

        @AttributeDefinition(
                name = "Cron expression defining when this Scheduled Service will run",
                description = "[12:01am daily = 0 1 0 ? * *]; see www.cronmaker.com",
                defaultValue = DEFAULT_SCHEDULER_EXPRESSION
        )
        String scheduler_expression() default DEFAULT_SCHEDULER_EXPRESSION;

        @AttributeDefinition(
                name = "Workflow Status",
                description = "Only remove Workflow Instances that have one of these statuses.",
                defaultValue = {STATUS_COMPLETED, STATUS_ABORTED}
        )
        String[] workflow_statuses() default {STATUS_COMPLETED, STATUS_ABORTED};

        @AttributeDefinition(
                name = "Workflow Models",
                description = "Only remove Workflow Instances that belong to one of these WF Models.",
                cardinality = Integer.MAX_VALUE
        )
        String[] workflow_models();

        @AttributeDefinition(
                name = "Payload Patterns",
                description = "Only remove Workflow Instances whose payloads match one of these regex patterns",
                cardinality = Integer.MAX_VALUE
        )
        String[] workflow_payloads();

        @AttributeDefinition(
                name = "Older Than UTC Timestamp",
                description = "Only remove Workflow Instances whose payloads are older than this UTC Time in Milliseconds"
        )
        long workflow_older$_$than();

        @AttributeDefinition(
                name = "Batch Size",
                description = "Save removals to JCR in batches of this defined size.",
                defaultValue = "" + DEFAULT_BATCH_SIZE
        )
        int batch$_$size() default DEFAULT_BATCH_SIZE;

        @AttributeDefinition(
                name = "Max duration (in minutes)",
                description = "Max number of minutes this workflow removal process can execute. 0 for no limit. "
                        + "[ Default: 0 ]",
                defaultValue = "" + DEFAULT_MAX_DURATION
        )
        int max$_$duration() default DEFAULT_MAX_DURATION;
    }

    private List<String> models = new ArrayList<String>();

    private List<Pattern> payloads = new ArrayList<Pattern>();

    private Calendar olderThan = null;

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private int batchSize = DEFAULT_BATCH_SIZE;

    private static final int DEFAULT_MAX_DURATION = 0;
    private int maxDuration = DEFAULT_MAX_DURATION;

    @Override
    @SuppressWarnings("squid:S2142")
    public final void run() {

        try (ResourceResolver adminResourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {

            final long start = System.currentTimeMillis();

            int count = workflowInstanceRemover.removeWorkflowInstances(
                    adminResourceResolver,
                    models,
                    statuses,
                    payloads,
                    olderThan,
                    batchSize,
                    maxDuration);

            if (log.isInfoEnabled()) {
                log.info("Removed [ {} ] Workflow instances in {} ms", count, System.currentTimeMillis() - start);
            }

        } catch (LoginException e) {
            log.error("Login Exception when getting admin resource resolver", e);
        } catch (PersistenceException e) {
            log.error("Persistence Exception when saving Workflow Instances removal", e);
        } catch (WorkflowRemovalException e) {
            log.error("Workflow Removal exception during Workflow Removal", e);
        } catch (InterruptedException e) {
            log.error("Interrupted Exception during Workflow Removal", e);
        } catch (WorkflowRemovalForceQuitException e) {
            log.info("Workflow Removal force quit", e);
        }
    }

    private List<String> arrayToList(String[] array) {
        List<String> list = new ArrayList<String>();

        for (String element : array) {
            if (StringUtils.isNotBlank(element)) {
                list.add(element);
            }
        }

        return list;
    }

    @Activate
    protected final void activate(WorkflowInstanceRemoverScheduler.Config config) {

        statuses = arrayToList(config.workflow_statuses());

        models = arrayToList(config.workflow_models());

        final String[] payloadsArray = config.workflow_payloads();
        for (final String payload : payloadsArray) {
            if (StringUtils.isNotBlank(payload)) {
                final Pattern p = Pattern.compile(payload);
                if (p != null) {
                    payloads.add(p);
                }
            }
        }

        final Long olderThanTs = config.workflow_older$_$than();

        if (olderThanTs > 0) {
            olderThan = Calendar.getInstance();
            olderThan.setTimeInMillis(olderThanTs);
        }

        batchSize = config.batch$_$size();
        if (batchSize < 1) {
            batchSize = DEFAULT_BATCH_SIZE;
        }

        maxDuration = config.max$_$duration();

        final InfoWriter iw = new InfoWriter();
        iw.title("Workflow Instance Removal Configuration");
        iw.message("Workflow status: {}", statuses);
        iw.message("Workflow models: {}", models);
        iw.message("Payloads: {}", Arrays.asList(payloadsArray));
        iw.message("Older than: {}", olderThan);
        iw.message("Batch size: {}", batchSize);
        iw.message("Max Duration (minutes): {}", maxDuration);
        iw.end();

        log.info(iw.toString());
    }

    @Deactivate
    protected final void deactivate(final Map<String, String> config) {
        olderThan = null;
        statuses = new ArrayList<String>();
        models = new ArrayList<String>();
        payloads = new ArrayList<Pattern>();
        batchSize = DEFAULT_BATCH_SIZE;
        maxDuration = DEFAULT_MAX_DURATION;
    }
}
