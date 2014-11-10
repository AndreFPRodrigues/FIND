//     GmapsHiddenMarkers.js 1.0.0

//     (c) 2013 Mateu Yábar
//     GmapsHiddenMarkers may be freely distributed under the MIT license.
//     For all details and documentation:
//     http://www.parkit.cat


// math functions
window.my = window.my || {};
my.math = my.math || {};

if (typeof(Number.prototype.toRad) === "undefined") {
	Number.prototype.toRad = function() {
		return this * Math.PI / 180;
	}
}

//returns the distance in meters between two points
my.math.haversine = function (lat1,lng1,lat2,lng2){
	var R = 6371000;//m
	var dLat = (parseFloat(lat2)-parseFloat(lat1)).toRad();
	var dLon = (parseFloat(lng2)-parseFloat(lng1)).toRad();
	var lat1 = parseFloat(lat1).toRad();
	var lat2 = parseFloat(lat2).toRad();

	var a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
	var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	var d = R * c;

	return d;
}

my.math.normalize = function (vector){
	var length = Math.sqrt((vector.x * vector.x) + (vector.y * vector.y))
	return {
		x: vector.x/length,
		y: vector.y/length
	}
}

my.math.getDirectionVector = function (p0, p1){
	var diference={	
		x: p0.x - p1.x,	
		y: p0.y - p1.y
	};
	return my.math.normalize(diference);
}

// Package definition
// -------------

//Define pakages names
window.my = window.my || {};
my.ggmaps = my.ggmaps || {};

// DirectionMarker class
// -------------

// Draws an icon at the border of a google maps, when a marker is outside the map bounds.
// The icon is updated if the map is moved.

//###DirectionMarker.Constructor
//Creates a new direction marker. The options should be like
//> options={
//> 	*map: google_map,
//> 	*position: {latitude: 1.0, longitude:1.0},
//> 	*icon: "assets/image1.png",
//> 	*icon_NE: "assets/image1_NE.png",
//> 	*icon_size: [34, 30]
//> }
my.ggmaps.DirectionMarker = function(options){
	this.options=options;
	this._initView();
	this._updateView();
	this._addListeners();
}

// ###DirectionMarker.unrender
// Deletes the view of the marker. After this method is called, the DirectionMarker should not be used again.
my.ggmaps.DirectionMarker.prototype.unrender = function(){
	this.$figure.detach();
}

// ###DirectionMarker.update
//	Call to update the images or position of the marker. The view will be updated
my.ggmaps.DirectionMarker.prototype.update = function(options){
	this.options = $.extend(this.options, options);
	this._updateView();
}

//###DirectionMarker._addListeners
// Private method. It adds the listeners to the map, so the markers is modified when map bounds change
my.ggmaps.DirectionMarker.prototype._addListeners = function(){
	var map= this.options.map;
	var self=this;
	var selfUpdate = function() {self._updateView.call(self);}
	google.maps.event.addListener(map, 'bounds_changed', selfUpdate);
			
}

// ###DirectionMarker._isMarkerOutside
// Private method. Returns if the given position is not displayed in the map (is outside the map bounds)
// 
my.ggmaps.DirectionMarker.prototype._isMarkerOutside = function(){
		var map= this.options.map;
		var marker = this.options.position;
		//map not initialized, return null
		if(!map.getBounds()){
			return null;
		}
		return !map.getBounds().contains(new google.maps.LatLng(marker.latitude,marker.longitude));
	}

// ###DirectionMarker._initView
// Private method. Inits the direction marker view, creating its html representation. Must be called before _updateView
// 
my.ggmaps.DirectionMarker.prototype._initView = function(){
	var imagePath=this.options.icon;
	this.$img=$('<img style="position: absolute; left: 0px; top: 0px; -webkit-user-select: none; border: 0px; padding: 0px; margin: 0px" src="'+imagePath+'" draggable="false">');
	this.$img.css('width', this.options.icon_size[0]);
	this.$img.css('height', this.options.icon_size[1]);
	this.$figure =$('<div style="z-index: 999; position:fixed; opacity:0"></div>');
	this.$figure.append(this.$img);
	$(this.options.map.getDiv()).parent().prepend(this.$figure);
}

// ###DirectionMarker._updateView
// Private method. Updates the html representation:
// *(hide/display) if in bounds
// *change postion
// *change icon
my.ggmaps.DirectionMarker.prototype._updateView = function(){
	var mapPin = this.options.marker;

	if(!this._isMarkerOutside() || !mapPin.getVisible()){
		this.$figure.css("display","none");
		return;
	} else {
		this.$figure.css("display","block");
	}

	var marker = this.options.position;
	var map= this.options.map;
	var image = this.options.icon;
	var imageNE = this.options.icon_NE;
	var iconSize = this.options.icon_size;

	var direction=my.ggmaps.getDirectionVector(map, new google.maps.LatLng(marker.latitude,marker.longitude));
	var height = $(this.options.map.getDiv()).height();
	var width = $(this.options.map.getDiv()).width();
	var halfHeight = height/2.0;
	var halfWidth = width/2.0;

	var sizeFactor = Math.min(Math.abs(halfWidth/direction.x), Math.abs(halfHeight/direction.y));

	var arrowWidth= sizeFactor * direction.x +halfWidth;
	var arrowHeight= sizeFactor * (direction.y) + halfHeight;
	
	//translate y to bottom of image size
	arrowHeight-=iconSize[1]

	//allways in
	arrowWidth=Math.min(arrowWidth, width-iconSize[0]);
	arrowHeight=Math.max(arrowHeight, 0);

	//opacity deppending on relative distance
	var opacity = Math.max(0, 2 - direction.relativeDistance);

	//dummy fast way to chek if is on the top of right corner
	var imagePath=image;
	/*if(arrowWidth<=halfWidth && arrowHeight <= halfHeight && arrowWidth/width>arrowHeight/height) imagePath=imageNE;
	else if(arrowWidth>halfWidth && arrowHeight <= halfHeight) imagePath=imageNE;
	else if(arrowWidth>halfWidth && arrowHeight > halfHeight && arrowWidth/width>arrowHeight/height) imagePath=imageNE;*/

	if(this.$img.attr("src")!=imagePath)
		this.$img.attr("src", imagePath);
	this.$figure.css("top", ($('#header').outerHeight() + arrowHeight) + "px");
	this.$figure.css("left", arrowWidth+"px");
	this.$figure.css("opacity", opacity);
	
	
	return this.$figure;
}

// Utility Functions
// -------------------

// ##getDistanceFromCenterToCorner
// Returns the distance (in meters) from the center of the map to one corner
my.ggmaps.getDistanceFromCenterToCorner = function(map){
	var boundsNE=map.getBounds().getNorthEast();
	var mapCenter=map.getCenter();
	return my.math.haversine(boundsNE.lat(),boundsNE.lng(),mapCenter.lat(),mapCenter.lng());
}

//##getDirectionVector
// Calculates the direction vector from the center of the map, to a latlng. 
// Input:
// *map: google map
// *latlng1: google.maps.LatLng of the marker
// It returns:
// 	>{
// 	>	*x: (float) x component of the normalized direction vector
// 	>	*y: (float) y component of the normalized direction vector
// 	>	*distance: (float) distance in meters between the two points
// 	>	*relativeDistance: (float) division between the distance and the distance from the center of the map to a corner
// 	>}
my.ggmaps.getDirectionVector = function(map, latlng1){
	var mapCenter=map.getCenter();
	
	var mapCenter_pixel=map.getProjection().fromLatLngToPoint(mapCenter);
	var marker_pixel=map.getProjection().fromLatLngToPoint(latlng1);
	var result = my.math.getDirectionVector(marker_pixel, mapCenter_pixel);
	

	var distance=my.math.haversine(mapCenter.lat(), mapCenter.lng(), latlng1.lat(),latlng1.lng());
	result.distance = distance;

	var relativeDistance = distance / my.ggmaps.getDistanceFromCenterToCorner(map);
	result.relativeDistance = relativeDistance;

	return result;
}