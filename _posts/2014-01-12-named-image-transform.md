---
layout: feature
title: Named Transform Image Servlet
description: Consistently resize, crop and transform images
date: 2014-01-12 01:00:00
thumbnail: /images/named-image-transform/thumbnail.png
categories: features
initial-release: 1.5.0
tags: new
---

# Purpose

Many web site designs demands consistency of images based on their use within components. For example, a panoramic spotlight may demand the image be 960 x 400, and bio picture must be 100 x 100 and greyscale. 

The ACS AEM Commons Named Transform Image Servlet allows specific image transforms to be defined centrally via OSGi configurations. These image transforms can be easily invoked via parameterized HTTP GET requests to image resources in AEM.

## Example

The below DAM Asset image has been resizes, rotated, cropped and greyscaled as defined by the custom defined `my-transform-name` transform rule set.

`http://localhost:4502/content/dam/geometrixx/shapes/sq_plan.png.transform/my-transform-name/image.png`

![image]({{ site.baseurl }}/images/named-image-transform/my-transform-name-example.png)

( Original image on left. Transformed image on right. )

Supporting OSGi Configuration

![image]({{ site.baseurl }}/images/named-image-transform/my-transform-name-osgi-config.png)



## Supported "image-y" resources

Almost any "image-like" resource can be requested using the named transform URI parameterization, and the underlying image will be derived and rendered using the transformation parameters.

* Pages (`cq:Page`s)

	* HTTP GET `/content/acme/article.transform/feature/image.png`

* Image component resources

	* HTTP GET `/content/acme/article/_jcr_content/image.transform/feature/image.png`

* DAM Assets (`dam:Asset`)

	* HTTP GET `/content/dam/images/dog.jpg.transform/feature/image.jpg`

* DAM Asset Renditions

	* HTTP GET `/content/dam/images/dog.jpg/jcr:content/renditions/thumbnail.jpg.transform/feature/image.jpg`

 * "Raw" Binary Images (`nt:file` or `nt:resource`)

	* HTTP GET `/etc/designs/acme/images/cat.png.transform/feature/image.jpg`



# How to Use

* Define any number of `sling:OsgiConfig`'s, each representing a different named transform

`/apps/mysite/config/com.adobe.acs.commons.images.impl.NamedImageTransformerImpl-myTransformName.xml`

{% highlight xml %}
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:cq="http://www.day.com/jcr/cq/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    jcr:primaryType="sling:OsgiConfig"
    name="my-transform"
    transforms="[resize:width=400,rotate:degrees=10,]"/>
{% endhighlight %}


* Get the URI to a supported resource (see above) to transform
* add the `.transform` extension (append this even if the resource is named with an extension; like a DAM asset)
* Add an initial suffix segment that matches your transform name (Ex. `/my-transform`)
* Add a final suffix segment of `/image.<image-format-extension>` OR `/img.<image-format-extension>`

Resulting in 

* `<img src="/content/mysite/article/_jcr_content/image.transform/my-transform/image.png"/>`

or 

* `<img src="/content/dam/images/dog.jpg.transform/my-transform/img.png"/>`


## OOTB Image Transformers

### Greyscale

Converts the image to greyscale.

Name

* `greyscale`

Params

* `None`

Example

* `greyscale`



### Resize

Resizes the image to the specified width and/or height. If only 

Name

* resize

Params

* `width=[width in px]`
* `height=[height in px]`

Example

* `resize:width=200`
* `resize:height=300`
* `resize:width=400&height=400`


### Rotate

Rotates the image.

Name

* `rotate`

Params

* `degrees=[degrees to rotate]`

Example

* `rotate:degrees=180`

### Crop

Crops the image based on 

Name

* `crop`

Params

* `bounds=[x,y,width,height]`
* `smart=[boolean]` Defaults to true. Smart bounding will attempt to shift the specified crop-zone to fit within the image dimensions if the crop-zone falls outside the images dimensions. 


Example

* `rotate:degrees=180`


### Adjust Brightness/Contrast

Adjusts the brightness and contrast of the image.

Name

* `adjust`

Params

* `brightness=[-255 .. 255]` (dark to light)
* `contrast=[positive float]` (1.0 does not change contrast. < 1.0 lower contrast. > 1.0 enhance contrast.)

Example

* `adjust:brightness=120&contrast=0`


## Notes

* **ORDER MATTERS** when defining your image transformation rules. For example, a resize then crop can yield significantly different results than a crop then resize.

