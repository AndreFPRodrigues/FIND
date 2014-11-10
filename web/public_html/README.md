LOST MAP
========

LOST Map is a tool which interacts with LOST Wifi generated data. It shows information gathered on a map along statistics and other useful information. It also allows the use of filters to present the information dynamically.

A webservice is also included on this project. It allows read/write access to the victims' information.

# Setup

In order to make LOST Map running, you will need the following requirements:

 * Apache Webserver with PHP 5 support
 * MySQL instance

## Installing database

Create a database of your choice and run `wifioppish-map.sql`. It will create the needed tables.

## Change CodeIgniter configuration

You will need to change the database configuration according to the setup of the previous step. Change the file `application/config/database.php`

## Deploy to webserver

Copy the files to the www directory and test in the webbrowser. The application should be scanning for points, since there are none. 

## Manually populate database

You can send machine-generated test data in order to test the application.

Open a webbrowser and point to `http://example.com/index.php/demo` where `example.com` is the address of your webserver. You should see LOST Map - Demo Manager. You can then upload one of the test files present on `examples` directory. The application should redirect to the map and show your test data dynamically.

# Webservice

This project also contains a webservice with read/write access to victims' information. The webservice is available at `http://example.com/index.php/rest` where `example.com` is the address of your webserver. The page contains dedicated documentation on how to use the webservice and which methods are available.
