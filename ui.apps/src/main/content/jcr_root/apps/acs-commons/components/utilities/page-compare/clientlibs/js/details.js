/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2013 - 2018 Adobe
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

pageCompareApp
.directive('detailsOpener', function($timeout) {
    return {
        restrict: 'AE',
        replace: 'true',
        transclude: false,
        compile: function(element, attrs) {
            var id = $(element).attr('id');
            var right = $('#' + id.replace('left', 'right') + '-detail');
            var left = $('#' + id.replace('right', 'left') + '-detail');
            left.hide();
            right.hide();
            var height = -1;

            var init = function() {
                if (left.hasClass('initiatlized')) {
                    return;
                }
                left.addClass('initiatlized');
                compare();
                left.show();
                right.show();
                height = right.height() > left.height() ? right.height() : left.height();
                left.hide();
                right.hide();
                left.height(height);
                right.height(height);
            };

            var compare = function() {
                var rightText = $('.inner', right).text().trim();
                var leftText = $('.inner', left).text().trim();

                var d = new diff();
                var newRigthText = d.prettyHtml(d.main(leftText, rightText));
                var newLeftText = d.prettyHtml(d.main(rightText, leftText));

                $('.inner', left).html(newLeftText);
                $('.inner', right).html(newRigthText);
            };

            var open = function() {
                left.slideDown();
                right.slideDown();
            };

            var close = function() {
                left.slideUp();
                right.slideUp();
            };

            $(element).click(function() {
                init();
                if (left.is(":visible") === true) {
                    close();
                } else {
                    open();
                }
            });
        }
    };

})
.directive('details', function() {
    return {
        restrict: 'AE',
        replace: 'true',
        transclude: false,
        compile: function(element, attrs) {
            $(element).click(function(e) {
                e.stopPropagation();
            });
        }
    };

});