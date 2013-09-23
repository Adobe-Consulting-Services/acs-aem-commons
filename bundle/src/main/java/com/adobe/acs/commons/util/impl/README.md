# Overlay Servlet

## Purpose

Avoid building out overlay content tree to provide overlay behavior.

### Use Cases

* Moving resource type implementations without modifying all content sling:resourceType properties
* Overlaying OOTB `/libs` behavior without creating overlay content trees under /apps not specific to your organization/project (ex. `/apps/foundation` or `/apps/cq`)


This Servlet Factory allows "Source resource types" to be mapped to a "Target resource type", effectively providing
an "in code" overlay of the "Source resource type".


## Implementation

The Overlay Servlet uses Sling Servlets' natural resource-based mapping to capture requests for the "Source" resource type,
and performs an internal forward forcing the resource type of the "Target" resource type via Request Dispatcher Options.


## Sling OSGi Configuration Example

    <?xml version="1.0" encoding="UTF-8"?>
    <jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:cq="http://www.day.com/jcr/cq/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
        jcr:primaryType="sling:osgiConfig">
        sling.servlet.resourceTypes="wcm/core/components/designer"
        sling.servlet.selectors=""
        sling.servlet.extensions="html"
        sling.servlet.methods="GET"
        prop.target-resource-type="acs-commons/components/utilities/designer"/>

