
jQuery(function($) {
    function changeSize(by) {
        var newSize = (by === 0) ? '16px' : (parseInt($('html').css('fontSize') || 16, 10)+by)+'px';

        $('html').css('fontSize', newSize);
    }

    // Automatically populate <code /> samples with the appropriate HTML.
    function populateCodeSamples()
    {
        // any <code /> blocks that have 'data-pre-src-id' set to a given element
        $('pre[data-src-id]').each(function()
        {
            // get the html of the source element, based on specified id.
            var elemName = $(this).attr('data-src-id');
            var elem = $("#" + elemName);

          
            // get the html
            var html = elem.html();

            // remove the indent based on the number of spaces prefixing the first line.
            var whitespaceIndentCount = html.search(/[^\s]/);
            var re = new RegExp('^\\s{'+whitespaceIndentCount+'}', 'gm');
            html = html.replace(re, '');

            // hack: sometimes last line has one less space. replace that too. 
            re = new RegExp('^\\s{'+(whitespaceIndentCount-1)+'}', 'gm');
            html = html.replace(re, '');


            // now set the <pre /> elements text.
            $(this).text( html );
        });
    }


    
    populateCodeSamples();

    // Prettify code examples
    prettyPrint();

    // init the rail
    $('#main-rail').rail();
    
 
    // Size changers
    $('#res-plus').on('click', function(evt) {
        changeSize(+8);
    });
 
    $('#res-minus').on('click', function(evt) {
        changeSize(-8);
    });
    
    $('#res-reset').on('click', function(evt) {
        changeSize(0);
    });


    // this adds some magic to the tabs example
    $('.tab-variant').on('click', function() {
        var variant = $(this).data('variant');
        $('#tabsExample').attr('class', variant);
        $('.tabsVariant').html(variant);
    });

    

    
    /**
        Show the paragraph link icon when a heading is hovered on that is within a named section
    */
    (function() {
        var anchor = $('#SectionAnchor');

        // Not linking any H3s in this fashion
        $('h1, h2').on('mouseenter', function(evt) {
            var heading = evt.target;
            var section = heading.parentNode;

            if (section.tagName === 'SECTION' && section.id) {
                anchor.attr('href', '#'+section.id);
                $(heading).append(anchor);
                anchor.show();
            }
        }).on('mouseleave', function(evt) {
            anchor.hide();
        });        
    }());
});
