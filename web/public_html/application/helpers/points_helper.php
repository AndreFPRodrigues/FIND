<?php

function points_to_javascript($pointsArray)
{
	$result = '[';
	$removeLastComma = FALSE;
	foreach($pointsArray as $p)
	{
		$result .= '(function() {';
		$result .= 'var marker = new google.maps.Marker({';
		$result .= 'position: new google.maps.LatLng(' . $p['latitude'] . ',' . $p['longitude'] . '),';
		$result .= 'map:map,';
		$result .= 'title: \'' . $p['nodeid'] . '\'';
		$result .= '});';
		$result .= 'marker.nodeId = \'' . $p['nodeid'] . '\';';
		if($p['msg'] !== NULL) $result .= 'marker.message = \'' . $p['msg'] . '\';';
		$result .= 'marker.timestamp = ' . $p['timestamp'] . ';';
		if($p['battery'] > -1) $result .= 'marker.battery = ' . $p['battery'] . ';';
		if($p['steps'] > -1) $result .= 'marker.steps = ' . $p['steps'] . ';';
		if($p['distance'] > -1) $result .= 'marker.distance = ' . $p['distance'] . ';';
		if($p['screen'] > -1) $result .= 'marker.screen = ' . $p['screen'] . ';';
		$result .= 'marker.safe = ' . $p['safe'] . ';';
		$result .= 'return marker;';
		$result .= "})()\n";
		$result .= ',';
		$removeLastComma = TRUE;
	}
	
	if($removeLastComma)
		$result = substr($result, 0, strlen($result) - 1);
		
	$result .= ']';
	
	return $result;
}

/* End of file points_helper.php */
/* Location: ./application/helpers/points_helper.php */