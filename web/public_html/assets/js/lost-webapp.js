/*
 * Global vars
 */
var map;
var infoWindow;
var points = [];
var maxSteps = 20;
var maxTime;
var minTime;
var oms;
var bounds;
var curPath = false;
var prePath = false;
var postPath = false;
var markerPath = false;
var pathPoints = [];
var prePathPoints = [];
var postPathPoints = [];
var updated = 0;
var curFilter = false;
var groupDistance = 0;
var zoomChanged = false;
var relScalePref = 0;
var isRelativeScale = false;
var criticalZone = false;
var criticalZoneOwner = 'you';  // TODO stub
var criticalZoneCross = false;
var criticalZoneHandle = false;
var settingsSetup = false;
var filterMissing = false;
var victimStats = [];
var victimsPoints = {};

var filterSimulation = false;
var simulation='default';
var queryDefault_init = 'index.php/rest/victims/ascending';
var queryDefault_new = 'index.php/rest/victims/mintimestamp/';
var querySimulation = 'index.php/rest/victims/simulation/';

var queryInit = queryDefault_init;
var queryNew= queryDefault_new;
var first_init=true;

var lost = {};
lost.debug = {};
lost.debug.mode = true;
lost.debug.log = function(str) {
	lost.debug.mode && console.log ? console.log(str) : function(){}
}

/*
 * Group range configuration
 */
var settRangeAbsolute = {
	range: 'min',
	min: 0,
	max: 100,
	value: 0,
	step: 10,
	tickLabels: {
		0: 'Don\'t<br/>group', 10: '10<br/>m', 20:'20<br/>m', 30:'30<br/>m',
		40:'40<br/>m', 50:'50<br/>m', 60:'60<br/>m', 70:'70<br/>m',
		80:'80<br/>m', 90:'90<br/>m', 100:'100<br/>m'
	}
};
var settRangeRelative = {
	range: 'min',
	min: 0,
	max: 6,
	value: 0,
	step: 1,
	tickLabels: {
		0: 'Don\'t<br/>group', 1: '&frac14;&times;<br/>scale', 2:'&frac12;&times;<br/>scale', 3:'1&times;<br/>scale',
		4:'2&times;<br/>scale', 5:'3&times;<br/>scale', 6:'4&times;<br/>scale'
	}
};

/**
 * Initializes the map and call the method to get first points
 */
function initialize() {
	
	infoWindow = new google.maps.InfoWindow();
	google.maps.event.addListener(infoWindow, 'closeclick', function(){
		// clear path when window is closed
		clearPath();
		// clear victim messages
		$('#message-cards').empty();
		$('#message-cards').append(document.createTextNode('Click on a victim to show sent messages'));
		$('#lblTabMessages').html('Messages');
		infoWindow.close();
	});
	
	bounds = new google.maps.LatLngBounds ();
	var opts = {
		zoom : 8,
		center : new google.maps.LatLng(38.71284,-9.135475),  // Lisboa, however the map will center on incident location
		mapTypeId : google.maps.MapTypeId.ROADMAP,
		noClear : true, // allow elements over the map
		panControl: false,  // remove superior left corner navigation
		zoomControl: true,
		mapTypeControl: true,  // allow toggle between Road and Sattelite views
		scaleControl: true, // scale visible on map canvas
		streetViewControl: false,
		overviewMapControl: false,
		zoomControlOptions: {
			position: google.maps.ControlPosition.LEFT_TOP // put zoom controls always at left top
		}
	};
	map = new google.maps.Map(document.getElementById('map'), opts);
	/*clusterer = new MarkerClusterer(map);*/

	/*oms = new OverlappingMarkerSpiderfier(map);
	oms.addListener('click', function(marker, event) {
		if( ! isOutsiteTimeline(marker)) {
			// show path
			markerPath = marker
			showPath(markerPath);
			showMarkerMessages(marker);
			infoWindow.setContent(createDialog(marker));
			infoWindow.open(map, marker);
		}
	});
	
	oms.addListener('spiderfy', function(markers) {
		infoWindow.close();
	});*/
	
		getStartPoints(); // async
	
		populateSimulationFilter();
	
}

function startSimulationView(){
	clearMarkers();
	points = [];
	victimStats = [];
	victimsPoints = {};
	getStartPoints();
	recalculateStats();
	updateMapPoints($('#slider-range').slider('option'));
}

/**
 * Gets the initial points to show on the map
 */
function getStartPoints() {
	$.ajax(queryInit).done(function(newPoints){
		var length = newPoints.length || 0;

		var distances = [];
		var totalValid = 0;
		var mintime = 9999999999999;
		var maxtime = 0;
		
		for(var i = 0; i < length; i++) {
			newPoints[i] = createNewMarker(newPoints[i]);
			
			var ll = newPoints[i].getPosition();
			if( ll.lat() == 0 && ll.lng() == 0 )
				continue;
			
			totalValid++;
			
			// min-max update
			if(newPoints[i].timestamp > maxtime)
				maxtime = newPoints[i].timestamp;
			
			if(newPoints[i].timestamp < mintime)
				mintime = newPoints[i].timestamp;
				
			if(newPoints[i].added > updated)
				updated = newPoints[i].added + 1;
			
			bounds.extend(newPoints[i].getPosition());
			//oms.addMarker(newPoints[i]);
			//clusterer.addMarker(newPoints[i]);
			
			// add support for marker outside the viewport
			/*var hiddenOtions = {
					map: map,
					position: {
						latitude: newPoints[i].getPosition().lat(),
						longitude: newPoints[i].getPosition().lng(),
					},
					marker: newPoints[i],
					icon: 'assets/imgs/markers/default_inactive.png',
					icon_size: [15,27]
			};
			newPoints[i].hiddenMarker = new my.ggmaps.DirectionMarker(hiddenOtions);*/
			
			// calculate point distance
			var myId = newPoints[i].nodeId;
			if(myId in distances) {
				var previousPoint = distances[myId];
				newPoints[i].distance = google.maps.geometry.spherical.computeDistanceBetween(
					newPoints[i].getPosition(), previousPoint.getPosition());
			}
			distances[myId] = newPoints[i];

			addNewPoint(newPoints[i]);
			updateMarkerIcon(newPoints[i]);
		}
		
		lost.debug.log('getStartPoints(): ' + totalValid + ' valid points to add');
		
		if( false && totalValid < 1) {
			$('#loadingStatus').html('No points yet, checking for new points...')
			setTimeout('getStartPoints()', 10 * 1000); // 10 seconds
			return;
		} else {
			// update scale on the slider
			$('#slider-range').slider( 'option', 'min', mintime - (10*60*1000) );
			$('#slider-range').slider( 'option', 'max', maxtime + (10*60*1000) );
			
			// generate adequate range on the slider (1 minutes step)
			$('#slider-range').slider( 'option', 'step', 1 * 60 * 1000 );
			
			// set point updates, change informative text
			$('#slider-range').on( 'slide', function( event, ui ) {
				if( typeof ui !== 'undefined' ){
					// typical update routines
					updateMapPoints(ui);
					changeScaleText(ui);
					if(markerPath !== false)
						showPath(markerPath);
					updateSliderTooltips(ui);
				}
			});

			$('#slider-range').on( 'slidestop', function( event, ui ) {
				$('#slider-container .timeline-tooltip').hide();
			
				// magnetic edges
				var magTrigger = 0.02;  // 2% of interval
				var values = ui.values;
				var min = $('#slider-range').slider('option', 'min');
				var max = $('#slider-range').slider('option', 'max');
				var interval = max - min;
				var refresh = false;
				
				if(values[0] <= (min + interval * magTrigger)) {
					$('#slider-range').slider('values', 0, min);
					refresh = true;
				}
				
				if(values[1] >= (max - interval * magTrigger)) {
					$('#slider-range').slider('values', 1, max);
					refresh = true;
				}
				
				if(refresh) {
					changeScaleText(ui);
					updateMapPoints(ui);
				}
			});
			
			// time range
			$('#slider-range').slider('option', 'values', [mintime, maxtime]);
			var ui = $('#slider-range').slider('option');
			updateMapPoints(ui);
			changeScaleText(ui);

			map.fitBounds(bounds);
			
			recalculateStats();
			
			// show only representative victims
			for(var name in victimsPoints)
				victimsPoints[name][0].setMap(map);
				
			// schedule future updates
			window.setInterval(
				function(){
					try {
						getNewPoints()
					} catch(e) {
						// Reserved only for the most horrible and weird bugs
						lost.debug.log('getNewPoints(): Error, reloading. : ' + e)
						window.location.reload();
					}
				},
				15 * 1000   // every 15 seconds
			);
			
			// close loading feedback
			$('#tplLoading').dialog('close');
		}
	});
}

/**
 * Gets new points after the page was loaded
 */
function getNewPoints() {
	$.ajax(queryNew + updated).done(function(newPoints){
		var length = newPoints.length || 0;
		var max = $('#slider-range').slider('option').max;
		var min = $('#slider-range').slider('option').min;
		var valid= 0;
		lost.debug.log('getNewPoints(): Received ' + length + ' new points');
		
		if(length > 0) {
			for(var i = 0; i < length; i++) {
				//oms.addMarker(newPoints[i]);
				//clusterer.addMarker(newPoints[i]);
				newPoints[i] = createNewMarker(newPoints[i]);
				
				var ll = newPoints[i].getPosition();
				if( ll.lat() == 0 && ll.lng() == 0 )
					continue;
					
				addNewPoint(newPoints[i]);
				bounds.extend(ll);
				valid++;
				//newPoints[i].setAnimation(google.maps.Animation.DROP);
				
				// update min/max timestamp
				if(newPoints[i].timestamp > max)
					max = newPoints[i].timestamp;

				if(newPoints[i].timestamp < min)
					min = newPoints[i].timestamp;
				
				// update timestamp for future request
				if(newPoints[i].added >= updated)
					updated = newPoints[i].added + 1;
				
				// add support for marker outside the viewport
				/*var hiddenOtions = {
						map: map,
						position: {
							latitude: newPoints[i].getPosition().lat(),
							longitude: newPoints[i].getPosition().lng(),
						},
						marker: newPoints[i],
						icon: 'assets/imgs/markers/default.png',
						icon_size: [15,27]
				};
				newPoints[i].hiddenMarker = new my.ggmaps.DirectionMarker(hiddenOtions);*/
				updateMarkerIcon(newPoints[i]);
			}
			
			if(valid > 0) {
					// update timeline to new max time
					max += 10*60*1000;  // usability
					min -= 10*60*1000;  // usability
					
					lost.debug.log('getNewPoints(): Will adjust maximum to ' + max);
					
					$('#slider-range').slider( 'option', 'max', max );
					$('#slider-range').slider( 'values', 1, max );
					$('#slider-range').slider( 'option', 'min', min );
					$('#slider-range').slider( 'values', 0, min );
					
					changeScaleText($('#timeline').slider('option'));
					updateMapPoints($('#timeline').slider('option'));
					
					recalculateStats();
					
					// update existing path
					if(curPath != false)
						showPath(markerPath);
			}
		}
	});
}


/**
 * Creates a new Google Marker from victim point information, without a map associated
 * @param data Victim point information in JSON format
 * @return new Google Marker with integrated information
 */
function createNewMarker(data)
{
	// basic attributes, setting map to null allows lazy loading
	var marker = new google.maps.Marker({
		position : new google.maps.LatLng(data.latitude, data.longitude),
		map : null,
		title : data.nodeid,
	});
	
	// extended attributes for LOST Map
	marker.nodeId = data.nodeid;
	marker.timestamp = parseInt(data.timestamp);
	marker.added = parseInt(data.added);
	if(data.msg != null) marker.message = data.msg;
	if(data.battery > -1) marker.battery = parseInt(data.battery);
	if(data.steps > -1) marker.steps = parseInt(data.steps);
	if(data.screen > -1) marker.screen = parseInt(data.screen);
	marker.safe = parseInt(data.safe) == 1;
	
	// events
	google.maps.event.addListener(marker, 'click', function() {
		showPath(marker);
		infoWindow.setContent(createDialog(marker));
		infoWindow.open(map, marker);
		showMarkerMessages(marker);
	});
	
	return marker;
}

/**
 * Adds a point to the victims' points collection
 * @param marker New point to add
 */
function addNewPoint(marker)
{
	var key = marker.nodeId;
	
	if( !(key in victimsPoints) ) {
		victimsPoints[key] = [];
		victimStats.push(key);
	}
		
	victimsPoints[key].push(marker);
	victimsPoints[key].sort(function(a, b) {
		return b.timestamp - a.timestamp;
	});
}

/**
 * Updates the visibility of map points according to the selected interval
 * @param ui Slider on which the user chooses the interval 
 */
function updateMapPoints(ui) {
	ui = $('#slider-range').slider('option');
	var min = ui.values[0]
	var max = ui.values[1];
	
	for(var victim in victimsPoints){
		
		var representative = false;
		for(var i = 0; i < victimsPoints[victim].length; i++) {
			var p = victimsPoints[victim][i];
			
			if(representative == false && 
				p.timestamp >= min && p.timestamp <= max && 
				(!filterMissing || (filterMissing && !isVictimSafe(victim))) ){
				if(p.getMap() === null)
					p.setMap(map);

				representative = true;
			}else{
				p.setMap(null);
			}
		}
	}
	
	// redraw marker colors
	redrawMarkers();
	
	// update window info
	/*if(markerPath !== false) {
		showPath(markerPath);
		infoWindow.setContent(createDialog(markerPath));
	}*/
}

/**
 * Changes the range textual description
 * @param ui Slider on which the user chooses the interval 
 */
function changeScaleText(ui) {
	ui = $('#slider-range').slider('option');
	var dateMin = new Date(ui.values[0]);
	var dateMax = new Date(ui.values[1]);

	var minMoment = "From <tt>" + dateMin.format('d-MM-yyyy hh:mm') + "</tt>";
	var maxMoment = "to <tt>" + dateMax.format('d-MM-yyyy hh:mm') + "</tt>";
	
	$('#timespan').html(minMoment + " " + maxMoment);
}

/**
 * Updates and shows the tooltips to aid user changing the timeline
 * @param ui Slider on which the user chooses the interval 
 */
function updateSliderTooltips(ui) {
	var fullUI = $('#slider-range').slider('option');
	var handle = $(ui.handle);
	var offset = handle.offset();
	var first = $('#slider-range').children('.ui-slider-handle').first();
	var pos = handle.is(first) ? 0 : 1;
	var value = new Date(fullUI.values[pos]).format('d-MM-yyyy<br/>hh:mm');
		
	var tooltip = $('#slider-container .timeline-tooltip');
	tooltip
		.html(value)
		.css('top', offset.top + 10 )
		.css('left', offset.left - tooltip.width() / 2)
		.show();
}

/**
 * Recalculates and updates the statistics to show in the right panel
 */
function recalculateStats() {
	var totalVictims = victimStats.length;
	var savedVictims = 0;
	var missingVictims = 0;
	
	for(var i = 0; i < totalVictims; i++)
		if(isVictimSafe(victimStats[i]))
			savedVictims++;
	
	missingVictims = totalVictims - savedVictims;
	
	$('#lblStatsVictims').html(totalVictims);
	$('#lblStatsSaved').html(savedVictims);
	$('#lblStatsMissings').html(missingVictims);
}

/**
 * Shows the full path for a given marker (outside and inside time interval)
 * @param marker Map marker
 */
function showPath(marker) {
	// clean previous path
	clearPath();
	
	var points = victimsPoints[marker.nodeId];
	var markerId = marker.nodeId;
	markerPath = marker;
		
	// get points that have the correct id
	var limits = $('#slider-range').slider( "values" );
	for(var i = points.length-1; i >= 0; i--) {
		points[i].setMap(map);
		
		// distribute points among correct path segments
		if(points[i].timestamp < limits[0])
			prePathPoints.push(points[i]);
			
		else if(points[i].timestamp > limits[1])
			postPathPoints.push(points[i]);
			
		else
			pathPoints.push(points[i]);
	}
	
	// no path to draw
	if(pathPoints.length < 1) {
		clearPath();
		pathPoints = [];
		// close infoWindow
		infoWindow.close();
		return;
	}
	
	//pathPoints = pathPoints.sort(function(a,b) { a.timestamp - b.timestamp});
	
	if(groupDistance !== false && groupDistance > 0) { 		
		for(var i = 0; i < pathPoints.length - 1; i++) {
			var diff = google.maps.geometry.spherical.computeDistanceBetween(
					pathPoints[i].getPosition(), pathPoints[i+1].getPosition());
			
			if(diff < groupDistance) {
				pathPoints[i].setVisible(false);
				pathPoints.splice(i, 1); // at position i, remove 1 item
				i--;
			}
		}
	}

	// calculate estimated distances for points without distance from GPS
	for(var i = 1; i < pathPoints.length; i++) {
		if(typeof pathPoints[i].distance === 'undefined' || pathPoints[i].distance < 0) {		
			pathPoints[i].distance = google.maps.geometry.spherical.computeDistanceBetween(
					pathPoints[i-1].getPosition(), pathPoints[i].getPosition());
		}
	}

	// prepare locations for paths
	var preLocations = [];
	for(i = 0; i < prePathPoints.length; i++) 
		preLocations.push(prePathPoints[i].position);
	
	var locations = [];
	for(i = 0; i < pathPoints.length; i++) 
		locations.push(pathPoints[i].position);
		
	var postLocations = [];
	for(i = 0; i < postPathPoints.length; i++) 
		postLocations.push(postPathPoints[i].position);
		
	// join locations (previous with next available)
	if(locations.length > 0)
		preLocations.push(locations[0]);
		
	else if(postLocations.length > 0)
		preLocations.push(postLocations[0])
	
	// join locations (current with the next, if any)
	if(locations.length > 0 && postLocations.length > 0)
		postLocations.unshift(locations[locations.length - 1]);
		
	// pre-path
	var dashSymbol = {
		path: 'M 0,-0.5 0,0.5',
        strokeWeight: 2,
        strokeOpacity: 1,
        scale: 3
	};
	
	if(preLocations.length > 0) {
		prePath = new google.maps.Polyline({
			path: preLocations,
			icons: [{
				icon: dashSymbol,
				offset: '100%',
				repeat: '10px'
			}],
			map: map,
			strokeColor : "#777"
		});
	}
	
	// path of defined time interval
	var lineSymbol = {
		path: google.maps.SymbolPath.FORWARD_CLOSED_ARROW,
		strokeColor : "#FF0000",
		fillColor : "#FF0000",
		strokeOpacity : 0.8,
		strokeWeight : 2
	};

	if(locations.length > 0) {
		curPath = new google.maps.Polyline({
			path : locations,
			icons: [{
				icon: lineSymbol,
				offset: '100%'
			}],
			map: map,
			strokeColor : "#FF0000"
		});
	}
	
	// post-path
	if(postLocations.length > 0) {
		postPath = new google.maps.Polyline({
			path: postLocations,
			icons: [{
				icon: dashSymbol,
				offset: '100%'
			}],
			map: map,
			strokeColor : "#777"
		});
	}
	
	// recolor marker (less focus on other points and past/future points on path)
	for(var i = 0; i < points.length; i++) {
		updateMarkerIcon(points[i]);
	}
}

/**
 * Clears the path from the map
 */
function clearPath() {
	if(curPath != false) {
		curPath.setMap(null);
		curPath = false;
	}
	
	if(prePath != false) {
		prePath.setMap(null);
		prePath = false;
	}
	
	if(postPath != false) {
		postPath.setMap(null);
		postPath = false;
	}

	markerPath = false;
	
	// remove points from map
	for(var i = 0; i < prePathPoints.length; i++)
		prePathPoints[i].setMap(null);
	
	prePathPoints = [];
	
	// remove all but the last, which is the representative
	for(var i = 0; i < pathPoints.length -1 ; i++)
		pathPoints[i].setMap(null);
	
	if(pathPoints.length > 0) {
		var representative = pathPoints[pathPoints.length - 1];
		pathPoints = [];
		updateMarkerIcon(representative);
	}
	
	for(var i = 0; i < postPathPoints.length; i++)
		postPathPoints[i].setMap(null);
		
	postPathPoints = [];
	
	updateMapPoints($('#slider-range').slider('option'));
}

/**
 * Creates a dialog to show on the map
 * @param marker Map marker
 * @returns {String} dialog contents
 */
function createDialog(marker) {
	var isMessage = marker.message != null && "" != marker.message;
	var htmlDialog = document.createElement('div');
	htmlDialog.className = 'marker-dialog';
	var isLast = false;
	var isFirst = false;
	
	// Create small banner to tell when victim has messages
	if(victimHasMessages(marker)) {
		var banner = document.createElement('div');
		banner.className += ' attention alert';
		var text = document.createElement('p');
		var link = document.createElement('a');
		text.appendChild(document.createTextNode('Victim sent messages. '));
		link.href = '#seeMessages';
		link.onclick = function() {
			// open Messages tab
			$('#tabs').tabs( "option", "active", 1 );
			return false;
		};
		link.appendChild(document.createTextNode('See all'));
		text.appendChild(link);
		banner.appendChild(text);
		htmlDialog.appendChild(banner);
	}
	
	// Create small bannet to tell when victim marked as safe
	if(isVictimSafe(marker.nodeId)) {
		var safeBanner = document.createElement('div');
		safeBanner.className += ' safe alert';
		var text = document.createElement('p');
		text.appendChild(document.createTextNode('Victim reported her/himself as safe.'));
		safeBanner.appendChild(text);
		htmlDialog.appendChild(safeBanner);
	}
	
	// Next/Previous links
	var nextPrev = false;
	if(pathPoints.length > 1) {
		nextPrev = document.createElement('div');
		nextPrev.className = 'nav-links';
		var curPos = pathPointIndex(marker);
		
		// prev link
		var prev = document.createElement('a');
		prev.className += ' btn btn-default';
		prev.appendChild(document.createTextNode('Previous'));
		prev.href = '#prevNode';
		
		if(curPos >= 1) {
			prev.onclick = function() {
				var prevMarker = pathPoints[curPos - 1];
				map.panTo(prevMarker.getPosition());
				infoWindow.close();
				infoWindow.setContent(createDialog(prevMarker));
				infoWindow.open(map, prevMarker);
				showMarkerMessages(prevMarker);
				return false;
			}
		} else {
			prev.className += ' disabled';
			isFirst = true;
		}
		
		nextPrev.appendChild(prev);
		
		// next link
		var next = document.createElement('a');
		next.className += ' btn btn-default';
		next.appendChild(document.createTextNode('Next'));
		next.href = '#nextNode';
		
		if(curPos <= pathPoints.length - 2) { // before the last, so there's a next marker
			next.onclick = function() {
				var nextMarker = pathPoints[curPos + 1];
				map.panTo(nextMarker.getPosition());
				infoWindow.close();
				infoWindow.setContent(createDialog(nextMarker));
		  		infoWindow.open(map, nextMarker);
		  		showMarkerMessages(nextMarker);
		  		return false;
			}
		} else {
			next.className += ' disabled';
			isLast = true;
		}
		
		nextPrev.appendChild(next);
	} else {
		isLast = true;
	}
	
	// heading
	var heading = document.createElement('div');
	var headIcon = document.createElement('img');
	headIcon.width = headIcon.height = 16;
	headIcon.src = 'assets/imgs/' + (isMessage ? 'user_comment' : 'transmit') + '.png';
	var headNameContainer = document.createElement('span');
	headNameContainer.style = 'margin-left: 10px';
	var headName = document.createElement('strong');
	var victimName = marker.nodeId;
	if(isLast) {
		victimName += ' (last activity)';
	} else if(isFirst) {
		victimName += ' (first activity)';	
	}
	headName.appendChild(document.createTextNode(victimName));
	headNameContainer.appendChild(headName);
	heading.appendChild(headIcon);
	heading.appendChild(headNameContainer);
		
	// message, if any
	var messageBody = false;
	if(isMessage) {
		messageBody = document.createElement('div');
		messageBody.className = 'message-body'
		messageBody.appendChild(document.createTextNode('"' + marker.message + '"'));
	}
	
	// popup body (instant indicators)
	var _createLine = function(head, value) {
		var line = document.createElement('tr');
		var headCell = document.createElement('th');
		var valueCell = document.createElement('td');
		
		headCell.appendChild(document.createTextNode(head));
		valueCell.appendChild(document.createTextNode(value));
		line.appendChild(headCell);
		line.appendChild(valueCell);
		
		return line;
	};
	
	var tableInstant = document.createElement('table');
	var tableInstantHeadRow = document.createElement('tr');
	var tableInstantHeadCell = document.createElement('th');
	tableInstantHeadCell.colSpan = 2;
	tableInstantHeadCell.innerHTML = 'Here (' + new Date(marker.timestamp).format('d-MM-yyyy hh:mm') + ')';
	tableInstantHeadRow.appendChild(tableInstantHeadCell);
	tableInstant.appendChild(tableInstantHeadRow);
	
	if('battery' in marker && marker.battery > -1)
		tableInstant.appendChild(_createLine('Battery', marker.battery + ' %'));
		
	/*if('hbr' in marker && marker.hbr > -1)
		tableInstant.appendChild(_createLine('Heart Beat Rate', marker.hbr + ' bpm'));*/
	
	// popup body (accumulated indicators)
	var tableAccum = document.createElement('table');
	var tableAccumHeadRow = document.createElement('tr');
	var tableAccumHeadCell = document.createElement('th');
	tableAccumHeadCell.colSpan = 2;
	tableAccumHeadCell.innerHTML = 
		'From ' + new Date(pathPoints[0].timestamp).format('d-MM-yyyy hh:mm') + ' until here';
	tableAccumHeadRow.appendChild(tableAccumHeadCell);
	tableAccum.appendChild(tableAccumHeadRow);
	
	if('steps' in marker && marker.steps > -1)
		tableAccum.appendChild(_createLine('Micro-movements', marker.steps));
	
	if('distance' in marker && marker.distance > -1)
		tableAccum.appendChild(_createLine('Distance traveled', Math.round(accumulatePointValues(pathPointIndex(marker), 'distance')) + ' m'));
	
	if('screen' in marker && marker.screen > -1)
		tableAccum.appendChild(_createLine('Screen activated', marker.screen + ' times'));
	
	// join all DOM objects
	if(nextPrev !== false)
		htmlDialog.appendChild(nextPrev);
		
	htmlDialog.appendChild(heading);
	
	if(messageBody !== false)
		htmlDialog.appendChild(messageBody);
		
	htmlDialog.appendChild(tableInstant);
	htmlDialog.appendChild(tableAccum);
	
	return htmlDialog;
}

/**
 * Gets the accumulated value for a given point indicator inside a path
 * @param markerIndex index of marker in the path
 * @param indicator Indicator to accumulate
 * @return Value accumulated for current indicator
 */
function accumulatePointValues(markerIndex, indicator) {
	var value = 0;
	
	for(var i = 0; i <= markerIndex; i++) {
		switch(indicator) {
			case 'steps':
			value += pathPoints[i].steps || 0;
			break;
			
			case 'distance':
			value += pathPoints[i].distance || 0;
			break;
			
			case 'screen':
			value += pathPoints[i].screen || 0;
			break;
		}
	}
	
	return value;
}

/**
 * Gets the accumulated value for a given point indicator globally
 * @param markerId identifier for this node
 * @param indicator Indicator to accumulate
 * @return Value accumulated for current indicator
 */
function accumulatePointValuesOutPath(markerId, indicator) {
	var value = 0;
	
	// get visible points
	var limits = $('#slider-range').slider( "values" );
	var points = victimsPoints[markerId];
	for(var i = 0; i < points.length; i++) {
		// eligible path point
		if(points[i].nodeId == markerId &&
			points[i].timestamp >= limits[0] &&
			points[i].timestamp <= limits[1]
		){
			switch(indicator) {
			case 'steps':
			value += points[i].steps || 0;
			break;
			
			case 'distance':
			value += points[i].distance || 0;
			break;
			
			case 'screen':
			value += points[i].screen || 0;
			break;
			}
		}
	}
	
	return value;
}

/**
 * Returns the index of a marker in the path list
 * @param marker Marker to find the position.
 * @returns 0..pathPoints.length-1 if marker exists; -1 otherwise
 */
function pathPointIndex(marker) {
	if(typeof pathPoints == 'undefined' || pathPoints.length < 1)
		return -1;
	
	var cur = 0;
	while(cur < pathPoints.length) {
		var curMarker = pathPoints[cur];
		if(marker.timestamp == curMarker.timestamp && marker.nodeId == curMarker.nodeId) {
			return cur;
		}
		cur++;
	}
	
	return -1;
}

/**
 * Returns the last time when the marker was seen during an interval
 * @param marker Map marker to extract node id
 * @param min Minimum border of time range (default start of event)
 * @param max Maximum border of time range (default the last time a victim was seen)
 * @return {String}
 */
function lastSeen(marker, min, max) {
	min = typeof min !== 'undefined' ? min : $('#slider-range').slider('option', 'min');
	max = typeof max !== 'undefined' ? max : $('#slider-range').slider('option', 'max');
	var recent = marker.timestamp;
	var points = victimsPoints[marker.nodeId];
	
	for(var i = 0; i < points.length; i++){
		if( points[i].timestamp >= min &&  points[i].timestamp <= max &&
			points[i].nodeId == marker.nodeId && points[i].timestamp > recent )
			recent = points[i].timestamp;
	}
	
	return new Date(recent).format('d-MM-yyyy hh:mm');
}

/**
 * Changes the graphical representation of the marker acording to current criteria
 * @param marker Map marker
 * @returns {String} Path to image
 */
function updateMarkerIcon(marker) {
	var name = 'assets/imgs/markers/';
	var limits = $('#slider-range').slider('values');
	
	if(curPath != false && markerPath.nodeId == marker.nodeId && (marker.timestamp < limits[0] || marker.timestamp > limits[1])) {
		name += 'out.png';
		marker.setIcon(name);
		//marker.hiddenMarker.options.icon = name;
		return name;
	}
	
	if(curFilter === false){
		if(pathPointIndex(marker) >= 0)
			name += 'path';
		else
			name += 'default';
	} else if( !(curFilter[0] in marker) ){
		name += 'default';
	} else {
		var type = curFilter[0];
		var toRed = curFilter[1]; // |xxxx0-----0-----|
		var toYellow = curFilter[2]; // |---0xxxx0-----|
		var reverse = curFilter[3];  // green -> red scale
		
		var value = 0;
		if(type != 'battery')
			value = accumulatePointValuesOutPath(marker.nodeId, type);
		else
			value = marker[type];
		
		if(value <= toRed) {
			name += (reverse ? 'green' : 'red');
		} else if(value <= toYellow) {
			name += 'yellow';
		} else {
			name += (reverse ? 'red' : 'green');
		}
	}
	
	// nodes that contains text message
	if('message' in marker && marker.message.length !== 0)
		name += '_msg';
		
	// check if node is outside path while on filter
	if(curFilter !== false && curPath != false && markerPath.nodeId != marker.nodeId)
		name += '_inactive';
	
	name += '.png';
	
	marker.setIcon(name);
	/*marker.hiddenMarker.options.icon = name;*/
	
	return name;
}

/**
 * Redraws the icons of all markers to ajust to new filters
 */
function redrawMarkers() {
	for(var victim in victimsPoints){
		for(var i = 0; i < victimsPoints[victim].length; i++)
			updateMarkerIcon(victimsPoints[victim][i]);
	}
}

function clearMarkers() {
	for(var victim in victimsPoints){
		for(var i = 0; i < victimsPoints[victim].length; i++)
			(victimsPoints[victim][i]).setMap(null);
	}
}

/**
 * Shows all messages sent by a victim in the right message panel
 * @param marker Map marker
 */
function showMarkerMessages(marker) {
	var table = false;
	var markerId = marker.nodeId;
	var messagePanel = document.getElementById('message-cards');
	var totalMsgs = 0;
	
	// remove placeholder text and clean previous messages
	$('#messages p.description').hide();
	$(messagePanel).empty();
	
	// victim identification
	var p = document.createElement('p');
	$(messagePanel).append(p);	

	var limits = $('#slider-range').slider('values');
	
	// message log, if any
	var points = victimsPoints[markerId];
	for(var i = 0; i < points.length; i++) {
		if(points[i].message != null && "" != points[i].message) {
			totalMsgs++;
			
			if(table === false) {
				table = document.createElement('table');
				table.className += ' messages-table';
			}
			
			// populate marker img, if relevant, timestamp and message
			var line = document.createElement('tr');
			
			// paint entries out of range with grey
			if(points[i].timestamp < limits[0] || points[i].timestamp > limits[1])
				line.className += ' old';

			var cellMarker = document.createElement('th');
			
			// Create shortcut icon
			var markerImage = document.createElement('img');
			markerImage.src = points[i].getIcon();
			markerImage.className += ' pointer';
			markerImage.curMarker = points[i]
			markerImage.onclick = function() {
				map.panTo(this.curMarker.getPosition());
				infoWindow.close();
				infoWindow.setContent(createDialog(this.curMarker));
				infoWindow.open(map, this.curMarker);
			};
			cellMarker.appendChild(markerImage);
			line.appendChild(cellMarker);
			
			// message time
			var cellTime = document.createElement('th');
			cellTime.className += ' text-center';
			cellTime.innerHTML = new Date(points[i].timestamp).format('d-MM-yyyy<br/>hh:mm');
			line.appendChild(cellTime);
			
			// message contents
			var cellMessage = document.createElement('td');
			cellMessage.appendChild(
				document.createTextNode(points[i].message)
			);
			line.appendChild(cellMessage);
			
			table.appendChild(line);
		}
	}
	
	$('#lblTabMessages').html('Messages (' + totalMsgs + ')');
	
	p.appendChild(document.createTextNode('All text messages sent by ' + marker.nodeId + ' (' + totalMsgs + ' messages)'));
	
	if(table === false) {
		var pwm = document.createElement('p');
		pwm.appendChild(document.createTextNode('This victim didn\'t send any message'));
		$(messagePanel).append(pwm);
	} else {
		// allow message reorder, descending by default
		var dropdown = document.createElement('select');
		
		var optDesc = document.createElement('option');
		optDesc.value = 'desc';
		optDesc.text = 'Descending (newer to older)';
		var optAsc = document.createElement('option');
		optAsc.value = 'asc';
		optAsc.text = 'Ascending (older to newer)';
		
		dropdown.appendChild(optDesc);
		dropdown.appendChild(optAsc);
		
		dropdown.onchange=function(){
			$(function(){
				$(table).each(function(elem,index){
				  var arr = $.makeArray($("tr",this).detach());
				  arr.reverse();
					$(this).append(arr);
				});
			});
		};
		
		messagePanel.appendChild(dropdown);
		
		messagePanel.appendChild(table);
		
	}
}

/**
 * Creates a custom button control to add to Google Maps
 * @param text Label do put on button
 * @param action Action on button click
 * @return outer div HTML element to put on map
 */
function customGoogleMapsButton(text, action) {
	var controlDiv = document.createElement('div');
	
	var controlUI = document.createElement('div');
	controlUI.style.backgroundColor = 'white';
	controlUI.style.backgroundClip = 'padding-box';
	controlUI.style.cursor = 'pointer';
	controlUI.style.textAlign = 'center';
	controlUI.style.color =  'rgb(86, 86, 86)';
	controlUI.style.border = '1px solid rgba(0, 0, 0, 0.15)';
	controlUI.style.borderBottomLeftRadius = '2px';
	controlUI.style.borderRadius = '2px';
	controlUI.style.margin = '5px';
	controlUI.style.boxShadow = '0px 1px 4px -1px rgba(0, 0, 0, 0.3)';
	controlDiv.appendChild(controlUI);
	
	var controlText = document.createElement('div');
	controlText.style.padding = '1px 6px';
	controlText.innerHTML = text;
	controlUI.appendChild(controlText);
	
	google.maps.event.addDomListener(controlUI, 'click', action);
	
	return controlDiv;
}

/**
 * Tries to return the current map scale
 * @param useMeters Indicates if the scale value must be in meters (boolean)
 * @return array with value and metric, eg [ 200, 'm'] = 200m; false if no value detected
 */
function getMapScale(useMeters) {
	// HACK may be broken in future API of Google Maps
	var scaleDiv = $('div.gm-style-cc:nth-child(11) > div:nth-child(2) > span:nth-child(1)');
	if(!scaleDiv)
		return false;
	
	var result = scaleDiv.html().split(' ');
	// cleanup
	result[0] = $('<span>' + result[0] + '</span>').text().trim();
	result[1] = $('<span>' + result[1] + '</span>').text().trim();
	
	if(useMeters === true && result[1] == 'km') {
		result[0] *= 1000;
		result[1] = 'm';
	}
	
	return result;
}

function populateScaleGroupFeedback() {
	var relScaleText = '';
	
	if(relScalePref == 1)
		relScaleText += '&frac14;';
	else if(relScalePref == 2)
		relScaleText +=  '&frac12;';
	else if(relScalePref != 0)
		relScaleText += (relScalePref - 2 + '');
	
	if( relScalePref != 0 ){
		relScaleText += ' &times; scale';
		$('#statusGrouping').html(groupDistance + ' meters (' + relScaleText + ')');
	} else {
		$('#statusGrouping').html('Don\'t group');
	}
}

function changeRelativeGroupDistance() {
	var scale = getMapScale(true)[0];
	relScalePref = $('#slider-group').labeledslider('value');
	if(relScalePref == 1)
		groupDistance = 0.25 * scale;
	else if(relScalePref == 2)
		groupDistance = 0.5 * scale;
	else if(relScalePref != 0)
		groupDistance = (relScalePref - 2) * scale;
	else if(relScalePref == 0)
		groupDistance = 0;
		
	// scale feedback
	populateScaleGroupFeedback();
}

/**
 * Checks if the victims was considered as safe now or before
 * @param nodeId The victim identifier
 * @return boolean: true if the victim was considered as safe at least once, false otherwise
 */
function isVictimSafe(nodeId) {
	if( typeof victimsPoints[nodeId] == 'undefined' )
		return false;
		
	for(var i = 0; i < victimsPoints[nodeId].length; i++)
		if('safe' in victimsPoints[nodeId][i] && victimsPoints[nodeId][i].safe == 1)
			return true;
	
	return false;
}

/**
 * Checks if a marker is outside of the current timeline
 * @param marker Marker defined by user
 * @return boolean: true if the marker is outside of the interval, false otherwise
 */
function isOutsiteTimeline(marker) {
	var interval = $('#slider-range').slider('option').values;
	
	return marker.timestamp < interval[0] || marker.timestamp > interval[1];
}

/**
 * Checks if a marker has messages during its trail
 * @param marker Marker defined by user
 * @return boolean: true if the marker has any messsage, false otherwise
 */
function victimHasMessages(marker) {
	var key = marker.nodeId;
	for(var i = victimsPoints[key].length - 1; i >= 0 ; i--)
		if(victimsPoints[key][i].message != null && "" != victimsPoints[key][i].message)
			return true;
	
	return false;
}

/**
 * Returns the last object for a victim
 * @param nodeId Victim ID (String)
 * @return marker: false if no information; marker of last instance otherwise
 */
function victimLastSeen(nodeId) {
	return victimsPoints[nodeId][0];
}

/** Populate the simulation filter with the active Simulations	
**/
function populateSimulationFilter(){

	$.ajax('index.php/rest/simulations/running').done(function(simulations){
		var length = simulations.length || 0;
		var select = document.getElementById('simulationsFilter');
		for(var i = 0; i < length; i++) {
			var opt = document.createElement('option');
			opt.value = (simulations[i]).name + "," + (simulations[i]).status;
			if((simulations[i]).status==1){
				opt.innerHTML = (simulations[i]).name + " starts at " + (simulations[i]).start_date + " with a duration " + (simulations[i]).duration_m + " minutes";
			}else if((simulations[i]).status==2){
					opt.innerHTML = (simulations[i]).name + " running";
				}else{
					opt.innerHTML = (simulations[i]).name + " finished";
				}
				
			select.appendChild(opt);
		}
	});
}

/**
 * Function which runs after the page load
 */
$(document).ready(function(){

	// loading popup
	$('#tplLoading').dialog({
		dialogClass: 'no-close',
		position: 'center center', 
        resizable: false, 
        draggable: false, 
        modal: true,
        closeOnEscape: false
	});
	
	// start map
	initialize();
	
	 //$( "#slider-range" ).labeledslider({
	$( "#slider-range" ).slider({
		range: true,
		min: 0,
		max: 100,
		values: [ 0, 100 ],
		step: 10
	});
	 
	// help tooltips
	$(document).tooltip({
		items: '.help-button',
		content: function() {
			var element = $( this );
			var id = element.data('id');
			return $('#' + id).html();
		}
	});

	// adjust background for Settings liveness slider
	var slideChangeBack = function(event, ui) {
		var opts = $('#slider-liveness').labeledslider('option');
		var first = ui.values[0];
		var second =  ui.values[1];
		
		// avoid slider overlapping
		if( (first + opts.step) > second ){
            return false;
        }
	
		// adjust colours in slider background
		var length = $('#slider-liveness').width();
		var percX =  (opts.values[0] / opts.max) * length - length;
		$( "#slider-liveness" ).css('background-position', percX + 'px -6px')
		
		// update preview values
		$('#lblPreviewRed').html(first == opts.min ? 'Exactly ' + first : 'Equal or less than ' + first);
		$('#lblPreviewYellow').html('From ' + (first + 1) + ' to ' + second);
		$('#lblPreviewGreen').html(/*second == opts.max ? '&#8211;' : */'More than ' + second);
	};
	
	// options icon
	$('#settingsLink').button();
	
	// Filter options
	$('#tplSettingsLiveness').dialog({
		autoOpen: false,
		resizable: false,
		width: 500,
		modal: true,
		position: 'center',
		buttons: {
			'Save' : function() { 
				if(curPath != false) {
					showPath(markerPath);
					infoWindow.close();
				}
				 
				 // save liveness
			 	var filterName = $('[name="liveness"]:checked').val();
				var useColour = $('#chkUseColor').prop('checked');
			 	
			 	if( !useColour )
			 		curFilter = false;
			 	else
					curFilter = [
					 	filterName,  // selected filter
					 	$( "#slider-liveness" ).labeledslider('values')[0],  
					 	$( "#slider-liveness" ).labeledslider('values')[1],
					 	$( "#slider-liveness" ).hasClass('green-red')  // reverse red-green
					];
				
				// change active criteria panel
				if( groupDistance > 0 ){
					$('#statusGrouping')
						.removeClass('label-default')
						.addClass('label-danger');
					
					if(isRelativeScale)
						populateScaleGroupFeedback();
					else 
						$('#statusGrouping').html(groupDistance + ' meters');
					
				}else{
					$('#statusGrouping')
						.removeClass('label-danger')
						.addClass('label-default')
						.html('Don\'t group');
				}
				
				if( curFilter === false )
					$('#statusLiveness')
						.removeClass('label-danger')
						.addClass('label-default')
						.html('None');
				else {
					$('[name="liveness"]:checked').each(function() {
						var idVal = $(this).attr("id");
						var status = $("label[for='"+idVal+"']").text();
						$('#lblPreviewFilter').html(status);
						$('#statusLiveness')
							.removeClass('label-default')
							.addClass('label-danger')
							.html(status);
					});
				}
				
				//save simulation filter redraw map with new points
				var aux_simulation = $('#simulationsFilter').val();
				if(aux_simulation!=simulation){
					simulation=aux_simulation;
					if(aux_simulation=='default'){
						filterSimulation = false;
						queryInit = queryDefault_init;
						queryNew = queryDefault_new;
						startSimulationView();
						$(this).dialog('close');
						return;
						}
					else{
						queryInit = querySimulation + simulation+ ',-1';
						queryNew = querySimulation + simulation + ',';
						filterSimulation = true;
						startSimulationView();
						$(this).dialog('close');
						return;
					}
				}
				
				// save missing filter
				var onlyMissing = $('#chkMissing').prop('checked');
				var slider = $('#slider-range').slider('option');
				
				filterMissing = onlyMissing;
				
				// adjust map information to only show missing victims
				if(onlyMissing == true) {
					if(markerPath != false && isVictimSafe(markerPath.nodeId))
						clearPath();
					
					for(var key in victimsPoints) {
						var points = victimsPoints[key];
						
						for(var i = 0; i < points.length; i++) {
							if(points[i].timestamp >= slider.values[0] &&
								points[i].timestamp <= slider.values[1] &&
								isVictimSafe(points[i].nodeId)) {
								points[i].setMap(null);
							}
						}
					}
				} else {
					// redraw points and path, if any
					updateMapPoints(slider);
				}
				
				// update marker information
				redrawMarkers();
				
				$(this).dialog('close');
			},
			'Cancel' : function() { $(this).dialog('close') }
		},
		create: function(ev, ui) {
			$('#slider-liveness').labeledslider({
				range: true,
				min: 0,
				max: 100,
				values: [ 30, 60 ],
				step: 10,
				slide: slideChangeBack,
				change: slideChangeBack
			});
			
			// activate/deactivate colour filter
			$('#chkUseColor').change(function() {
				var value = $('#chkUseColor').prop('checked');
				$('#livenessRadioGroup input').prop('disabled', value ? false : 'disabled');
				$('#livenessRadioGroup label').css('opacity', (value ? 1.0 : 0.35));
				$('#slider-liveness').labeledslider((value ? 'enable' : 'disable'));
				$('#livenessPreview').css('opacity', (value ? 1.0 : 0.35));
				
				// HACK update preview values at startup
				slideChangeBack(false, $('#slider-liveness').labeledslider('option'))
			});
			
			// change scales, etc, according to indicator type
			$('[name="liveness"]').change(function() {
				var value = $( this ).val();
				var idVal = $(this).attr('id');
				var status = $("label[for='"+idVal+"']").text();
				$('#lblPreviewFilter').html(status);

				// hide range bar if no indicator is selected
				/*$( '#slider-liveness' ).labeledslider(
					value === 'none' ? 'disable' : 'enable'
				);*/

				// toggle scale (red-green/green-red)
				switch(value) {

					// red-green
					case 'battery':
					case 'steps':
					case 'distance':
					case 'screen':
					case 'hbr':
						$('#slider-liveness').removeClass('green-red');
						$('#slider-liveness').addClass('red-green');
						break;

					// green-red
					default:
						$('#slider-liveness').removeClass('red-green');
						$('#slider-liveness').addClass('green-red');
				 }

				// Default scales [min, max, until is left extreme value, until is middle value, step of slider]
				var scales = {
					battery : [0, 100, 20, 50, 10],
					steps : [0, 200, 20, 60, 20],
					distance : [0, 500, 50, 100, 50],
					screen : [0, 20, 2, 4, 2],
					hbr : [0, 120, 40, 60, 10]	
				};

				// default
				if(curFilter !== false) {
					scales[curFilter[0]][2] = curFilter[1];
					scales[curFilter[0]][3] = curFilter[2];
				}

				 // recreate new limits
				$('#slider-liveness').labeledslider('option', 'min', scales[value][0] );
				$('#slider-liveness').labeledslider('option', 'max', scales[value][1] );
				$('#slider-liveness').labeledslider('option', 'step', scales[value][4] );
				$('#slider-liveness').labeledslider('option', 'values', [scales[value][2], scales[value][3]] );
			});
			
			// HACK set initial scale to red-green, so colours are visible when open
			$('#slider-liveness').addClass('red-green');
		},
		open: function(ev, ui) {
			$('#chkUseColor').prop('checked', curFilter !== false ? 'checked' : false);
			$('#chkMissing').prop('checked', filterMissing);
		}
	});
	$('#btnSettLiveness').button();
	$('#btnSettLiveness').click(function() {
		$('#tplSettingsLiveness').dialog('open');
		return false;
	});
	
	// make the buttons same width
	$('#btnSettLiveness, #btnSettRange, #btnSettZone').css('display', 'block');
	
	// Critical Area options
	$('#tplSettingsZone').dialog({
		autoOpen: false,
		resizable: false,
		width: 400,
		modal: true,
		position: 'center',
		create: function(ev, ui) {
			// draw critical area button
			$('#btnDrawZone').button();
			$('#btnDrawZone').click(function() {
				$('#tplSettingsZone').dialog('close');
				
				var zoneOptions = {
					strokeColor: '#FF0000',
					strokeOpacity: 0.5,
					strokeWeight: 2,
					fillColor: '#FF0000',
					fillOpacity: 0.2,
					map: map,
					center: map.getCenter(),
					radius: getMapScale(true)[0] * 2,
					clickable: false,
					editable: true
				};
				criticalZone = new google.maps.Circle(zoneOptions);
				
				// settings overviews feedback for critical area
				$('#statusZone')
					.removeClass('label-default')
					.addClass('label-danger')
					.html('Set by ' + criticalZoneOwner);
					
				// create button to jump to this zone
				var buttonJump = customGoogleMapsButton('Go to Critical Area', function() {
					map.setCenter(criticalZone.getCenter());
				});
				map.controls[google.maps.ControlPosition.TOP_CENTER].push(buttonJump);
				
				return false;
			});
			
			$('#btnRemoveZone').button();
			$('#btnRemoveZone').click(function(){
				$('#tplRemoveZone').dialog({
					autoOpen: false,
					resizable: false,
					modal: true,
					buttons: {
						'Delete Critical Area': function() {
							// remove controls
							criticalZone.setMap(null);
							criticalZone = false;
							
							// remove feedback
							$('#statusZone')
								.addClass('label-default')
								.removeClass('label-danger')
								.html('None');
								
							// remove custom button from map
							map.controls[google.maps.ControlPosition.TOP_CENTER].removeAt(1); // TODO hack manhoso
							
							$('#tplRemoveZone').dialog('close');
							$('#tplSettingsZone').dialog('close');
						},
						'Cancel': function() {
							$('#tplRemoveZone').dialog('close');
						}
					}
				});
				$('#tplRemoveZone').dialog('open');
				return false;
			});
		},
		open : function(ev, ui) {
			var zoneExists = (criticalZone !== false);
			$('#btnDrawZone').css('display', zoneExists ? 'none' : 'block')
			$('#btnRemoveZone').css('display', !zoneExists ? 'none' : 'inherit')
			$('#zoneDescription').html(
				zoneExists ? 
					'The Critical Area was set by  <strong>' + criticalZoneOwner + '</strong>. ' +
						'You can remove if it is not needed anymore.'
					: 
					'There is not a Critical Area defined yet, but you can create one. ' +
						'The Critical Area will be placed on the map centre and then you can move or resize it as needed.'
			)
		}
	});
	$('#btnSettZone').button();
	$('#btnSettZone').click(function() {
		$('#tplSettingsZone').dialog('open');
		return false;
	});
	
	// Grouping options
	$('#tplSettingsRange').dialog({
		autoOpen: false,
		resizable: false,
		width: 500,
		modal: true,
		position: 'center',
		buttons: {
			'Save' : function() { alert('stub save') },
			'Cancel' : function() { $(this).dialog('close') }
		}
	});
	$('#btnSettRange').button();
	$('#btnSettRange').click(function() {
		$('#tplSettingsRange').dialog('open');
		return false;
	});
	
	// Add button to restore map view, near zoom control
	var controlDiv = customGoogleMapsButton('Center on situation', function() {
		if(!bounds.isEmpty()){
			map.fitBounds(bounds);
			map.panToBounds(bounds);
		}
	});
	map.controls[google.maps.ControlPosition.TOP_CENTER].push(controlDiv);
	
	// right panel tabs support
	$('#tabs').tabs();
	
	// victim text search config
	$('#txtSearch').autocomplete({
		source: victimStats,
		minLength: 2,
		select: function( event, ui ) {
			// show selected victim on map
			var marker = victimLastSeen(ui.item.value);
			map.panTo(marker.getPosition()); 
			google.maps.event.trigger(marker, 'click', {
			  latLng: new google.maps.LatLng(0, 0) // mock click event
			});
			return false;
		}
	}).data('ui-autocomplete')._renderItem = function( ul, item ) {
		return $('<li>').
			append(
				'<a>' + item.value + '<br/>' +
				'<span class="old small">' + 
				'Last seen: ' + new Date(victimLastSeen(item.value).timestamp).format('d-MM-yyyy hh:mm') +
				'</span>' +
				'</a>'	
			).appendTo(ul);
	};
});

