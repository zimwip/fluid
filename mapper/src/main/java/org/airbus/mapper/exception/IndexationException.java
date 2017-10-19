/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.airbus.mapper.exception;

/**
 *
 * @author TZIMMER
 */
public class IndexationException extends RuntimeException {

    public IndexationException() {
    }

    public IndexationException(String message) {
        super(message);
    }

    public IndexationException(String message, Throwable cause) {
        super(message, cause);
    }

    public IndexationException(Throwable cause) {
        super(cause);
    }

    public IndexationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    
}
