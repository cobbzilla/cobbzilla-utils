var dmp = new diff_match_patch();

function diff(text1, text2, opts) {
    dmp.Diff_Timeout = parseFloat(opts['timeout']);
    dmp.Diff_EditCost = parseFloat(opts['editCost']);

    var ms_start = (new Date()).getTime();
    var d = dmp.diff_main(text1, text2);
    var ms_end = (new Date()).getTime();

    if (opts['semantic']) {
        dmp.diff_cleanupSemantic(d);
    }
    if (opts['efficiency']) {
        dmp.diff_cleanupEfficiency(d);
    }
    return dmp.diff_prettyHtml(d);
    // return dmp.diff_text1(d);
    //+ '<BR>Time: ' + (ms_end - ms_start) / 1000 + 's';
}

diff(text1, text2, opts);