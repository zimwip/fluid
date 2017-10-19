/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.airbus.mapper.domain;

/**
 *
 * @author tzimmer
 */
public class ForeignKey {

    private final String current;
    private final String outer;

    public ForeignKey(String current, String outer) {
        this.current = current;
        this.outer = outer;
    }

    public String getCurrent() {
        return current;
    }

    public String getOuter() {
        return outer;
    }
    
}
