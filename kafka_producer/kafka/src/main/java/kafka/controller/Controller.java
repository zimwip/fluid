/*
 * Copyright 2012-2016 the original author or authors.
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
package kafka.controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import kafka.services.KafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

@Component
@Path("/")
public class Controller {

    @Autowired
    private KafkaProducer producer;

    @GET
    @Path("/start")
    @Produces("application/json")
    public Response start() {
        producer.start();
        return Response.ok("Starting producer").build();
    }
    
        @GET
    @Path("/send")
    @Produces("application/json")
    public Response send() {
        producer.send();
        return Response.ok("Sending message").build();
    }
    
            @GET
    @Path("/config")
    @Produces("application/json")
    public Response config() {
        producer.config();
        return Response.ok("Config Done").build();
    }

    @GET
    @Path("/stop")
    @Produces("application/json")
    public Response stop() {
        producer.stop();
        return Response.ok("Stoping producer").build();
    }

}
