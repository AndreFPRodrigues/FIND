<!DOCTYPE html>
<html><head>
<meta charset="UTF-8">
<title>LOST Map – REST API Documentation</title>
<link rel="stylesheet" type="text/css" href="<?php echo base_url() ?>/assets/css/bootstrap.min.css">
</head>

<body>
<div class="container">
<div class="page-header">
<h1>LOST Map – REST API Documentation</h1>
<p>This API allows read and write operations to victims' data and
related information.</p>
<p><a href="<?php echo base_url() ?>">Cancel and go to map</a></p>
</div>
<div>
 
<ul>
	<li><a href="#data_structures">Data Structures</a>
		<ul><li><a href="#victim_info">Victim Information</a></li></ul>
	</li>
	<li><a href="#methods_available">Methods available</a>
		<ul>
			<li><a href="#rest_victims">/rest/victims</a></li>
			<li><a href="#rest_victims_mintimestamp">/rest/victims/mintimestamp</a></li>
			<li><a href="#rest_victims_lastpoints">/rest/victims/lastpoints</a></li>
			<li><a href="#rest_victims_llbbox">/rest/victims/llbbox</a></li>
			<li><a href="#rest_victims_id">/rest/victims/id</a></li>
		</ul>
	</li>
</ul>

<h2 id="data_structures">Data Structures</h2>
<p>This sections explains some of the common data structures you may find while using the 
webservice.</p>

<h3 id="victim_info">Victim information</h3>
<p>The victim information consists on a JSON array containing object. Each object represents a 
single point from a single victim. Each object have a set of fields, based on key-value storage, which 
contains useful information regarding the victim condition at that point.</p>
<p>The next table summarizes the fields you may find while decoding the JSON structure:</p>
<table class="table">
<tr>
	<th>Key</th>
	<th>Description</th>
</tr>
<tr>
	<td>nodeid</td>
	<td>Victim unique identificator.</td>
</tr>
<tr>
	<td>timestamp</td>
	<td>The time in miliseconds since 1-Jan-1970 registered by the victim application. It corresponds to the time when the message was created in the client. It is usually composed by 13 digits.</td>
</tr>
<tr>
	<td>msg</td>
	<td>The text message written by the victim, if any.</td>
</tr>
<tr>
	<td>latitude</td>
	<td>Represents the point latitude, used for geographical positioning.</td>
</tr>
<tr>
	<td>longitude</td>
	<td>Represents the point longitude, used for geographical positioning.</td>
</tr>
<tr>
	<td>llconf</td>
	<td>Confidence of the geographical coordinates. Currently, '0' means that the coordinates were obtained from the last known location, while '10' means that the exact geographical location was retrieved directly from the GPS, and should be treated as accurate. This field is intended for future use.</td>
</tr>
<tr>
	<td>battery</td>
	<td>Current battery level reported by the victim application.</td>
</tr>
<tr>
	<td>steps</td>
	<td>Number of steps detected by the victim application, if sensors are available.</td>
</tr>
<tr>
	<td>screen</td>
	<td>Number of times that the screen was turned on by the victim, if available.</td>
</tr>
<tr>
	<td>distance</td>
	<td>Currently, this field has no meaning. Distance should be calculated by the client consuming the webservive, if needed. It should return NULL of -1.</td>
</tr>
<tr>
	<td>safe</td>
	<td>Tells if the victim marked itself as safe (1) or not (0). Possible value are 1 and 0.</td>
</tr>
<tr>
	<td>added</td>
	<td>The time in miliseconds since 1-Jan-1970 when the message was received by the webservice. This value should be used to get new data periodically, minimizing the number of points to process.</td>
</tr>
</table>


<h2 id="methods_available">Methods available</h2>
<p>The available methods allows control of victims' data and
interaction with other features in LOST-Map</p>

<!-- victims method -->
<h3 id="rest_victims">/rest/victims</h3>
<p>Restrieves information regarding all registered victims.</p>
<h4>Request</h4>
<table class="table">
<tbody>
<tr>
<th>Verb</th>
<th>Parameter</th>
<th>Description</th>
</tr>
<tr>
<td>GET</td>
<td>—</td>
<td>Gets all points for every victim registered in
webservice. This method doesn't accept parameters.<br>
</td>
</tr>
<tr>
<td>POST</td>
<td>data (JSON array)</td>
<td>Must contain victim information in JSON format. This
method allows the insertion of multiple victim information. You should
send an array even if you intend to send only one record.</td>
</tr>
</tbody>
</table>
<h4>Response</h4>
<table class="table">
<tbody>
<tr>
<th>HTTP code</th>
<th>Internal code</th>
<th>Response</th>
</tr>
<tr>
<td>200</td>
<td>—</td>
<td> Returns all victims' points. Example: <br>
Request:
<pre>GET /rest/victims</pre>
Response:
<pre>[<br> {<br> "nodeid":"Alberto",<br> "timestamp":"1385888400000",<br> "msg":"",<br> "latitude":"38.7531",<br> "longitude":"-9.15618",<br> "battery":"92",<br> "steps":"0",<br> "screen":"1",<br> "distance":null,<br> "safe":"0",<br> "added":"1385888400000"<br> },<br> {<br> "nodeid": ...<br> },<br> ...<br>]</pre>
<hr>Request:
<pre>POST /rest/victims/<br>...<br>data=[{"nodeid":"Alberto", "timestamp":"1385888400000", ... }, { ... } ]<br></pre>
Response:
<pre>{ "sent":2, "inserted":2 }<br></pre>
</td>
</tr>
<tr>
<td>400</td>
<td>801</td>
<td>The sent string was not correctly interpreted. It could
be damaged, incomplete or with wrong syntax. Manually check the data
sent to this method and ensure that the string is an array with
information for each victim in the string format. You should send an
array even if you intend to report only one victim.</td>
</tr>
<tr>
<td>400</td>
<td>802</td>
<td>No victim information had been received. This means
that the information was correctly decoded but there are no victim
records. Please ensure that the records are being sent along with your
request. Also check if the data is being sent in JSON array format even
if you intend to report only one victim. </td>
</tr>
</tbody>
</table>

<!-- victims/mintimestamp method -->
<h3 id="rest_victims_mintimestamp">/rest/victims/mintimestamp</h3>
Retrieves information about victims' points with a given minimum
timestamp.<br>
<h4>Request</h4>
<table class="table">
<tbody>
<tr>
<th>Verb</th>
<th>Parameter</th>
<th>Description</th>
</tr>
<tr>
<td>GET</td>
<td>long numeric type (ex: 1234567890123)</td>
<td>Represents the minimum timestamp from which points are
included in result. The time to compare is the time when the message was 
registered in the database and not the client application timestamp. 
The exact match is also included. point.added &gt;= parameter.</td>
</tr>
</tbody>
</table>
<h4>Response</h4>
<table class="table">
<tbody>
<tr>
<th>HTTP code</th>
<th>Internal code</th>
<th>Response</th>
</tr>
<tr>
<td>200</td>
<td>—</td>
<td>Example Request:<br>
<pre>GET /rest/victims/mintimestamp/1234567890123</pre>
Response:<br>
<pre>[<br> {<br> "nodeid":"Alberto",<br> "timestamp":"1300000000000",<br> "msg":"",<br> "latitude":"38.7531",<br> "longitude":"-9.15618",<br> "battery":"92",<br> "steps":"0",<br> "screen":"1",<br> "distance":null,<br> "safe":"0",<br> "added":"1300000000000" <br> },<br> {<br> "nodeid": ...,<br> "added": 1234567890123,<br> ...&nbsp;<br> },<br> ...<br>]</pre>
</td>
</tr>
</tbody>
</table>

<!-- victims/lastpoints method -->
<h3 id="rest_victims_lastpoints">/rest/victims/lastpoints</h3>
Retrieves the last points for every victim.<br>
<h4>Request</h4>
<table class="table">
<tbody>
<tr>
<th>Verb</th>
<th>Parameter</th>
<th>Description</th>
</tr>
<tr>
<td>GET</td>
<td>positive integer numeric type (ex: 5)</td>
<td>Retrieves the N last points of every victim. Semantically,
the result is a collection of the most updated entries of each victim in
the disaster. Without parameters, this method returns the very last record (one entry) per victim.</td>
</tr>
</tbody>
</table>
<h4>Response</h4>
<table class="table">
<tbody>
<tr>
<th>HTTP code</th>
<th>Internal code</th>
<th>Response</th>
</tr>
<tr>
<td>200</td>
<td>—</td>
<td>Example Request:<br>
<pre>GET /rest/victims/lastpoints</pre>
Response:<br>
<pre>[<br> {<br> "nodeid":"Alberto",<br> "timestamp":"1300000000000",<br> "msg":"",<br> "latitude":"38.7531",<br> "longitude":"-9.15618",<br> "battery":"92",<br> "steps":"0",<br> "screen":"1",<br> "distance":null,<br> "safe":"0",<br>  },<br> {<br> "nodeid": "Bernardina",<br> "timestamp": 1234567890123,<br> ...&nbsp;<br> },<br> ...<br>]</pre>
</td>
</tr>
<tr>
<td>200</td>
<td>—</td>
<td>Example Request:<br>
<pre>GET /rest/victims/lastpoints/2</pre>
Response:<br>
<pre>[<br> {<br> "nodeid":"Alberto",<br> "timestamp":"1300000000000",<br> "msg":"",<br> "latitude":"38.7531",<br> "longitude":"-9.15618",<br> "battery":"92",<br> "steps":"0",<br> "screen":"1",<br> "distance":null,<br> "safe":"0",<br>  },<br> {<br> "nodeid":"Alberto",<br> "timestamp":"1299999999999",<br> "msg":"",<br> "latitude":"38.7531",<br> "longitude":"-9.15618",<br> "battery":"92",<br> "steps":"0",<br> "screen":"1",<br> "distance":null,<br> "safe":"0",<br>  } {<br> "nodeid": "Bernardina",<br> "timestamp": 1234567890123,<br> ...&nbsp;<br> },<br> ...<br>]</pre>
</td>
</tr>
</tbody>
</table>

<!-- victims/llbbox method -->
<h3 id="rest_victims_llbbox">/rest/victims/llbbox</h3>
Retrieves information about victims' points within a given pair of
coordinates forming a&nbsp;<span style="text-decoration: underline;">l</span>atitude/<span style="text-decoration: underline;">l</span>ongitude <span style="text-decoration: underline;">b</span>ounding <span style="text-decoration: underline;">box</span>.<br>
<h4>Request</h4>
<table class="table">
<tbody>
<tr>
<th>Verb</th>
<th>Parameter</th>
<th>Description</th>
</tr>
<tr>
<td>GET</td>
<td>Exactly two pair of coordinates splited by commas -
lat1,lon1,lat2,lon2</td>
<td>Get the victims' point within the given coordinates of
bounding box. The bouding box is created a follows: the top left point
is composed placed in (lat1,lon1) coordinates, while the bottom right
point is placed in (lat2,lon2).</td>
</tr>
</tbody>
</table>
<h4>Response</h4>
<table class="table">
<tbody>
<tr>
<th>HTTP code</th>
<th>Internal code</th>
<th>Response</th>
</tr>
<tr>
<td>200</td>
<td>—</td>
<td>Example Request:<br>
<pre>GET /rest/victims/llbbox/38.7855,-9.15618,38.7605,-9.145</pre>
Response:<br>
<pre>[<br> {<br> "nodeid":"Alberto",<br> "timestamp":"1300000000000",<br> "msg":"",<br> "latitude":"38.7613",<br> "longitude":"-9.15532",<br> "battery":"92",<br> "steps":"0",<br> "screen":"1",<br> "distance":null,<br> "safe":"0",<br> "added":"1300000000000" <br> },<br> {<br> "nodeid": ...,<br> "timestamp": 1234567890123,<br> ...&nbsp;<br> },<br> ...<br>]</pre>
</td>
</tr>
<tr>
<td>400</td>
<td>802</td>
<td>The bounding box values are incorrectly formatted. You
must pass two pair of coordinates splitted with commas. The . (period)
character must be used as decimal separator for each latitude or
longitude value.</td>
</tr>
</tbody>

<!-- victims/id method -->
</table>
<h3 id="rest_victims_id">/rest/victims/id</h3>
Retrieves all points for a single victim.<br>
<h4>Request</h4>
<table class="table">
<tbody>
<tr>
<th>Verb</th>
<th>Parameter</th>
<th>Description</th>
</tr>
<tr>
<td>GET</td>
<td>victim id (string)</td>
<td>Gets all point information for a single victim ID. The ID is case-insensitive.</td>
</tr>
</tbody>
</table>
<h4>Response</h4>
<table class="table"><tbody><tr>
<th>HTTP code</th>
<th>Internal code</th>
<th>Response</th>
</tr>
<tr>
<td>200</td>
<td>—</td>
<td>Example Request:<br>
<pre>GET /rest/victims/id/alberto</pre>
Response:<br>
<pre>[<br> {<br> "nodeid":"Alberto",<br> "timestamp":"1300000000000",<br> "msg":"",<br> "latitude":"38.7531",<br> "longitude":"-9.15618",<br> "battery":"92",<br> "steps":"0",<br> "screen":"1",<br> "distance":null,<br> "safe":"0",<br> "added":"1300000000000" <br> },<br> {<br> "nodeid": "Alberto",<br> "timestamp": 1234567890123,<br> ...&nbsp;<br> },<br> ...<br>]</pre></td></tr></tbody></table>
</div>
</div>
</body></html>