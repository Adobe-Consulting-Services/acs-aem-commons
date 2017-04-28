
pageCompareApp
.directive('detailsOpener', function() {
    return {
        restrict: 'AE',
        replace: 'true',
        transclude: false,
        compile: function(element, attrs) {
            $(element).click(function() {
                var id = $(element).attr('id');
                var right = $('#' + id.replace('left', 'right') + '-detail');
                var left = $('#' + id.replace('right', 'left') + '-detail');
                if (left.is(":visible") === true) {
                    left.slideUp();
                    right.slideUp();
                } else {
                    var height = right.height() > left.height() ? right.height() : left.height();
                    left.height(height);
                    right.height(height);
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