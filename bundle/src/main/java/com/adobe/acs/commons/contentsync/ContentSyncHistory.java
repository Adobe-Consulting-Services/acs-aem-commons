package com.adobe.acs.commons.contentsync;

import com.adobe.granite.security.user.util.AuthorizableUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import static com.adobe.acs.commons.contentsync.ContentSyncJobConsumer.JOB_TOPIC;
import static org.apache.sling.event.jobs.Job.PROPERTY_FINISHED_DATE;

@Model(adaptables = SlingHttpServletRequest.class)
public class ContentSyncHistory {
    @SlingObject
    private SlingHttpServletRequest request;

    @OSGiService
    private JobManager jobManager;

    public Collection<Item> getJobs(){
        Collection<Job> active = jobManager.findJobs(JobManager.QueryType.ACTIVE, JOB_TOPIC, 100, null);
        Collection<Job> history = jobManager.findJobs(JobManager.QueryType.HISTORY, JOB_TOPIC, 100, null);
        Collection<Job> all = new ArrayList<>();
        all.addAll(active);
        all.addAll(history);

        return all.stream()
                .sorted((j1, j2) -> j2.getCreated().compareTo(j1.getCreated()))
                .map(Item::new).collect(Collectors.toList());
    }

    public class  Item extends LinkedHashMap  {
        private Job job;

        Item(Job job){
            this.job = job;
            for(String key : job.getPropertyNames()){
                put(key, job.getProperty(key));
            }
        }

        public String getJobState(){
            return job.getJobState().toString();
        }

        public int getProgress(){
            if(containsKey("slingevent:finishedState")) return 100;

            int progressStep = (int)getOrDefault(Job.PROPERTY_JOB_PROGRESS_STEP, 0);
            int progressSteps = (int)getOrDefault(Job.PROPERTY_JOB_PROGRESS_STEPS, 0);
            return progressSteps == 0 ? 0 : (100 * progressStep) / progressSteps ;
        }

        public String getStatusColor(){
            String color;
            switch (job.getJobState()){
                case ACTIVE:
                    color = "yellow";
                    break;
                case SUCCEEDED:
                    color = "blue";
                    break;
                default:
                    color = "red";
                    break;
            }
            return color;
        }

        public boolean isDrynRun(){
            return containsKey("dryRun");
        }

        public String getProgressMessage(){
            if(getNumberOfSteps() == 0 || getCurrentStep() == 0){
                return "loading";
            }
            return String.format("%d of %d", getCurrentStep(), getNumberOfSteps());
        }

        public int getNumberOfSteps(){
            return (int)getOrDefault("slingevent:progressSteps", 0);
        }

        public int getCurrentStep(){
            return (int)getOrDefault("slingevent:progressStep", 0);
        }

        public String getStartedBy() {
            return AuthorizableUtil.getFormattedName(request.getResourceResolver(), (String)get("cq:startedBy"));
        }
    }
}
