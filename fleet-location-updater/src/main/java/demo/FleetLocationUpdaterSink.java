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
package demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableModule;
import org.springframework.cloud.stream.annotation.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Spring Cloud Stream {@link Sink}, responsible for sending current position data to connected
 * Websocket clients.
 *
 * @author Gunnar Hillert
 *
 */
@EnableModule(Sink.class)
public class FleetLocationUpdaterSink {

	@Autowired
	MessageChannel input;

	@Autowired
	private SimpMessagingTemplate template;

	@Transformer(inputChannel="input", outputChannel="sendToBroker")
	public String addServiceLocations(String payload) {
		return payload;
	}

	@Bean
	public MessageChannel sendToBroker() {
		return new DirectChannel();
	}

	@ServiceActivator(inputChannel="sendToBroker")
	public void sendToStompClients(String payload) {
		this.template.convertAndSend("/queue/fleet.location.ingest.queue", payload);
	}
}
