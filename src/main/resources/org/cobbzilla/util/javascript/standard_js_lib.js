// returns length of array or collection
function len (thing) {
    if (typeof thing == 'undefined' || thing == null) return 0;
    try {
        return thing.size();
    } catch (err) {
        return thing.length;
    }
}

// returns item[0] of item['0'] from array or collection
function first (thing) {
    return _get_element(thing, 0);
}

// returns true if item is in arr array
function found (item, arr) { return (typeof arr != 'undefined') && arr != null && _findIndex(arr, function(o){return ''+o == item}) != -1; }

// returns true if any element in items array is found in arr array
function found_any (items, arr) {
    if (typeof items == 'undefined' || typeof arr == 'undefined' || items == null || arr == null) return false;
    for (var i = 0; i < len(items); i++) if (found(_get_element(items, i), arr)) return true;
    return false;
}

// calculate a percent value, where percentage is between 0 and 100, and amount is some number
function pct (percentage, amount) { return (parseFloat(percentage) / 100.0) * parseFloat(amount); }

// standard comparison functions
function gt (x, compare) { return x > compare; }
function ge (x, compare) { return x >= compare; }
function lt (x, compare) { return x < compare; }
function le (x, compare) { return x <= compare; }
function eq (x, compare) { return x == compare; }
function ne (x, compare) { return x != compare; }
function startsWith (x, compare) { return x.startsWith(compare); }

function _inner_get_element(not_null_arr, field) {
    var v = not_null_arr[field];
    if ((v === undefined || v === null) && typeof not_null_arr.get === 'function') return not_null_arr.get(field);
    return v;
}

function _get_element(arr, field) {
    if (arr == null) return null;
    var v = _inner_get_element(arr, field);
    if ((v === undefined || v === null) && typeof field === 'number') return _inner_get_element(arr, '' + field);
    return v;
}

function match_object (field, value, comparison, found) {
    return function (obj) {
        if (typeof obj == 'undefined' || obj == null) return false;
        var target = obj;
        var path = field;
        var dotPos = path.indexOf('.');
        var v;
        while (dotPos != -1) {
            var prop = path.substring(0, dotPos);
            v = _get_element(target, prop);
            if (!v) return false;
            target = v;
            path = path.substring(dotPos+1);
            dotPos = path.indexOf('.');
        }
        v = _get_element(target, path);
        if (v && comparison(v, value)) {
            if (typeof found != 'undefined') {
                found.push(obj);
                return false;
            } else {
                return true;
            }
        }
        return false;
    };
}

// function to find the first object in array that matches field==value
// field may contain embedded dots to navigate within each object element of the array
function find (arr, field, value, comparison) {
    if (typeof comparison == 'undefined') comparison = eq;
    return _find(arr, match_object(field, value, comparison));
    // arr.find(match_object(field, value, comparison));
}

function _find (arr, func) {
    if ((typeof arr == 'undefined') || arr == null) return null;
    for (var i = 0; i < len(arr); i++) {
        var v = _get_element(arr, i);
        if (func(v)) return v;
    }
    return null;
}

function _findIndex (arr, func) {
    if ((typeof arr == 'undefined') || arr == null) return null;
    for (var i = 0; i < len(arr); i++) {
        if (func(_get_element(arr, i))) return i;
    }
    return -1;
}

function contains (arr, field, comparison, value) {
    var found = find(arr, field, value, comparison);
    return (typeof found !== 'undefined') && found !== null && found !== false;
}

// function to find the all object in array that match field==value
// field may contain embedded dots to navigate within each object element of the array
function find_all (arr, field, value, comparison) {
    if (typeof comparison == 'undefined') comparison = eq;
    var found = [];
    if ((typeof arr == 'undefined') || arr == null || len(arr) == 0) return found;
    _find(arr, match_object(field, value, comparison, found));
    // arr.find(match_object(field, value, comparison, found));
    return found;
}

// return sum total of array elements
function sum_total (arr, field) {
    var hasField = (typeof field != 'undefined');
    var sum = 0;
    for (var i = 0; i < len(arr); i++) {
        var v = _get_element(arr, i);
        sum += (hasField ? _get_element(v, field) : v);
    }
    return sum;
}

// returns a function that:
// 1) applies itemFunc function to an item, 2) uses comparison function to compare the result against compareVal
function compare (itemFunc, comparison, compareVal) {
    return function (item) {
        if ((typeof item != 'undefined') && item != null) {
            var val = itemFunc(item);
            if ((typeof val != 'undefined') && val != null) {
                return comparison(itemFunc(item), compareVal);
            }
        }
        return false;
    };
}

// return an itemFunc that treats item.field as a percentage, and multiplies it by total, and compares it against compareVal
function compare_pct (field, total, comparison, compareVal) {
    return function (item) {
        if ((typeof item == 'undefined') || item == null) return false;
        var val = _get_element(item, field);
        return (typeof val != 'undefined') && val != null && comparison(pct(val, total), compareVal);
    }
}

// apply itemFunc to each item in array arr. if any such invocation of itemFunc returns true, then this function returns true
function match_any (arr, itemFunc) {
    if ((typeof arr == 'undefined') || arr == null || len(arr) == 0) return false;
    // var found = arr.find(itemFunc);
    var found = _find(arr, itemFunc);
    return (typeof found != 'undefined') && found != null;
}

// functions for rounding up/down to nearest multiple
function up (x, multiple) { return multiple * parseInt(Math.ceil(parseFloat(x)/parseFloat(multiple))); }
function down (x, multiple) { return multiple * parseInt(Math.floor(parseFloat(x)/parseFloat(multiple))); }

// percentage difference between two numbers, as a floating-point number (1% == 1.0, 100% == 100.0, -5.4% == -5.4)
function pct_diff (x, y) {
    var d1 = 100 * (x/y);
    var d2 = 100 * (y/x);
    return d1 > d2 ? d1 : d2;
}

// return true if x is "close enough" to y, in terms of max_pct (default is 1%)
function is_close_enough (x, y, max_pct) {
    return 100 - pct_diff(x, y) <= ((typeof max_pct == "undefined") ? 1.0 : max_pct);
}
