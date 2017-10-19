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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ConnectionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import jersey.repackaged.com.google.common.util.concurrent.Futures;
import kafka.services.KafkaProducer;
import org.fagazi.enedis.DataResponse;
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
    public void send(@Suspended final AsyncResponse asyncResponse) {
        asyncResponse.register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                if (throwable == null) {
                    //Everything is good. Response has been successfully 
                    //dispatched to client
                } else {
                    //An error has occurred during request processing
                }
            }
        }, new ConnectionCallback() {
            public void onDisconnect(AsyncResponse disconnected) {
                //Connection lost or closed by the client!
            }
        });
        
        CompletableFuture.supplyAsync(() -> {
            try {
                return producer.send().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        })
                .thenApply((result) -> 
        asyncResponse.resume(result)
    );
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
