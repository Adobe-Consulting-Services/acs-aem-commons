/*
 * Copyright 2018 Adobe.
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
package com.adobe.acs.commons.mcp.impl.processes;

/**
 * Replace folder thumbnails under a user-definable set of circumstances
 * As a business user, I would like an easy way to scan and repair missing thumbnails, or just regenerate all thumbnails under a given tree of the DAM.

Observations:

Thumbnail is under folder/jcr:content/folderThumbnail
Binary is attached to the jcr:data property of that node
Folders have placeholder thumbnails in cases where the thumbnail was generated prior to assets being available, these placeholders are usually under 1kb (observed set were 631 bytes or so)
jcr:lastModified property in the jcr:content might be a useful determining factor if a thumbnail is older than the content of the folder
Haven't figured out a clean way to regenerate the thumbnail but the .folderthumbnail.jpg selector/suffix does it from the browser.
Therefore:

User will want to select to replace thumbnails in a given set of circumstances and have options for those such as: all, outdated, missing, or placeholders.
Probably some combination therein
If the folder thumbnail generation is in a "public" API then it would make sense to call it directly, otherwise an internal Sling Request will have to suffice to trigger that servlet.

Look at /jcr:content/folderThumbnail/jcr:content/@dam:folderThumbnailPaths that list 3 images in that folder.
Confirm if all assets exist
If any are missing, remove that attribute and the thumbnail to trigger regeneration.
 */
public class RefreshFolderTumbnails {
    
}
