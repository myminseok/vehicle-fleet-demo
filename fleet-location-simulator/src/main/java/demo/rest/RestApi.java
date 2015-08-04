/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package demo.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.springframework.aop.support.Pointcuts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.GpsSimulator;
import demo.GpsSimulatorRequest;
import demo.model.DirectionInput;
import demo.model.GpsSimulatorInstance;
import demo.model.Point;
import demo.model.PositionInfo.VehicleStatus;
import demo.model.SimulatorFixture;
import demo.service.GpsSimulatorFactory;
import demo.service.KmlService;
import demo.service.PathService;
import demo.support.NavUtils;

/**
 *
 * @author Gunnar Hillert
 *
 */
@RestController
@RequestMapping("/api")
public class RestApi {

	@Autowired
	private PathService pathService;

	@Autowired
	private KmlService kmlService;

	@Autowired
	private GpsSimulatorFactory gpsSimulatorFactory;

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	private Map<Long, GpsSimulatorInstance> taskFutures = new HashMap<>();

	@RequestMapping("/dc")
	public List<GpsSimulatorInstance>dc() {
		final SimulatorFixture fixture = this.pathService.loadSimulatorFixture();

		final List<GpsSimulatorInstance> instances = new ArrayList<>();
		final List<Point> lookAtPoints = new ArrayList<>();

		final Set<Long> instanceIds = new HashSet<>(taskFutures.keySet());

		for (GpsSimulatorRequest gpsSimulatorRequest : fixture.getGpsSimulatorRequests()) {

			final GpsSimulator gpsSimulator = gpsSimulatorFactory.prepareGpsSimulator(gpsSimulatorRequest);
			lookAtPoints.add(gpsSimulator.getStartPoint());
			instanceIds.add(gpsSimulator.getId());

			final Future<?> future = taskExecutor.submit(gpsSimulator);
			final GpsSimulatorInstance instance = new GpsSimulatorInstance(gpsSimulator.getId(), gpsSimulator, future);
			taskFutures.put(gpsSimulator.getId(), instance);
			instances.add(instance);
		}

		if (fixture.usesKmlIntegration()) {
			kmlService.setupKmlIntegration(instanceIds, NavUtils.getLookAtPoint(lookAtPoints));
		}

		return instances;
	}

	@RequestMapping("/status")
	public Collection<GpsSimulatorInstance> status() {
		return taskFutures.values();
	}

	@RequestMapping("/cancel")
	public int cancel() {
		int numberOfCancelledTasks = 0;
		for (Map.Entry<Long, GpsSimulatorInstance> entry : taskFutures.entrySet()) {
			GpsSimulatorInstance instance = entry.getValue();
			instance.getGpsSimulator().cancel();
			boolean wasCancelled = instance.getGpsSimulatorTask().cancel(true);
			if (wasCancelled) {
				numberOfCancelledTasks++;
			}
		}
		taskFutures.clear();
		return numberOfCancelledTasks;
	}

	@RequestMapping("/directions")
	public List<DirectionInput> directions() {
		return pathService.loadDirectionInput();
	}

	@RequestMapping("/fixture")
	public SimulatorFixture fixture() {

		final List<DirectionInput> directions = this.pathService.loadDirectionInput();
		final SimulatorFixture fixture = new SimulatorFixture();

		for (DirectionInput directionInput : directions) {

			final GpsSimulatorRequest gpsSimulatorRequest = new GpsSimulatorRequest();
			gpsSimulatorRequest.setExportPositionsToKml(true);
			gpsSimulatorRequest.setExportPositionsToMessaging(true);
			gpsSimulatorRequest.setMove(true);

			String polyline = this.pathService.getCoordinatesFromGoogleAsPolyline(directionInput);
			gpsSimulatorRequest.setPolyline(polyline);
			gpsSimulatorRequest.setReportInterval(1000);
			gpsSimulatorRequest.setSpeedInKph(50d);
			gpsSimulatorRequest.setExportPositionsToMessaging(true);
			gpsSimulatorRequest.setSecondsToError(60);
			gpsSimulatorRequest.setVehicleStatus(VehicleStatus.NORMAL);
			fixture.getGpsSimulatorRequests().add(gpsSimulatorRequest);
		}

		return fixture;
	}

	@RequestMapping("/kml/{instanceId}")
	public byte[] getKmlInstance(@PathVariable Long instanceId) {
		return kmlService.getKmlInstance(instanceId);
	}
}