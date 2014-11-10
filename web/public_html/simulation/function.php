<?php
 

   //Storing new user and returns user details
   function storeUser($mac, $gcm_regid) {
        // insert user into database
        $result = mysql_query(
                      "INSERT INTO gcm_users
                            (gcm_regid, created_at, test, mac_address, storage, association) 
                            VALUES
                            ('$gcm_regid', 
                             NOW(), '', '$mac',2,1)");
         
        // check for successful store
        if ($result) {
             
            // get user details
            $id = mysql_insert_id(); // last inserted id
            $result = mysql_query(
                               "SELECT * 
                                     FROM gcm_users 
                                     WHERE id = $id") or die(mysql_error());
            // return user details 
            if (mysql_num_rows($result) > 0) { 
                return mysql_fetch_array($result);
            } else {
                return false;
            }
             
        } else {
            return false;
        }
    }
	
	function registerParticipants($ids , $name){
	
		foreach($ids as &$value){
			$result = mysql_query(
                      "UPDATE gcm_users set test='$name' where id='$value'
                           ");
		   $result2 = mysql_query(
						"INSERT INTO user_simulation (mac_address, simulation_name) VALUES( '$value', '$name')
							");
		}
	}
	
	 /**
     * Get user by email
     */
	   function getUserByEmail($email) {
			$result = mysql_query("SELECT * 
										FROM gcm_users 
										WHERE email = '$email'
										LIMIT 1");
			return $result;
		}
	 
		// Getting all registered users
	  function getAllUsers() {
			$result = mysql_query("select * 
										FROM gcm_users");
			return $result;
		}
	 
		// Validate user
	  function isUserExisted($email) {
			$result    = mysql_query("SELECT email 
										   from gcm_users 
										   WHERE email = '$email'");
											
			$NumOfRows = mysql_num_rows($result);
			if ($NumOfRows > 0) {
				// user existed
				return true;
			} else {
				// user not existed
				return false;
			}
		}
		
	//Storing new user and returns user details
	
   function createSimulation($name,$location, $start_date, $duration, $latS, $lonS , $latE, $lonE) {

		$migratePoints = mysql_query(
                      "insert legacy_points select * from points ");
		$clearCurrentPoints = mysql_query(
                      "truncate points ");
					  
		$result = mysql_query(
                      "INSERT INTO Simulations
                            (name, location, status, latS, lonS, latE, lonE, start_date, duration_m) 
                            VALUES
                            ('$name','$location' 
                             , 1, '$latS', '$lonS', '$latE', '$lonE', '$start_date', '$duration')");
							 
      
        // check for successful store
        if ($result) {
             
            // get user details
            $id = mysql_insert_id(); // last inserted id
            $result = mysql_query(
                               "SELECT * 
                                     FROM Simulations 
                                     WHERE id = $id") or die(mysql_error());
            // return user details 
            if (mysql_num_rows($result) > 0) { 
                return mysql_fetch_array($result);
            } else {
                return false;
            }
             
        } else {
            return false;
        }
    }
			
	function notifyUsersNewSimulation($name,$location, $start_date, $duration, $latS, $lonS , $latE, $lonE){
		$NOTIFICATION=0;
		$POP_UP =1;
	
		/*$auto_ids = array();
		$auto_regid = array();
		$messageAuto = array("type" => $NOTIFICATION, "name" => $name, "location"=> $location, "date"=> $start_date, "duration" => $duration, "latS" => $latS , "lonS" => $lonS, "latE" => $latE, "lonE" => $lonE);
		$autoUsers = mysql_query(
                               "SELECT gcm_regid, id, mac_address 
                                     FROM gcm_users 
                                     WHERE association = 1 AND test=''") or die(mysql_error());
		while ($row = mysql_fetch_array($autoUsers, MYSQL_NUM)) {
			array_push ($auto_regid,$row [0]);
			array_push ($auto_ids,$row [1]);

		}
		send_push_notification($auto_regid, $messageAuto);
		registerParticipants($auto_ids , $name);*/

		$pop_ids = array();
		$messagePop = array("type" => $POP_UP, "name" => $name, "location"=> $location, "date"=> $start_date, "duration" => $duration, "latS" => $latS , "lonS" => $lonS, "latE" => $latE, "lonE" => $lonE);
		$popUsers = mysql_query(
                               "SELECT gcm_regid ,id
                                     FROM gcm_users 
                                     WHERE association >0 AND test=''") or die(mysql_error());
		while ($row = mysql_fetch_array($popUsers, MYSQL_NUM)) {
			array_push ($pop_ids,$row [0]);
			echo "sending to" . $row [0];
		}
		send_push_notification($pop_ids, $messagePop);
		
	}
     
    //Sending Push Notification
   function send_push_notification($registatoin_ids, $message) {
 
        // Set POST variables
        $url = 'https://android.googleapis.com/gcm/send';
 
        $fields = array(
            'registration_ids' => $registatoin_ids,
            'data' => $message,
        );
 
        $headers = array(
            'Authorization: key=' . GOOGLE_API_KEY,
            'Content-Type: application/json'
        );
       // print_r($headers);
		// print_r($fields);
        // Open connection
        $ch = curl_init();
 
        // Set the url, number of POST vars, POST data
        curl_setopt($ch, CURLOPT_URL, $url);
 
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
 
        // Disabling SSL Certificate support temporarly
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
 
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fields));
 
        // Execute post
        $result = curl_exec($ch);
        if ($result === FALSE) {
            die('Curl failed: ' . curl_error($ch));
        }
 
        // Close connection
        curl_close($ch);
        //echo $result;
    }
?>