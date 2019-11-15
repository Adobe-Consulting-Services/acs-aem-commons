/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

$(function() {
    $('body').on('click', '#packager-form input[type=submit]', function(e) {

        var $this = $(this),
            $form = $this.closest('form'),
            json,
            i;

        $('.notification').fadeOut();

        $.post($form.attr('action'), {'preview': $this.attr('name') === 'preview' }, function(data) {
            $('.notification').hide();

            if(data.status === 'preview') {
                /* Preview */

                $('.notification.preview .filters').html('');
                if(!data.filterSets) {
                    $('.notification.preview .filters').append('<li>No matching resources found.</li>');
                } else {
                    for(i = 0; i < data.filterSets.length; i++) {
                        $('.notification.preview .filters').append('<li>' + data.filterSets[i].rootPath + '</li>');
                    }
                }

                $('.notification.preview').fadeIn();
            } else if(data.status === 'success') {
                /* Success */

                $('.notification.success .package-path').text(data.path);
                $('.notification.success .package-manager-link').attr('href', '/crx/packmgr/index.jsp#' + data.path);

                $('.notification.success .filters').html('');

                if(!data.filterSets) {
                    $('.notification.preview .filters').append('<li>No matching resources found.</li>');
                } else {
                    for(i = 0; i < data.filterSets.length; i++) {
                        $('.notification.success .filters').append('<li>' + data.filterSets[i].rootPath + '</li>');
                    }
                }

                $('.notification.success').fadeIn();
            } else {
                /* Error */
                $('.notification.error .msg').text(data.msg || '');
                $('.notification.error').fadeIn();
            }

            $('html, body').animate({
                scrollTop: $('.notifications').offset().top - 20
            }, 500);
        }, 'json');

        e.preventDefault();
        //return false;
    });
});
