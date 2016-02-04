#!/bin/bash

echo "Loading data..."
# Load data
if ! [ -e "fleet.json" ]; then 
	wget http://assets.springone2gx2015.s3.amazonaws.com/fleet/fleet.json
fi
curl -s -X POST http://fleet-location-service.local.micropcf.io/purge
curl -sH "Content-Type: application/json" http://fleet-location-service.local.micropcf.io/fleet -d @fleet.json

if ! [ -e "serviceLocations.json" ]; then 
	wget http://assets.springone2gx2015.s3.amazonaws.com/fleet/serviceLocations.json
fi
curl -s -X POST http://service-location-service.local.micropcf.io/purge
curl -sH "Content-Type: application/json" http://service-location-service.local.micropcf.io/bulk/serviceLocations -d @serviceLocations.json

echo "Starting simulator..."
# Start the simulator
curl -s http://fleet-location-simulator.local.micropcf.io/api/cancel > /dev/null
curl -s http://fleet-location-simulator.local.micropcf.io/api/dc > /dev/null
echo "**** Vehicle Fleet Demo is running on http://fleet-dashboard.local.micropcf.io "


