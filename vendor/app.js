// hackity-hack

$(document).ready(function() {
    var ft = $("#floating-toc");
    var ul = ft.find('ul');
    var lis = ft.find('li');

    ul.css('maring', '0px');

    var liHeight = $(lis.get(0)).height();

    ft.css('height', (liHeight) + 'px');


    showNs = function(ns) {
        //this is killing performance, lookup table.
        //var el = $("[id='floating-toc_" + ns + "']")
        //var index = lis.index(el)

        var index = 0;

        for(i in nsPositions.nss) {
            if(ns == nsPositions.nss[i]) 
		index = i;
        }

        console.log(index);

        if(index == lastNsIndex) return;

        lastNsIndex = index;


        ul.animate({marginTop: (-1 * liHeight * index) + 'px'}, 300);
    };

    var calcNsPositions = function() {
        var nss = [];
        var anchors = [];
        var positions = [];

        $.each(lis, function(i, el) {
            var ns = $(el).attr('id').split('_')[1];
            nss.push(ns);
            var a = $("a[name='"+ns+"']");
            anchors.push(a);
            positions.push(a.offset().top);
            console.log(a.offset().top);
        });

        return {nss: nss, positions: positions};
    };

    var nsPositions = calcNsPositions();

    console.log(nsPositions);

    var lastNsIndex = -1;

    var $window = $(window);

    var currentSection = function(nsp) {
	console.log(nsp);
        var ps = nsp.positions;
        var nss = nsp.nss;
        var scroll = $window.scrollTop() + 300;
        var nsIndex = -1;

        for(var i in ps) {
            var p = ps[i];

            if(p >= scroll) {
                nsIndex = i-1;
                break;
            }
                
        }

        if(nsIndex == -1 && scroll >= ps[0]) {
            nsIndex = ps.length-1;
        }

        if(nsIndex == -1) nsIndex = 0;

        return nss[nsIndex];
    };

    $(window).scroll(function(e) {
        showNs(currentSection(nsPositions));
    });

    ul.css('margin-top', '0px');
});
