/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2013 Adobe
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

/*
 Based on http://jonraasch.com/blog/a-simple-jquery-slideshow
*/

$(function() {
    var slideSwitch = function() {
        var shows = $('.acs-commons-simple-slideshow');

        if(!shows || (shows.length === 0) ){
            return;
        }

        $.each(shows, function(index, show){
            var showImagesActive = $(show).find('img.active'), nextImage;

            if ( showImagesActive.length === 0 ){
                showImagesActive = $(show).find('img:last');
            }

            nextImage =  showImagesActive.next().length ? showImagesActive.next()
                                : $(show).find('img:first');

            showImagesActive.addClass('last-active');

            nextImage.css({ opacity: 0.0 } ).addClass('active')
                .animate({opacity: 1.0}, 1000, function() {
                    showImagesActive.removeClass('active last-active');
                });
        });
    };
    setInterval(slideSwitch, 2000);
});
