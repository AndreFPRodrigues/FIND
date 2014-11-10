/**
 * Extensions to objects and auxiliary functions
 */

/**
 * Formating for date object
 */
 Date.prototype.format = function (format) //author: meizz
{
    var hours = this.getHours();
    var ttime = "AM";
    if(format.indexOf("t") > -1 && hours > 12)
    {
        hours = hours - 12;
        ttime = "PM";
     }

	var o = {
	    "M+": this.getMonth() + 1, //month
	    "d+": this.getDate(),    //day
	    "h+": hours,   //hour
	    "m+": this.getMinutes(), //minute
	    "s+": this.getSeconds(), //second
	    "q+": Math.floor((this.getMonth() + 3) / 3),  //quarter
	    "S": this.getMilliseconds(), //millisecond,
	    "t+": ttime
	}
	
	if (/(y+)/.test(format)) format = format.replace(RegExp.$1,
	  (this.getFullYear() + "").substr(4 - RegExp.$1.length));
	for (var k in o) if (new RegExp("(" + k + ")").test(format))
	    format = format.replace(RegExp.$1,
	      RegExp.$1.length == 1 ? o[k] :
	        ("00" + o[k]).substr(("" + o[k]).length));
	return format;
}
 
 /**
  * Loads content asynchronously
  */
function loadXMLDoc(target, method, onResult) {
	var xmlhttp;

	if (window.XMLHttpRequest) {
		// code for IE7+, Firefox, Chrome, Opera, Safari
		xmlhttp = new XMLHttpRequest();
	} else {
		// code for IE6, IE5
		xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
	}

	xmlhttp.onreadystatechange = function() {
		if (xmlhttp.readyState == 4 && xmlhttp.status == 200) {
			onResult(xmlhttp.responseText);
		}
	}

	xmlhttp.open(method, target, true);
	xmlhttp.send();
}

/** 
 * jQuery changed event propagation through "prop" calls
 * http://stackoverflow.com/a/8392279
 */
 jQuery.propHooks.checked = {
    set: function (el, value) {
        if (el.checked !== value) {
            el.checked = value;
            $(el).trigger('change');
        }
    }
};