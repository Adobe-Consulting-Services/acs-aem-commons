
pageCompareApp
.directive('legend', function() {
    return {
        restrict: 'AE',
        replace: 'true',
        transclude: false,
        compile: function(element, attrs) {
            $(element).click(function() {
                var type = attrs.legend;
                if ($(element).css('opacity') == 1) {
                    $(element).fadeTo(200 , 0.3);
                    $('.elem-' + type).fadeOut();
                } else {
                    $(element).fadeTo(200 , 1);
                    $('.elem-' + type).fadeIn();
                }
            });
        }
    };
});