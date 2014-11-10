<?php

/**
 * Model to read/write victim points from the database
 */
class simulations extends CI_Model {

    function __construct()
    {
        // Call the Model constructor
        parent::__construct();
    }
	
	
	function getActive()
	{
		$this->load->database();
		$q = $this->db->select('*')->from('Simulations')->where('status =',1) ->get();
		
		return $q->result_array();
		
	}
	
	function getRunning()
	{
		$this->load->database();
		$q = $this->db->select('*')->from('Simulations') ->get();
		
		return $q->result_array();
		
	}
	
	function registerParticipant($name, $regid, $mac_address)
	{
		$this->load->database();
		$this->db->where('name =',$name);
		$q = $this->db->get('Simulations');
		
		$simulation=$q->result_array();
		
		if(count($simulation)>0){
			$data=array('test'=>$name);
			$this->db->where('gcm_regid',$regid);
			$this->db->update('gcm_users',$data);
			
			$newData = array(
				   'mac_address' => $mac_address ,
				   'simulation_name' => $name ,
				);
			$this->db->insert('user_simulation', $newData); 

			return true;
		}

		return false;
	}
	
	function unregisterParticipant($mac_address)
	{	
			$this->load->database();
			$data=array('test'=>'');
			$this->db->where('mac_address',$mac_address);
			$this->db->update('gcm_users',$data);
			$this->db->delete('user_simulation', array('mac_address' => $mac_address)); 

			

		return true;
	}
	
	function save_preferences($association, $allow_storage, $regid){
	
		$this->load->database();
		$data=array('association'=> $association);
		$this->db->where('gcm_regid',$regid);
		$this->db->update('gcm_users',$data);
		return true;

	}
	
	function getUserTest($regid=0){
		$this->load->database();
		$q = $this->db->select('test')->from('gcm_users')->where('gcm_regid =',$regid) ->get();
		$name="";
		foreach ($q->result_array() as $row)
		{
			$name= $row['test'];
		}
		$q2 = $this->db->select('name, location, start_date, duration_m')->from('Simulations')->where('name =',$name) ->get();
		return $q2->result_array();
	}
	function startSimulation($name){
		$this->load->database();
		
		$data=array('status'=> 2);
		$this->db->where('name',$name);
		$this->db->update('Simulations',$data);	
		
		$q = $this->db->query('SELECT gcm_regid FROM user_simulation AS us INNER JOIN gcm_users as users
							ON us.mac_address=users.mac_address 
							WHERE us.simulation_name=?', array($name));
		$reg_ids = array();
	
		foreach ($q->result_array() as $row){
			array_push($reg_ids, $row['gcm_regid']);
		}
		$messageStop = array("type" => '2');
				$this->simulations->send_push_notification($reg_ids,$messageStop);
		return $reg_ids;
	}

	
	function stopSimulation($name){
		$this->load->database();
		
		$data=array('status'=> 3);
		$this->db->where('name',$name);
		$this->db->update('Simulations',$data);
		
		$data=array('test'=> '');
		$this->db->where('test',$name);
		$this->db->update('gcm_users',$data);
		
		
		$q = $this->db->query('SELECT gcm_regid FROM user_simulation AS us INNER JOIN gcm_users as users
							ON us.mac_address=users.mac_address 
							WHERE us.simulation_name=?', array($name));
		$reg_ids = array();
	
		foreach ($q->result_array() as $row){
			array_push($reg_ids, $row['gcm_regid']);
		}
		$messageStop = array("type" => '3');
				$this->simulations->send_push_notification($reg_ids,$messageStop);
		
		$q = $this->db->query('UPDATE points set simulation=? where simulation is NULL	', array($name));
		$q = $this->db->query('INSERT INTO legacy_points select * from points');
		$q = $this->db->query('TRUNCATE points');

		return $reg_ids;
	}
	
	function deletePoints($regid){
		$this->load->database();
		$q = $this->db->select('mac_address')->from('gcm_users')->where('gcm_regid =',$regid) ->get();
		$mac_address="";
		foreach ($q->result_array() as $row)
		{
			$mac_address= $row['mac_address'];
		}
		$this->db->where('nodeid', $mac_address);
		$this->db->delete('points'); 
		
		return $mac_address;
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
	
	
}
