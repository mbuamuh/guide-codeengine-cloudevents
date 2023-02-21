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
package io.openliberty.guides.system;

import java.net.URI;
import java.util.Map;

// CloudEvent
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
// CDI
import jakarta.enterprise.context.RequestScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.GET;
// JAX-RS
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RequestScoped
@Path("/properties")
public class SystemResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public CloudEvent getProperties() {
		/*
		 * java.util.properties does not have a direct way to obtain a byte[] so store
		 * in an intermediary Map first
		 */
		Map<?, ?> properties = System.getProperties();
		Jsonb jsonb = JsonbBuilder.create();
		/*
		 * convert properties map into a JSON string which can then be converted into a
		 * byte[]
		 */
		String jsonString = jsonb.toJson(properties);

		/* convert system properties to byte[] */
		byte[] byteProperties = jsonString.getBytes();

		return CloudEventBuilder.v1().withData(byteProperties).withDataContentType("application/json")
				.withId("properties").withType("java.properties").withSource(URI.create("http://system.properties"))
				.build();
	}
}
