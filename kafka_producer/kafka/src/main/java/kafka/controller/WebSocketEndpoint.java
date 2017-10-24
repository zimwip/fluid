/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.controller;

import java.util.Date;
import kafka.message.Command;
import kafka.message.EventMessage;
import kafka.services.KafkaProducer;
import org.fagazi.enedis.DataResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 *
 * @author tzimmer
 */
@Controller
public class WebSocketEndpoint {

    @Autowired
    private KafkaProducer producer;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/command")
    @SendTo("/topic/events")
    public EventMessage commandHandler(Command message) throws Exception {
        if ("start".equals(message.getName())) {
            producer.start();
            return new EventMessage("Processor started", new Date());
        }
        if ("send".equals(message.getName())) {
            DataResponse result = producer.send().get();
            messagingTemplate.convertAndSend("/topic/data", result);
            return new EventMessage("Send message", new Date());
        } else if ("config".equals(message.getName())) {
            producer.config();
            return new EventMessage("Processor stopped", new Date());
        } else if ("stop".equals(message.getName())) {
            producer.stop();
            return new EventMessage("Processor stopped", new Date());
        }
        return new EventMessage("Unknow command", new Date());
    }

}
