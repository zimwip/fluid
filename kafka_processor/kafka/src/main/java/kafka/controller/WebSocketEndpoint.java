/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.controller;

import java.util.Date;
import kafka.message.Command;
import kafka.message.EventMessage;
import kafka.services.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 *
 * @author tzimmer
 */
@Controller
public class WebSocketEndpoint {

    @Autowired
    private KafkaProcessor consumer;

    
    @MessageMapping("/command")
    @SendTo("/topic/events")
    public EventMessage commandHandler(Command message) throws Exception {
        if ("start".equals(message.getName()))
        {
             consumer.start();
             return new EventMessage("Processor started", new Date());
        }
        else if ("stop".equals(message.getName()))
        {
            consumer.stop();
            return new EventMessage("Processor stopped", new Date());
        }
        return new EventMessage("Unknow command", new Date());
    }

}
