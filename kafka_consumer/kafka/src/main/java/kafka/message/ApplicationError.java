/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.message;

/**
 *
 * @author tzimmer
 */
public class ApplicationError {

    private final String error;

    public ApplicationError(String test) {
        this.error = test;
    }
    
}
