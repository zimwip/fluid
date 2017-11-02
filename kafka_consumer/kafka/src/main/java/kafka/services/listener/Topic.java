/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.services.listener;

/**
 *
 * @author tzimmer
 */
public interface Topic {

    /**
     * Returns the topic (as a String).
     *
     * @return the topic
     */
    String getTopic();

}
