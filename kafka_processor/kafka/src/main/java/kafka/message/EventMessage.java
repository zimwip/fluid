/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.message;

import java.util.Date;

/**
 *
 * @author tzimmer
 */
public class EventMessage {
    
    private String event;
    private Date date;

    public EventMessage(String event, Date date) {
        this.event = event;
        this.date = date;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
    
}
