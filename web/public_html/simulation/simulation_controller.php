<?php
	require_once('loader.php');
	require_once("ATrigger.php");
	
	if ($_SERVER['REQUEST_METHOD'] === 'POST') {
		$name = $_POST["fname"];
		$location = $_POST["location"];
		$sdate = $_POST["sdate"];
		$duration = $_POST["duration"]; 
		$latS = $_POST["latS"];
		$lonS = $_POST["lonS"];
		$latE = $_POST["latE"];
		$lonE = $_POST["lonE"];
		$startIn = $_POST["startIn"];
		$sdate = $sdate .":00"; 
				
		ATrigger::init("4893348271254246192","ZhH4tDt2eJYESp1jO6z6Xu9FRY2Ws3");
	
	
		
		if(createSimulation($name ,$location , $sdate ,$duration ,$latS , $lonS ,$latE , $lonE)){
			echo "Successfully created a new simulation schedule for " . $sdate  ." with " . $duration ." minutes of duration <p>";
			
			/*echo '<script language="javascript">';
			echo 'alert("Successfully created a new simulation schedule for ' . $sdate  .' with ' . $duration .' minutes of duration")';
		//	echo 'window.location.href = "http://accessible-serv.lasige.di.fc.ul.pt/~lost/";';
			echo '</script>';*/
			
			notifyUsersNewSimulation($name ,$location , $sdate ,$duration ,$latS , $lonS ,$latE , $lonE);
	
			$init = array();
			$init['type']=$name . "_init";
			
			$end = array();
			$end['type']=$name . "_end";
			
			$minutes_init = $startIn;
			$minutes_end = $minutes_init + $duration;
			echo $sdate . "  init:" . $minutes_init . " end:" . $minutes_end  ;
			//Create
			//ATrigger::doCreate($minutes_init. "minute", "http://accessible-serv.lasige.di.fc.ul.pt/~lost/index.php/rest/simulations/startSimu/" . $name, $init);
			
			ATrigger::doCreate($minutes_end . "minute", "http://accessible-serv.lasige.di.fc.ul.pt/~lost/index.php/rest/simulations/stop/" . $name, $end);
			
		}else
			echo "Could not schedule the simulation please try again";
		
		ob_start(); // ensures anything dumped out will be caught

		// do stuff here
		$url = 'http://accessible-serv.lasige.di.fc.ul.pt/~lost/'; // this can be set based on whatever

		// clear out the output buffer
		while (ob_get_status()) 
		{
			ob_end_clean();
		}
		// no redirect
		header( "Location: $url" );
	}
?>