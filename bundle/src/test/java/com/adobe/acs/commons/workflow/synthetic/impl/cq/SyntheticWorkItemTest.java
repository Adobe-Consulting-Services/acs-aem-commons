package com.adobe.acs.commons.workflow.synthetic.impl.cq;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SyntheticWorkItemTest {
	@Mock
	SyntheticWorkflow syntheticWorkflow;

	@Test
	public void test_getMetaData() throws Exception {
		SyntheticWorkItem syntheticWorkItem = SyntheticWorkItem.createSyntheticWorkItem(syntheticWorkflow.getWorkflowData());
		Assert.assertNotNull(syntheticWorkItem.getMetaData());
	}
}

