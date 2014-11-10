<?php

/**
 * Model to read/write victim points from the database
 */
class Points extends CI_Model {

    function __construct()
    {
        // Call the Model constructor
        parent::__construct();
    }
	
	/**
	 * Gets the victim points for a given criteria
	 * @param minTimestamp Minimum timestamp to return points (default: no points filtered by timestamp)
	 * @param lattop Latitude from top left corner of bouding box (default: do not set bounding box)
	 * @param lontop Longitude from top left corner of bouding box (default: do not set bounding box)
	 * @param latbottom Latitude from bottom right corner of bouding box (default: do not set bounding box)
	 * @param lonbottom Longitude from bottom right corner of bouding box (default: do not set bounding box)
	 * @return array containing point information
	 */
	function get($minTimestamp = 0, $lattop = NULL, $lontop = NULL, $latbottom = NULL, $lonbottom = NULL)
	{
		$this->load->database();
		if($lattop !== NULL && $lontop !== NULL && $latbottom !== NULL && $lonbottom !== NULL)
		{
			$this->db->where('latitude >=', $latbottom);
			$this->db->where('latitude <=', $lattop);
			$this->db->where('longitude >=', $lontop);
			$this->db->where('longitude <=', $lonbottom);
		}
		$q = $this->db->where('added >=', $minTimestamp)->
				order_by('nodeid asc, timestamp desc')->
				get('points');
				
		$result = $q->result_array();
		if($lattop !== NULL && $lontop !== NULL && $latbottom !== NULL && $lonbottom !== NULL)
		{
			$this->db->where('latitude >=', $latbottom);
			$this->db->where('latitude <=', $lattop);
			$this->db->where('longitude >=', $lontop);
			$this->db->where('longitude <=', $lonbottom);
		}
		$q1 = $this->db->where('added >=', $minTimestamp)->
				order_by('nodeid asc, timestamp desc')->
				get('legacy_points');
		$result2 = $q1->result_array();
		$result = array_merge($result, $result2);
				
		return $result;
	}
	
	/**
	 * Gets all points from victims in ascending order
	 * @param minTimestamp Minimum timestamp to return the point (optional)
	 * @return array containing point information
	 */
	function get_ascending($minTimestamp=-1)
	{
		$this->load->database();
		$q = $this->db->from('points')->where('timestamp >=', $minTimestamp)->
				order_by('timestamp asc')->get();
		$result = $q->result_array();
		$q1 = $this->db->from('legacy_points')->where('timestamp >=', $minTimestamp)->order_by('timestamp asc')->get();
		$result2= $q1->result_array();
		$result = array_merge($result, $result2);
		return $result;
	}
	/**
	 * Gets all points from victims registered in the simulation
	 * @return array containing point information
	 */
	function getVictimsSimulation($simulation, $status=1 , $minTimestamp=-1)
	{		
		$this->load->database();
		if($minTimestamp==0){

			$q = $this->db->select('start_date')->from('Simulations')->where('name =',$simulation) ->get();
			$aux ="";
			foreach ($q->result_array() as $row)
			{
				$aux= $row['start_date'];
			}

			$date = new DateTime($aux);
			$minTimestamp= $date->getTimestamp()*1000;
		}
		if($status==3){
			$q = $this->db->query('SELECT  *
									FROM legacy_points 
										WHERE simulation=?',array($simulation));
		}else{
			$q = $this->db->query('SELECT  *
									FROM points 
										');
		}
		//$q = $this->db->from('points')->where('timestamp >=', $minTimestamp)->
			//	order_by('timestamp asc')->get();
		
		return $q->result_array();
	}
	
	function getRescueView($lattop = NULL, $lontop = NULL, $latbottom = NULL, $lonbottom = NULL, $minTimestamp = 0)
	{
		$this->load->database();
		if($lattop !== NULL && $lontop !== NULL && $latbottom !== NULL && $lonbottom !== NULL)
		{
			$this->db->where('latitude >=', $latbottom);
			$this->db->where('latitude <=', $lattop);
			$this->db->where('longitude >=', $lontop);
			$this->db->where('longitude <=', $lonbottom);
		}
		$q = $this->db->where('added >=', $minTimestamp)->
				order_by('nodeid asc, timestamp desc')->
				get('points');
		
		
		$result = $q->result_array();
	
		return $result;
	}
	
	/**
	 * Gets the last points for all victims
	 * @param limit The limit of results to retrieve (only the very last result by default)
	 * @return array of points from the victims
	 */
	function get_last_all($limit=1)
	{
		$this->load->database();
		$q = $this->db->query('
			SELECT * FROM(
				SELECT a.* FROM points AS a
				LEFT JOIN points AS a2
				ON a.nodeid = a2.nodeid AND a.timestamp <= a2.timestamp
				GROUP BY nodeid, timestamp
				HAVING COUNT(*) <= ?) a
			ORDER BY nodeid ASC, timestamp DESC',
			$limit
		);
		
		return $q->result_array();
	}
	
	/**
	 * Gets all points for a given victim
	 * @param nodeid Node identificator for victim
	 * @param limit The limit of results to retrieve (all results by default)
	 * @return list of points for the specified victim
	 */
	function get_from($nodeid, $limit=0)
	{
		$this->load->database();
		$q = $this->db->from('points')->where('nodeid =', $nodeid )->
			order_by('timestamp desc');
			
		if( $limit > 0 )
			$q = $q->limit($limit);
			
		$q = $q->get();
		
		return $q->result_array();
	}
	
	/**
	 * Delete all points from the database
	 */
	function truncate()
	{
		$this->load->database();
		$this->db->truncate('points');
	}
	
	/**
	 * Inserts a new point in the database
	 * @param data A single point containing the usual point information
	 * @return True if point was inserted; False otherwise
	 */
	function insert($data, $mode)
	{
		if(empty($data))
			return FALSE;
			
		$data->distance = NULL;  // set distance to NULL; must be calculated by client
		$data->added = time() * 1000;
		$this->load->database();
		$this->db->set($data);
		if($mode=="legacy"){
			$this->db->insert('legacy_points');
		}
		else{
			$this->db->insert('points');
		}
		return $this->db->affected_rows() == 1;
	}
	

}

/* End of file points.php */
/* Location: ./application/models/points.php */
