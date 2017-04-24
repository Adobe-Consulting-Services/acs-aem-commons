package com.adobe.acs.commons.replication.status.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.modules.junit4.PowerMockRunner;

import com.adobe.acs.commons.replication.status.ReplicationStatusManager;
import com.adobe.acs.commons.util.WorkflowHelper;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.metadata.MetaDataMap;

import junitx.util.PrivateAccessor;

@RunWith(PowerMockRunner.class)
public class SetReplicationStatusProcessTest {

	@Mock
	WorkflowHelper workflowHelper;
	
	@Mock
	ReplicationStatusManager replicationStatusManager;
	
	@Mock
	WorkItem workItem;
	
	@Mock
	WorkflowSession workflowSession;
	
	@Mock
	MetaDataMap metadataMap;
	
	@Mock
	ResourceResolver resourceResolver;
	
	@Mock
	WorkflowData workflowData;
	
	@Mock
	Resource payloadResource;
	
	String workflowPayload = "/content/workflow-payload";
	
	@Spy
	SetReplicationStatusProcess setReplicationStatusProcess = new SetReplicationStatusProcess();;
	
	@Before
    public void setUp() throws Exception {		
		when(workflowHelper.getResourceResolver(workflowSession)).thenReturn(resourceResolver);
		
		when(workItem.getWorkflowData()).thenReturn(workflowData);
		when(workflowData.getPayload()).thenReturn(workflowPayload);
		
		when(replicationStatusManager.getReplicationStatusResource(workflowPayload, resourceResolver)).thenReturn(payloadResource);
		when(payloadResource.getPath()).thenReturn(workflowPayload);
		
		PrivateAccessor.setField(setReplicationStatusProcess, "workflowHelper", workflowHelper);
		PrivateAccessor.setField(setReplicationStatusProcess, "replStatusMgr", replicationStatusManager);
	}
	
	@Test
	public void testUpdateStatusWithProvidedParams() throws Exception {
		String workflowParams = "replicationDate=2017-04-21T15:02" + System.lineSeparator();
		workflowParams += "replicatedBy=customUser" + System.lineSeparator();
		workflowParams += "replicationAction=ACTIVATED";
		
		when(metadataMap.get(WorkflowHelper.PROCESS_ARGS, "")).thenReturn(workflowParams);
		
		setReplicationStatusProcess.execute(workItem, workflowSession, metadataMap);
		
		CalendarMatcher calMatch = new CalendarMatcher("2017-04-21T15:02");
		
		verify(replicationStatusManager).setReplicationStatus(any(), eq("customUser"), argThat(calMatch), eq(ReplicationStatusManager.Status.valueOf("ACTIVATED")), eq(workflowPayload));
	}
	
	@Test
	public void testNoReplicationActionDoesNotUpdateStatus() throws Exception {
		String workflowParams = "";
		when(metadataMap.get(WorkflowHelper.PROCESS_ARGS, "")).thenReturn(workflowParams);
		
		setReplicationStatusProcess.execute(workItem, workflowSession, metadataMap);
		
		verify(replicationStatusManager, never()).setReplicationStatus(any(), any(), any(), any(), anyString());
	}

	@Test
	public void testDefaultReplicationUserAndDate() throws Exception {
		String workflowParams = "replicationAction=ACTIVATED";
		when(metadataMap.get(WorkflowHelper.PROCESS_ARGS, "")).thenReturn(workflowParams);
		
		Calendar now = Calendar.getInstance();
		
		setReplicationStatusProcess.execute(workItem, workflowSession, metadataMap);
		
		CalendarMatcher calMatch = new CalendarMatcher(now);
		
		verify(replicationStatusManager).setReplicationStatus(any(), eq("migration"), argThat(calMatch), any(), anyString());
	}

}

class CalendarMatcher extends ArgumentMatcher<Calendar> {

	private Calendar leftCal;
	
	public CalendarMatcher(String date) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.ENGLISH);
		Calendar cal = Calendar.getInstance();
		cal.setTime(sdf.parse(date));
		this.leftCal = cal;
	}
	
	public CalendarMatcher(Calendar cal) throws ParseException {
		this.leftCal = cal;
	}
	
	@Override
	public boolean matches(Object argument) {
		if (argument instanceof Calendar) {
			Calendar rightCal = (Calendar)argument;
			return (leftCal.get(Calendar.YEAR) == rightCal.get(Calendar.YEAR) &&
					leftCal.get(Calendar.MONTH) == rightCal.get(Calendar.MONTH) &&
					leftCal.get(Calendar.DATE) == rightCal.get(Calendar.DATE) &&
					leftCal.get(Calendar.HOUR) == rightCal.get(Calendar.HOUR) &&
					leftCal.get(Calendar.MINUTE) == rightCal.get(Calendar.MINUTE));
		} else {
			return false;
		}
	}
	
}
