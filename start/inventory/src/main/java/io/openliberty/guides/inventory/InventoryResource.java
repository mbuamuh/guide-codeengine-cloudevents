// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package io.openliberty.guides.inventory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.openliberty.guides.inventory.client.SystemClient;
import io.openliberty.guides.inventory.model.InventoryList;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RequestScoped
@Path("/systems")
public class InventoryResource {

	@Inject
	InventoryManager manager;

	@Inject
	SystemClient systemClient;

	@POST
	@Path("/{hostname}")
	@Produces(MediaType.APPLICATION_JSON)
	@Timed(name = "queryPropertiesTime", description = "Time needed to query the JVM system properties")
	@Counted(absolute = true, description = "Number of times the JVM system properties are queried")
	public CloudEvent getPropertiesForHost(@PathParam("hostname") String hostname, CloudEvent dataQuery) {

		/**
		 * Query the system application using its hostname and path, to get data from
		 * the application text/html
		 */
		Properties systemAppProperties = systemClient.getProperties(hostname);
		if (systemAppProperties == null) {

			String response = "{ \"error\" : \"Unknown hostname or the system application " + "may not be running on "
					+ hostname + "\" }";
			
			Jsonb jsonb = JsonbBuilder.create();
			String jsonString = jsonb.toJson(response);
			byte[] errorResponse = jsonString.getBytes();
			
			return CloudEventBuilder.v1().withData(errorResponse).withDataContentType("application/json")
					.withId("properties").withType("java.properties").withSource(URI.create("http://inventory.systems"))
					.build();
		}

		/**
		 * Retrieve the data from the dataQuery CloudEvent this is in binary format so
		 * convert it into a standard String It should contain the keys of the
		 * properties from the system application we want to query, and thus return
		 * their values with this method
		 */
		CloudEventData data = dataQuery.getData();
		String jsonStringQuery = new String(data.toBytes(), StandardCharsets.UTF_8);

		/**
		 * Retrieve the queried properties in key, value pairs. If the queried property
		 * doesn't exist in the system application, return the value "property does not
		 * exist for the nonexistent key"
		 */
		Jsonb jsonb = JsonbBuilder.create();
		ArrayList<String> propertyKeys = jsonb.fromJson(jsonStringQuery, ArrayList.class);
		HashMap<String, String> queriedProperties = new HashMap<String, String>();
		for (String key : propertyKeys) {
			String noValue = "The property with key " +key+ " does not exist in the system application";
			queriedProperties.put(key, systemAppProperties.getProperty(key, noValue));
		}

		//Add os.name and user.name system properties 
		queriedProperties.put("os.name", systemAppProperties.getProperty("os.name"));
		queriedProperties.put("user.name", systemAppProperties.getProperty("user.name"));
		
		// Add to inventory
		manager.add(hostname, queriedProperties);

		return CloudEventBuilder.v1().withData(jsonb.toJson(queriedProperties).getBytes())
				.withDataContentType("application/json").withId("properties").withType("java.properties")
				.withSource(URI.create("http://system.poperties")).build();
	}

	/**
	 * List all the properties in the inventory. These are properties that were added to 
	 * @return
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public CloudEvent listContents() {

		Jsonb jsonb = JsonbBuilder.create();
		InventoryList inventoryList = manager.list();
		String systemDataString = jsonb.toJson(inventoryList);

		return CloudEventBuilder.v1().withData(systemDataString.getBytes())
				.withDataContentType("application/json").withId("properties").withType("java.properties")
				.withSource(URI.create("http://system.poperties")).build();
	}

	@POST
	@Path("/reset")
	public void reset() {
		manager.reset();
	}

}
