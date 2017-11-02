/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.controller;

import java.util.Date;
import kafka.message.Command;
import kafka.message.EventMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

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
