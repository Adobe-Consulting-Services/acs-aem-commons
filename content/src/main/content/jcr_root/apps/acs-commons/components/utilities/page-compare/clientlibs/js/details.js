
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
            $(element).click(function() {
                if (height < 0) {
                    left.show();
                    right.show();
                    height = right.height() > left.height() ? right.height() : left.height();
                    left.hide();
                    right.hide();
                    left.height(height);
                    right.height(height);
                }
                if (left.is(":visible") === true) {
                    left.slideUp();
                    right.slideUp();
                } else {
                    left.slideDown();
                    right.slideDown();
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