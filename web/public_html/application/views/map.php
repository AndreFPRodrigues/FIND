<!DOCTYPE html>
<html>
<head>
	<meta charset="utf-8">
	<base href="<?php echo base_url() ?>" />
	
	<title>LOST Map &#8211; Victims Locator</title>
	
	<link rel="stylesheet" type="text/css" href="assets/css/bootstrap.min.css">
	<link rel="stylesheet" type="text/css" href="assets/css/jqueryui/jquery-ui-1.10.4.min.css">
	<link rel="stylesheet" type="text/css" href="assets/css/jquery.ui.labeledslider.css">
	<link rel="stylesheet" type="text/css" href="assets/css/lost-webapp-styles.css">
	
	<script type="text/javascript" src="http://maps.google.com/maps/api/js?sensor=false&amp;libraries=geometry&amp;language=en-GB"></script>
	<script type="text/javascript" src="assets/js/jquery-1.11.0.min.js"></script>
	<script type="text/javascript" src="assets/js/jquery-ui-1.10.4.min.js"></script>
	<script type="text/javascript" src="assets/js/jquery.ui.labeledslider.js"></script>
	<script type="text/javascript" src="assets/js/jquery.ui.touch-punch.min.js"></script>
	<script type="text/javascript" src="assets/js/moment.min.js"></script>
	<script type="text/javascript" src="assets/js/oms.min.js"></script>
    <script type="text/javascript" src="assets/js/gmapsHiddenMarkers.js"></script>
	<script type="text/javascript" src="assets/js/extensions.js"></script>
	<script type="text/javascript" src="assets/js/lost-webapp.js"></script>
</head>
<body>
	<div id="container">
		<div id="header" class="row">
			<div id="slider-container" class="col-xs-12">
				<p id="timeinfo">
					<strong>Timeline:</strong> <span id="timespan">Present</span> 
				<img alt="help" src="assets/imgs/help.png" width="16" height="16" class="help-button" data-id="tplHelpTimeline" />
				</p>	
				<div id="slider-range" class="shadow">&shy;</div>
				<span class="ui-tooltip timeline-tooltip">&shy;</span>
			</div>
		</div>
		
		<div id="contents" class="row">
			<div id="map" class="col-xs-9">&shy;</div>
			<div class="col-xs-3">
				<div id="statusPanel" class="row">
					<p><a id="btnSettZone" href="#tplSettingsZone">Critical Area <span id="statusZone" class="label label-default settings-open" data-highlight="legendZone">None</span></a></p>
					<p><a id="btnSettLiveness" href="#tplSettingsLiveness">Filters <span id="statusLiveness" class="label label-default settings-open" data-highlight="legendLiveness">None</span></a></p>
					<hr/>
					<label for="txtSearch"><strong>Search for victims (min. 2 characters):</strong></label>
					<input type="search" id="txtSearch" placeholder="Search by name" class="form-control" />
				</div>
				
				<hr/>
				
				<div id="tabs" class="row">
					<ul>
						<li><a href="#tabs-1">Stats</a></li>
						<li><a href="#tabs-2" id="lblTabMessages">Messages</a></li>
					</ul>
					<div id="tabs-1">
						<table>
							<tr>
								<th>Victims still missing</th>
								<td><span id="lblStatsMissings">&hellip;</span></td>
							</tr>
							<tr>
								<th>Victims already saved</th>
								<td><span id="lblStatsSaved">&hellip;</span></td>
							</tr>
							<tr>
								<th>Total of victims registered</th>	
								<td><span id="lblStatsVictims">&hellip;</span></td>
							</tr>
						</table>
					</div>
					<div id="tabs-2">
						<div id="messages" class="row">
							<p class="description">
								Select a victim to show the aproximate path and text messages.
							</p>
							<div id="message-cards">&shy;</div>
						</div>
					</div>
				</div>
			</div>		
		</div>
	</div>
	
	<!-- Templates -->
	
	<div id="tplSettingsRange" title="Grouping" class="template">
		<h2>Grouping <img alt="help" src="assets/imgs/help.png" width="16" height="16" class="help-button" data-id="tplHelpGroup" /></h2>
		<p>Group same victim locations when they are within:</p>
		<div id="settings-group-range">
			<div id="slider-group" class="shadow">&shy;</div>
		</div>
		<p>
			<input type="checkbox" name="groupScale" id="chkGroupScale" /> 
			<label for="chkGroupScale">Adjust values to the current map scale <img alt="help" src="assets/imgs/help.png" width="16" height="16" class="help-button" data-id="tplHelpGroupScale" /></label>
		</p>
	</div>
	
	<div id="tplSettingsLiveness" title="Filters" class="template">
	
		
		<div>
			<input type="checkbox" name="usecolor" id="chkUseColor" checked="checked" />
			<label for="chkUseColor"><strong>Use colour filters to distinguish victims</strong></label>
			<p>Allows you to filter markers with a colour range for one criterion.</p>
		</div>
		
		<div class="row" id="livenessRadioGroup">
			<div class="col-sm-6">
				<input type="radio" name="liveness" value="distance" id="radDistance" checked="checked" /> 
				<label for="radDistance">Distance traveled (meters)</label><br/>
				<input type="radio" name="liveness" value="steps" id="radSteps" /> 
				<label for="radSteps">Number of movements</label><br/>
			</div>
			
			<div class="col-sm-6">
				<input type="radio" name="liveness" value="battery" id="radBattery" /> 
				<label for="radBattery">Device battery (%)</label><br/>
				<input type="radio" name="liveness" value="screen" id="radScreen" /> 
				<label for="radScreen">User screen interaction</label><br/>
			</div>
		</div>
		
		<div id="settings-range" class="row">
			<div id="slider-liveness">&shy;</div>
		</div>
		
		<div id="livenessPreview">
			<p>Markers with "<span id="lblPreviewFilter">Distance traveled (meters)</span>" criterion will be shown as</p>
			<div class="row">
				<div class="col-sm-3">
					<p class="text-center"><img src="assets/imgs/markers/red.png" width="18" height="33" alt="Red" /></p>
					<p class="text-center" id="lblPreviewRed">&#8211;</p>
				</div>
				<div class="col-sm-3">
					<p class="text-center"><img src="assets/imgs/markers/yellow.png" width="18" height="33" alt="Yellow" /></p>
					<p class="text-center" id="lblPreviewYellow">&#8211;</p>
				</div>
				<div class="col-sm-3">
					<p class="text-center"><img src="assets/imgs/markers/green.png" width="18" height="33" alt="Green" /></p>
					<p class="text-center" id="lblPreviewGreen">&#8211;</p>
				</div>
				<div class="col-sm-3">
					<p class="text-center"><img src="assets/imgs/markers/default.png" width="18" height="33" alt="Blue" /></p>
					<p class="text-center">Without information</p>
				</div>
			</div>
		</div>
		
		<div>
			<input type="checkbox" name="missing" id="chkMissing" />
			<label for="chkMissing"><strong>Hide victims that have already been saved</strong></label>
			<p>Allows you to quickly filter out victims which are reported as being safe, 
			alowing more focus on victims still missing. </p>
		</div>
		
		<div>
			<select id="simulationsFilter">
			  <option value="default">All</option>
			</select>
			<label for="simulationsFilter"><strong>Display only points associated with the simulation</strong></label>

		</div>
		
	</div>
	
	<div id="tplSettingsZone" title="Critical Area" class="template">
		<p id="zoneDescription">&shy;</p>
		<a href="#drawZone" id="btnDrawZone">Create Critical Area</a>
		<a href="#removeZone" id="btnRemoveZone">Remove Critical Area...</a>
	</div>
	
	<!-- Zone Remove template -->
	<div id="tplRemoveZone" title="Remove Critical Area?" class="template">
		<p>
			<strong>Attention!</strong> The Critical Area is shared by all people using the map. By 
			removing the current Critical Area, others will not be able to see it 
			and will be removed from their maps too.
		</p>
		<p>
			This action is not reversible. Do you want to delete the Critical Area?
		</p>
	</div>
	
	<!-- loading template -->
	<div id="tplLoading" title="Loading..." class="template">
		<img src="assets/imgs/loading.gif" width="64" height="64" alt="Loading from server..."/>
		<p id="loadingStatus">Loading information from the server...</p>
	</div>
	
	<!-- help templates -->
	<div id="tplHelp" class="template">
		<div id="tplHelpGroup">
			<p>
				Grouping victim locations can help to avoid GPS
				errors while the victim is fixed in some place.
			</p>
			<p>
				By using this option, the locations for each single victim will be grouped 
				into one single location for that victim. That single point will contain 
				all data consolidated regarding the time period.
			</p>
		</div>
		
		<div id="tplHelpGroupScale">
			<p>
				When this option is active, the groupping distance will be calculated with the current zoom 
				level. This is useful when you want to zoom it or out and changing the visual groupping distance 
				according to the zoom. May be useful on widely dispersed regions with several victims.
			</p>
			<p>
				When the option is inactive, the actual fixed scale of groupping is used (0-100m), regardless the 
				zoom level applied in the map.
			</p>
		</div>
		
		<div id="tplHelpLiveness">
			<p>
				The marker on the map will be drawn according the 
				criterion and the color range you choose. Markers without 
				information about the desired indicator will be shown 
				as blue.
			</p>
			<p>
				If you choose 'None', all markers will be shown as blue, 
				regardless their properties.
			</p>
			<p>
				Preview: 
				<img src="assets/imgs/markers/red.png" width="11" height="20" alt="Red marker" />
				<img src="assets/imgs/markers/yellow.png" width="11" height="20" alt="Yellow marker" />
				<img src="assets/imgs/markers/green.png" width="11" height="20" alt="Green marker" />
				<img src="assets/imgs/markers/default.png" width="11" height="20" alt="Blue marker" />
			</p>
		</div>
		
		<div id="tplHelpTimeline">
			<p>
				The timeline shows the time range as projected by the map. You can 
				increase or decrease the time range using the big red slider. The 
				last victim position within the selected period is always visible in the map.
			</p>
			<p>
				By clicking on a marker, the path that victim walked during the selected time range 
				will be shown, along the data gathered during the message.
			</p>
		</div>
		
		<div id="tplHelpMessages">
			<p>
				Select a point on the map to reveal the victim's 
				path during the defined interval and all text messages 
				sent by that victim during the full event regardless the 
				selected interval.
			</p>
			<p>
				The path will be visible on the map and the text messages, if any, 
				will be visible on this right panel.
			</p>
		</div>
		
		<div id="tplHelpZone">
			<p>
				The Critical Area is a zone in the map which has a special meaning for a 
				group of people. It could represent a particular dangerous place on 
				the scene or even an operations center.
			</p>
			<p>
				When someone creates a zone, it is shared with all people, and everyone 
				can see or move the zone in the map. All changes are reflected to everyone.
			</p>
		</div>
	</div>
</body>
</html>
