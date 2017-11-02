/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.services.listener;

import io.netty.handler.codec.stomp.StompFrame;

/**
 *
 * @author tzimmer
 */
public interface MessageListener {
    
    	/**
	 * Callback for processing received objects through Redis.
	 *
	 * @param message message must not be {@literal null}.
	 */
	void onMessage(String topic, String message);
    
}
