<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <title>Contact with Map Template | PrepBootstrap</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />

    <link rel="stylesheet" type="text/css" href="bootstrap/css/bootstrap.min.css" />
    <link rel="stylesheet" type="text/css" href="font-awesome/css/font-awesome.min.css" />

    <script type="text/javascript" src="js/jquery-1.10.2.min.js"></script>
    <script type="text/javascript" src="bootstrap/js/bootstrap.min.js"></script>
</head>
<body>

<div class="container"> 

<div class="page-header">
    <h1>Create a simulation </h1>
</div>

<!-- Contact with Map - START -->
<div class="container">
    <div class="row">
        <div class="col-md-6">
            <div class="well well-sm">
                <form name="myForm" class="form-horizontal" onsubmit="return validateForm()" action="simulation_controller.php" method="post">
                    <fieldset>
                        <div class="form-group">
						
                            <div class="col-md-10 col-md-offset-1">
															<h4> Name</h4>

                                <input id="fname" name="fname" type="text" placeholder="Name" class="form-control">
                            </div>
                        </div>
						
						<div class="form-group">
                            <div class="col-md-10 col-md-offset-1">
								<h4> Location name</h4>
                                <input id="location" name="location" type="text" placeholder="location" class="form-control">
                            </div>
                        </div>
                        <div class="form-group">
                            <div class="col-md-10 col-md-offset-1">
								<h4> Starting Date and time</h4>
                                <input id="sdate" name="sdate" type="text" placeholder="YYYY/MM/DD HH:MM" class="form-control">
                            </div>
                        </div>
						
					

                        <div class="form-group">
                            <div class="col-md-10 col-md-offset-1">
                                <h4> Duration</h4>
                                <input id="duration" name="duration" type="text" placeholder="HH:MM" class="form-control">
                            </div>
                        </div>
                                <input id="latS" name="latS" type="hidden" value="38.744063"  class="form-control">
								<input id="lonS" name="lonS" type="hidden" value="-9.161049" class="form-control">                                
								<input id="latE" name="latE" type="hidden"  value="38.709244" class="form-control">
								<input id="lonE" name="lonE" type="hidden"  value="-9.135062" class="form-control">	
								<input id="startIn" name="startIn" type="hidden"  value="" class="form-control">						
								
                        
                        <div class="form-group">
                            <div class="col-md-12 text-center">
                                <button type="submit" class="btn btn-primary btn-lg">Submit</button>
                            </div>
                        </div>
                    </fieldset>
                </form>
            </div>
        </div>
        <div class="col-md-6">
            <div>
                <div class="panel panel-default">
                    <div class="text-center header">Define the area of the simulation</div>
                    <div class="panel-body text-center">
                       
                        <hr />
                        <div id="map1" class="map">
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>


<script src="http://maps.google.com/maps/api/js?sensor=false"></script>

<script type="text/javascript">
	var map;
	var rectangle;
	var latS = document.getElementById("latS");
	var latE = document.getElementById("latE");
	var lonS = document.getElementById("lonS");
	var lonE = document.getElementById("lonE");
	 
	function empty( name,element){
		if (element==null || element=="") {
			alert( name + " must be filled out");
			return true;
		}
	}
	function validateDuration(dt) {
		var isValidDuration = false;

		try {
		    var arr1 = dt.split(':');
			var hour = parseInt(arr1[0],10);
            var minute = parseInt(arr1[1],10);
			if(minute >=0 && minute<=59){
                isValidDuration=true;
				duration.value= minute+ (60*hour);
			}
		}
        catch(er){isValidDuration = false;}
		return isValidDuration;
	}
	function validateDate(dt) {
        try {
            var isValidDate = false;
            var arr1 = dt.split('/');
            var year=0;var month=0;var day=0;var hour=0;var minute=0;var sec=0;
            if(arr1.length == 3)
            {
                var arr2 = arr1[2].split(' ');
                if(arr2.length == 2)
                {
                    var arr3 = arr2[1].split(':');
                    try{
                        year = parseInt(arr1[0],10);
                        month = parseInt(arr1[1],10);
                        day = parseInt(arr2[0],10);
                        hour = parseInt(arr3[0],10);
                        minute = parseInt(arr3[1],10);
                        //sec = parseInt(arr3[0],10);
                        sec = 0;
                        var isValidTime=false;
                        if(hour >=0 && hour <=23 && minute >=0 && minute<=59 && sec >=0 && sec<=59)
                            isValidTime=true;
                        else if(hour ==24 && minute ==0 && sec==0)
                            isValidTime=true;

                        if(isValidTime)
                        {
                            var isLeapYear = false;
                            if(year % 4 == 0)
                                 isLeapYear = true;

                            if((month==4 || month==6|| month==9|| month==11) && (day>=0 && day <= 30))
                                    isValidDate=true;
                            else if((month!=2) && (day>=0 && day <= 31))
                                    isValidDate=true;

                            if(!isValidDate){
                                if(isLeapYear)
                                {
                                    if(month==2 && (day>=0 && day <= 29))
                                        isValidDate=true;
                                }
                                else
                                {
                                    if(month==2 && (day>=0 && day <= 28))
                                        isValidDate=true;
                                }
                            }
                        }
                    }
                    catch(er){isValidDate = false;}
                }

            }

            return isValidDate;
        }
        catch (err) { alert('ValidateDate: ' + err); }
    }
	
	function validateForm() {
		var name = document.forms["myForm"]["fname"].value;
		var location = document.forms["myForm"]["location"].value;
		var sdate = document.forms["myForm"]["sdate"].value;
		var duration = document.forms["myForm"]["duration"].value;
		var startIn = document.getElementById("startIn");;

		if (empty("Nome", name)) {
			return false;
		}
		 if (empty("Location", location)) {
			return false;
		}
		if (empty("Starting Date", sdate)) {
			return false;
		}
		if ( !validateDate(sdate)) {
			alert( "Please fill the date field with the correct format");
			return false;
		}
		var currentdate = new Date(); 
		var datetime =   currentdate.getFullYear()  + "/"
                + (currentdate.getMonth()+1)  + "/" 
				+ currentdate.getDate() +  " "  
                + currentdate.getHours() + ":"  
                + currentdate.getMinutes();

		if (Date.parse(sdate)< Date.parse(datetime)) {
			alert( "Please fill the date field with a future date");
			return false;
		}
		
		if (empty("Duration ", duration)) {
			return false;
		}
		if (!validateDuration(duration)) {
			alert( "Please fill the duration field with the correct format");
			return false;
		}
				
		var diff = Math.abs(Date.parse(sdate) - Date.parse(datetime));
		var minutes = Math.floor((diff/1000)/60);
		startIn.value=minutes;

		if(minutes<1){
			return false;
		}
		var dist = getDistanceFromLatLonInKm(latS.value,lonS.value,latE.value,lonE.value );
		if(dist>10){
			var r = confirm("You selected a big simulation area. Are you sure you want to continue?");
			return r;
		}
			

	
	}
	
	function getDistanceFromLatLonInKm(lat1,lon1,lat2,lon2) {
		var R = 6371; // Radius of the earth in km
		var dLat = deg2rad(lat2-lat1);  // deg2rad below
		var dLon = deg2rad(lon2-lon1); 
		var a = 
		Math.sin(dLat/2) * Math.sin(dLat/2) +
		Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * 
		Math.sin(dLon/2) * Math.sin(dLon/2)
		; 
		var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
		var d = R * c; // Distance in km
		return d;
	}

	function deg2rad(deg) {
	  return deg * (Math.PI/180)
	}
	
    jQuery(function ($) {
        function init_map1() {
            var topSide = new google.maps.LatLng(38.744063, -9.161049);
			var botSide = new google.maps.LatLng(38.709244, -9.097792);
			var center = new google.maps.LatLng(38.724354, -9.135062);
			
            var mapOptions = {
                center: center,
                zoom: 14,
				disableDefaultUI: true,
				zoomControl: true				
            };
           
			map = new google.maps.Map(document.getElementById("map1"),
                mapOptions);
				
			 if (navigator.geolocation) {
				 navigator.geolocation.getCurrentPosition(function (position) {
					initialLocation = new google.maps.LatLng(position.coords.latitude, position.coords.longitude);
					var topSide=new google.maps.LatLng((position.coords.latitude-0.005), position.coords.longitude-0.01);
					var botSide = new google.maps.LatLng((position.coords.latitude+0.005), position.coords.longitude+0.01);
					map.setCenter(initialLocation);
					
					rectangle = new google.maps.Rectangle({
						editable: true,
						draggable:true,
						bounds: new google.maps.LatLngBounds(
							topSide,
							botSide)
					});

					 rectangle.setMap(map);
			  		google.maps.event.addListener(rectangle, 'bounds_changed', showNewRect);

				
				 });
				 function showNewRect(event) {
					var ne = rectangle.getBounds().getNorthEast();
					var sw = rectangle.getBounds().getSouthWest();
					  latS.value= ne.lat(); 
					  lonS.value= ne.lng(); 
					  latE.value= sw.lat(); 
					  lonE.value= sw.lng(); 
			  
				}
			 }			
        }			
		google.maps.event.addDomListener(window, 'load', init_map1);
		
	});
	
	
</script>

<style>
    .map {
        min-width: 300px;
        min-height: 300px;
        width: 100%;
        height: 100%;
    }

    .header {
        background-color: #F5F5F5;
        color: #36A0FF;
        height: 70px;
        font-size: 27px;
        padding: 10px;
    }
</style>

<!-- Contact with Map - END -->

</div>

</body>
</html>