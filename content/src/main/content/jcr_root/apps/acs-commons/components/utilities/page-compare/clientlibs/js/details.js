
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