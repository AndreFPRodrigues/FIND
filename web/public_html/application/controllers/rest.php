<?php if ( ! defined('BASEPATH')) exit('No direct script access allowed');

require APPPATH.'/libraries/REST_Controller.php';

class Rest extends REST_Controller {

	/**
	 * Documentation page for REST API
	 */
	public function index_get()
	{	
		$this->load->view('api/docs');
	}
	
	/**
	 * Gets information available for victims
	 */
	public function simulations_get($func='all', $params=NULL)
	{
				$this->load->model('simulations');
			
				
				switch($func)
				{
					/**
					 * Default function
					 */
					case 'all':
						$simulations = $simulations = $this->simulations->getActive();
					break;
					
					/**
					 * Function: minimum timestamp
					 */
					case 'user':
						if($params !== "0" && empty($params))
							return $this->response(
								array( 'code'=> 802, 'error' => 'Invalid parameter for operation userTest'),
								400
							);
						$simulations = $simulations = $this->simulations->getUserTest($params);
					break;
					case 'unregister':
						if($params !== "0" && empty($params))
							return $this->response(
								array( 'code'=> 802, 'error' => 'Invalid parameter for operation unregister'),
								400
							);

						$simulations = $simulations = $this->simulations->unregisterParticipant($params);

					break;
					case 'savePreferences':
					
						$top = explode(',', $params, 3);

						if(count($top) != 3)
							return $this->response(
								array( 'code'=> 802, 'error' => 'Invalid parameters for operation save preferences'),
								400
							);
						$simulations = $simulations = $this->simulations->save_preferences($top[0], $top[1], $top[2]);
						
					break;
					case 'running':
						$simulations = $simulations = $this->simulations->getRunning();
					break;
					case 'startSimu':
						if($params !== "0" && empty($params))
								return $this->response(
									array( 'code'=> 802, 'error' => 'Invalid parameter for operation stop'),
									400
								);
						$simulations = $simulations = $this->simulations->startSimulation($params);
					break;
					case 'stop':
						if($params !== "0" && empty($params))
								return $this->response(
									array( 'code'=> 802, 'error' => 'Invalid parameter for operation stop'),
									400
								);
						$simulations = $simulations = $this->simulations->stopSimulation($params);
					break;
					case 'deletePoints':
						if($params !== "0" && empty($params))
								return $this->response(
									array( 'code'=> 802, 'error' => 'Invalid parameter for operation stop'),
									400
								);
						$simulations = $simulations = $this->simulations->deletePoints($params);
						//$simulations="hey";
					break;
				}
				
				return $this->response($simulations, 200);
	}
	
	
	/**
	 * Registers one user in the simulation
	 */
	public function simulations_post()
	{
		$name = $this->post('name');
		$regid = $this->post('regid');
		$mac_address = $this->post('mac_address');

		$this->load->model('simulations');
		$simulations = $this->simulations->registerParticipant($name, $regid, $mac_address);
		return $this->response($simulations,200);
	}
	

	/**
	 * Gets information available for victims
	 */
	public function victims_get($func='all', $params=NULL)
	{
		$this->load->model('points');
		
		$victims = NULL;
		
		switch($func)
		{
			/**
			 * Default function
			 */
			case 'all':
			$victims = $victims = $this->points->get();
			break;
			
			/**
			 * Function: minimum timestamp
			 */
			case 'mintimestamp':
			if($params !== "0" && empty($params))
				return $this->response(
					array( 'code'=> 802, 'error' => 'Invalid parameter for operation mintimestamp'),
					400
				);
			$victims = $this->points->get($params);
			break;
		
			/**
			 * Function: last points of all victims
			 */
			case 'lastpoints':
			$victims = $this->points->get_last_all(empty($params) ? 1 : $params);
			break;
			
			/**
			 * Function: all points for a given victim
			 */
			case 'id':
			$victims = $this->points->get_from($params);
			break;
			
			/**
			 * Function: points inside bounding box
			 */
			case 'llbbox':
			$top = explode(',', $params, 4);
			if(count($top) != 4)
				return $this->response(
					array( 'code'=> 802, 'error' => 'Invalid parameter for operation llbox'),
					400
				);
			
			$victims = $this->points->get(0, $top[0], $top[1], $top[2], $top[3]);
			break;
			
			/**
			 * Function: all points in timestamp order (ascending)
			 */
			case 'ascending':
			$victims = $this->points->get_ascending($params == NULL ? 0 : $params);
			break;
		
			default:
			return $this->response(
				array( 'code'=> 801, 'error' => 'Invalid operation for method victims: ' . $func ),
				400
			);
			
			case 'simulation':
				$aux = explode(',', $params, 3);
				if( strlen($aux[0]) <2 || count($aux)>3 )
					return $this->response(
						array( 'code'=> 802, 'error' => 'Invalid parameter for operation victim simulation'),
						400
					);			
				$victims = $this->points->getVictimsSimulation($aux[0], $aux[1] , $aux[2]);
			break;
			case 'rescue':
				$top = explode(',', $params, 5);
				if(count($top) != 5 )
					return $this->response(
						array( 'code'=> 802, 'error' => 'Invalid parameter for operation rescue'),
						400
					);
				$victims = $this->points->getRescueView($top[0], $top[1], $top[2], $top[3], $top[4]);
			break;
			
			
		}
		
		return $this->response($victims, 200);
	}
	
	/**
	 * Inserts information from victims
	 */
	public function victims_post($func='all')
	{
		// JSON array containing victims' information
		$data = $this->post('data');
		$victims = json_decode($data);
		
		if($victims == NULL)
			return $this->response(
				array( 'code' => 801, 'error' => 'Error while decoding JSON string. Please check the syntax.' ),
				400
			);
		
		$total = is_array($victims) ? count($victims) : 0;
		if($total < 1)
			return $this->response(
				array( 'code'=> 802, 'error' => 'There is no victim information to insert. Ensure that victim information is inside a JSON array.' ),
				400
			);

		// insert points
		$this->load->model('points');
		$inserted = 0;
		for($i = 0; $i < $total; $i++)
			if($this->points->insert($victims[$i], $func))
				$inserted++;
		
		return $this->response(array('sent' => $total, 'inserted' => $inserted), 200);
	}
}

/* End of file rest.php */
/* Location: ./application/controllers/rest.php */
