/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.reactor.controller;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import org.reactor.message.Command;
import org.reactor.message.EventMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 *
 * @author tzimmer
 */
@Controller
public class WebSocketEndpoint {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/command")
    @SendTo("/topic/events")
    public EventMessage commandHandler(Command message) throws Exception {
        Flux.range(1,100000)
                .parallel(3)
                .runOn(Schedulers.parallel())
               // .collect(ArrayList::new, ArrayList::add)
                .sequential()
               // .reduce(0, (a, b) -> a + b.size())
                .reduce(0, (a, b) -> a + b%2)
                .subscribe(System.out::println);
            return new EventMessage("Command "+message.getName()+" received", new Date());
    }

    /**
     *
     * @param message
     * @return
     * @throws ApplicationErrorHandler
     */
    @MessageExceptionHandler
    @SendToUser(value = "/queue/errors", broadcast = false)
    public String handleException(Exception message) {
        return message.getMessage();
    }

}
