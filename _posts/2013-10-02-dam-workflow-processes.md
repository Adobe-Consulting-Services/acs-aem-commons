---
layout: feature
title: DAM Workflow Processes
description: Commonly required DAM workflow processes.
date: 2013-10-02 15:39:29
thumbnail: /images/dam-workflow-processes/thumbnail.png
categories: features
initial-release: 1.0
---

## Abstract Rendition Modifying Process

### Purpose:
Abstract asset workflow which performs some action on a particular rendition (which was presumably created by an earlier workflow process).

### How to use:
1. Install the ACS AEM Commons package
2. Add the ACS AEM Commons bundle as a dependency to your project
3. Extend the `com.adobe.acs.commons.dam.AbstractRenditionModifyingProcess` and implement the abstract methods.

## Rendition Watermarker

### Purpose:
This process overlays the named watermark image onto the named rendition in the lower left corner.  Currently, only one position is supported (lower left).  Future enhancements may include the ability to add custom text (e.g. "Private"), text/image rotation, opacity, flexible image placement (i.e. top-left, bottom-right, top-right, center) and exact placement.

### How to Use
1. Store your watermark file in CQ. This can either be a DAM asset or just an `nt:file` node.
2. Update the DAM Update Asset workflow (`/etc/workflow/models/dam/update_asset`) and add a custom process step at any point after the rendition you want to watermark has been generated.
    1. Open the DAM Update Asset workflow
    2. Insert a new Process step, Workflow/Process Step ![Workflow Component List]({{ site.baseurl }}/images/dam-workflow-processes/1.png)
    3. Edit the Process Step
        1. Title: Add watermark to image
        2. On the Process tab, select “Add Watermark to Rendition” from the Process drop down. ![Process Drop Down]({{ site.baseurl }}/images/dam-workflow-processes/2.png)
        3. Check the Handler Advance option
        4. Two arguments are required
            1. `renditionName`: The name of the rendition to modify, e.g. "cq5dam.web.1280.1280"
            2. `watermark`: The repository path of the watermark. If this is a simple `nt:file` node, it is just the path to the node. If it is a DAM asset, it is the path to the original rendition, e.g. `/content/dam/geometrixx/icons/target.png/jcr:content/renditions/original`. ![Arguments]({{ site.baseurl }}/images/dam-workflow-processes/3.png)
    4. Click OK and then “Save” the workflow.

A full configuration example might have all the following:  
> watermark:/content/dam/geometrixx/icons/draft.png/jcr:content/renditions/original,renditionName:cq5dam.web.1280.1280

This places the `draft.png` image in the lower left of the 1280x1280 rendition of any image file loaded into the DAM.

## Rendition Matter (Added in version 1.2.0)

### Purpose:
This process ensures that an rendition *exactly* matches a set of dimensions by applying a matte. This  is sometimes referred to as letterboxing (when the matte is applied on the top and bottom of the image) or windowboxing (when the matte is applied on the left and right of the Both the horizontal and the vertical position along with the dimensions can be configured via parameters.

### How to Use
Update the DAM Update Asset workflow (`/etc/workflow/models/dam/update_asset`) and add a custom process step at any point after the rendition you want to matte has been generated.

1. Open the DAM Update Asset workflow
2. At the end insert a new Process step, Workflow/Process Step ![Workflow Component List]({{ site.baseurl }}/images/dam-workflow-processes/1.png)
3. Edit the Process Step
    1. Title: Matte Rendition
    2. On the Process tab, select “Matte Rendition” from the Process drop down ![image]({{ site.baseurl }}/images/dam-workflow-processes/select-matte-finish.jpg)
    3. Check the Handler Advance option
    4. Five arguments are required ![Matte Renditions Params]({{ site.baseurl }}/images/dam-workflow-processes/matte-finish-params.jpg)
        1. `bgcolor`: The background color (in hex notation) you want to apply to the rendition
        2. `dimension`: dimensions of the image size
        3. `vpos`: starting vertical position, the options are "top", "bottom", and "middle"
        4. `hpos`: horizontal start position , the options are "left", "right", "center"
        5. `renditionName`: The name of the rendition to modify like "cq5dam.web.1280.1280"
4. Click OK and then “Save” the workflow.

A full configuration example might have all the following:  
> bgcolor:000000,dimension:1280:1280,vpos:top,hpos:left,renditionName:cq5dam.web.1280.1280

This will ensure that the rendition "cq5dam.web.1280.1280" is 1280x1280 pixels, adding black (#000000) pixels to the bottom right where necessary.