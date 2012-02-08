SyntaxHighlighter.defaults['gutter'] = false;
SyntaxHighlighter.all();

// hackity hack
$(window).load(function() {
    var ft = $("#floating-toc");
    var ul = ft.find('ul');
    var lis = ft.find('li');
    var liHeight = $(lis.first()).height();

    ul.css('margin', '0px');
    ft.css('height', liHeight + 'px');

    showNs = function(ns) {
        var index = 0;

        for(i in nsPositions.nss) {
            if(ns == nsPositions.nss[i]) index = i;
        }

        if(index != lastNsIndex) {
            lastNsIndex = index;
            ul.animate({marginTop: (-1 * liHeight * index) + 'px'},
               300);
        }

    }

    var calcNsPositions = function() {
        var hheight = $('.docs-header').first().height();
        var nss = [];
        var anchors = [];
        var positions = [];
        $.each(lis, function(i, el) {
            var ns = $(el).attr('id').split('_')[1];
            nss.push(ns);
            var a = $("a[name='"+ns+"']");
            anchors.push(a);
            positions.push(a.offset().top - hheight);
            // console.log(a.offset().top)
        });

        return {nss: nss, positions: positions};
    }

    var nsPositions = calcNsPositions();
    // console.log(nsPositions)
    var lastNsIndex = -1;
    var $window = $(window);

    var currentSection = function(nsp) {
        var ps = nsp.positions;
        var scroll = $window.scrollTop();
        var nsIndex = -1;

        for(var i = 0, length = ps.length; i < length; i++) {
            if(ps[i] >= scroll) {
                nsIndex = i-1;
                break;
            }
        }

        if(nsIndex == -1) {
             if(scroll >= ps[0]) {
                 nsIndex = ps.length - 1;
             } else {
                 nsIndex = 0;
             }
        }

        return nsp.nss[nsIndex];
    }

    $(window).scroll(function(e) {
        showNs(currentSection(nsPositions));
    });
});
