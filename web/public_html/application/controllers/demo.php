<?php if ( ! defined('BASEPATH')) exit('No direct script access allowed');

class Demo extends CI_Controller {

	public function index()
	{	
		$this->load->view('demo');
	}
	
	public function load()
	{
		// CSV file upload
		$config['upload_path'] = './application/cache/uploads/';
		$config['allowed_types'] = 'txt|csv';
		$config['max_size']	= '2048';
		$config['encrypt_name']	= TRUE;
		$this->load->library('upload', $config);

		if ( ! $this->upload->do_upload())
		{
			$this->output->set_content_type('text/html');
			echo '<p>Error uploading the file</p>';
			/*var_dump($this->upload->display_errors());
			var_dump($this->upload->data());*/
		}
		else
		{
			$data = $this->upload->data();
			$path = $data['full_path'];
			
			// 9 fields
			// - msg
			// - nodeid
			// - timestamp
			// - latitude
			// - longitude
			// - battery
			// - steps
			// - screen
			// - safe (0 or 1)
			$rows = 0;
			if(($file = fopen($path, 'r')) !== FALSE)
			{
				$this->load->database();
				$this->load->model('points');
				// clear old points
				$this->points->truncate();
				
				while(($line = fgetcsv($file, 0, ';')) !== FALSE)
				{
					$num = count($line);
					if($num == 9)
					{
						// create object to store on database
						$point = 
						$res = $this->points->insert(
							(object) array(
								'msg' => $line[0],
								'nodeid' => $line[1],
								'timestamp' => $line[2],
								'latitude' => $line[3],
								'longitude' => $line[4],
								'battery' => $line[5],
								'steps' => $line[6],
								'screen' => $line[7],
								'safe' => $line[8],
								'llconf' => 10
							)
						);
						if($res === TRUE)
							$rows++;
					}
				}
				fclose($file);
				
				// remove uploaded file
				// FIXME this doesn't work, and generates errors in log
				/*$this->load->helper('file');
				delete_files($data['full_path']);*/
				
				// go to the map
				redirect(base_url());
			}
			else
			{
				echo 'Error opening uploaded file';
			}
		}
	}
}

/* End of file demo.php */
/* Location: ./application/controllers/demo.php */