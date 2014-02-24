---
layout: feature
title: DAM workflow processes
description: DAM custom workflow processes!
date: 2014-02-19 15:39:29
thumbnail: /images/dam-workflow-processes/thumbnail.png
categories: features
tags: new
initial-release:
---



## Abstract Rendition Modifying Process

### Purpose:
Abstract asset workflow which performs some action on a particular rendition (which was presumably created by an earlier workflow process).

### How to use:
* Install the ACS AEM Commons package
* Extend the com.adobe.acs.commons.dam.AbstractAssetWorkflowProcess


## Add Watermark to a rendition

### Purpose:
This process adds the watermark to the rendition.

### How to Use
Update the DAM Update Asset workflow to add a custom process step in the end.

Path to the workflow: /etc/workflow/models/dam/update_asset.html

*   	Open the DAM Update Asset workflow
* 	At the end insert a new Process step, Workflow/Process Step

![image]({{ site.baseurl }}/images/dam-workflow-processes/1.png)

*  	Edit the Process Step
	* Title: Add watermark to image
	* On the Process tab, select “Add Watermark to Rendition” from the Process drop down

	![image]({{ site.baseurl }}/images/dam-workflow-processes/2.png)

	* Check the Handler Advance option
	* Two arguments are required
		1. renditionName: The name of the rendition to modify like “original”
		2. watermark: The repository path of the watermark like “/content/dam/geometrixx/icons/target.png/jcr:content/renditions/original”

	![image]({{ site.baseurl }}/images/dam-workflow-processes/3.png)

        * A full configuration example might have all the following:
           watermark:/content/dam/geometrixx/icons/draft.png/jcr:content/renditions/original,renditionName:cq5dam.web.1280.1280

*  Click OK and make sure to click “Save” of the workflow.


