<?php if ( ! defined('BASEPATH')) exit('No direct script access allowed');

class Api extends CI_Controller {

	/**
	 * Allows users to put or get data from database
	 */
	public function index()
	{	
		$this->output->set_content_type('text/plain');
		$method = $this->input->server('REQUEST_METHOD');
		
		switch($method)
		{
			case 'GET':
			$this->_doGet();
			break;
			
			case 'POST':
			$this->_doPost();
			break;
			
			default:
			echo 'Invalid HTTP request (possibilities are GET, POST)';
		}
	}
	
	/**
	 * Allows user to retrieve information from database
	 */
	private function _doGet()
	{
		$params = array('action', 'timestamp');
		$actions = array('points');
		
		$errors = FALSE;
		foreach($params as $p)
		{
			$r = $this->input->get($p);
			if($r === FALSE)
			{
				echo "Parameter $p missing\n";
				$errors = TRUE;
			}
		}
		
		if(!$errors)
		{
			$act = $this->input->get('action');
			
			$contains = FALSE;
			foreach($actions as $a)
			{
				if($a === $act)
				{
					$contains = TRUE;
					break;
				}
			}
			
			if(!$contains)
			{
				echo 'Invalid action';
			}
			else
			{
				$this->output->set_content_type('text/javascript');
				$this->load->model('points');
				$this->load->helper('Points');
				
				// minimum time filter
				$minTimestamp = $this->input->get('timestamp');
				$points = $this->points->get($minTimestamp);
				$resp = points_to_javascript($points);
				
				echo $resp;
			}
		}
	}
	
	/**
	 * Alows user to put information in the database
	 */
	private function _doPost()
	{
		$action = $this->input->get('action');
		
		if($action == 'point')
		{
			// TODO upload single point
			
		}
	}
	
	/**
	 * Deletes all points from the database
	 */
	public function truncate()
	{
		$this->load->model('points');
		$this->points->truncate();
		echo 'All points deleted.';
	}
}

/* End of file api.php */
/* Location: ./application/controllers/api.php */