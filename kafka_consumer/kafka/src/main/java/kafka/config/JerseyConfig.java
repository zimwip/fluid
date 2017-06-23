/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.config;

import kafka.controller.Controller;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

/**
 *
 * @author tzimmer
 */
@Component
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        register(Controller.class);
    }
}
