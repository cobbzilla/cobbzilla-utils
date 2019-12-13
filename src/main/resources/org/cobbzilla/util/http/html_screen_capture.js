// inspired by https://github.com/pofider/phantom-html-to-pdf/pull/16#issuecomment-155020653

var page = require('webpage').create();
var fs = require('fs');
var url = '@@URL@@';
var outFile = '@@FILE@@';

function checkExists (path, start, timeout) {
    if (fs.exists(path)) return true;
    if (Date.now() - start > timeout) {
        console.log('checkExists: timeout waiting for '+path);
        phantom.exit(2);
    }
    window.setTimeout(function () {
        if (checkExists(path, start, timeout)) {
            phantom.exit(0);
        }
    }, 250);
}

// @see https://github.com/ariya/phantomjs/issues/12685
// @see https://github.com/ariya/phantomjs/issues/12936
// the 2 lines below essentially hack phantomjs into outputting the right size PDF for US 8.5x11in paper when run on
// a linux machine. It has to do with the DPI of some underlying software, perhaps there is a better way to make the adjustment.
// or we should allow caller to name a saved preset config. then we could add support for A4 paper/etc.
page.zoomFactor = 1.25;
page.paperSize = { width: '615px', height: '790px', margin: '30px' };

page.open(url, function(status) {
  if (status !== 'success') {
    console.log('Error loading ('+status+'): '+url);
    phantom.exit(1);

  } else {
    page.render(outFile);
  }
});
