<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<base href="<?php echo base_url() ?>" />
	
	<title>LOST Map &#8211; Demo Manager</title>

	<link rel="stylesheet" type="text/css" href="assets/css/bootstrap.min.css">
	<script type="text/javascript" src="assets/js/jquery-1.11.0.min.js"></script>
	<script>
		$(document).ready(function() {
			$('#formLoad').submit(function() {
				if($('#fileDemo').val() == ''){
					alert('Select a demo file to upload')
					return false;
				}
				
				// lock button before submit the form
				$('#btnLoad').html('Sending demo data...');
				$('#btnLoad').attr('disabled','disabled');
			});
		});
	</script>
</head>
<body>
	
	<div class="container">
		<div class="page-header">
			<h1>LOST Map &#8211; Demo Manager</h1>
			<p>With Demo Manager, it's possible to load or save demo files for future reference.</p>
			<p><a href=".">Cancel and go to map</a></p>
		</div>
		
		<div>
			<h2>Load a demo</h2>
			<p>
				You can load a demo to the system in order to show on the map the situation. 
				When you upload a demo, the current contents of the map will be removed.
			</p>
			<form id="formLoad" action="index.php/demo/load" class="form-inline" method="post" enctype="multipart/form-data">
				<div class="form-group">
					<label for="fileDemo">Demo file (CSV format)</label> 
					<input id="fileDemo" type="file" name="userfile"  />
					<button type="submit" id="btnLoad">Load Demo</button>
				</div>
			</form>
			
			<!--<h2>Save a demo</h2>
			<p>
				Saving a demo allows you to export the current data to a file. It can be loaded in 
				the future using this panel. A CSV file will be generated immediately, which you can download 
				and save for later.
			</p>
			<form action="index.php/demo/save">
				<button type="submit" class="btn btn-default">Save current data to Demo</button>
			</form>-->
		</div>
	</div>
</body>
</html>