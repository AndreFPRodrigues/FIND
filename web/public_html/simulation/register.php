<?php
require_once('loader.php');
 
// return json response 
$json = array();
 
 
 // GCM Registration ID got from device
$gcmRegID  = $_POST["regId"]; 
$mac  = $_POST["mac"];
 

 
/**
 * Registering a user device in database
 * Store reg id in users table
 */
if (isset($mac)  
     && isset($gcmRegID)) {
     
    // Store user details in db
    $res = storeUser($mac, $gcmRegID);
 
  /*  $registatoin_ids = array($gcmRegID);
    $message = array("product" => "shirt");
 
    $result = send_push_notification($registatoin_ids, $message);
 
    echo $result;*/
} else {
    echo "errros";
}
?>