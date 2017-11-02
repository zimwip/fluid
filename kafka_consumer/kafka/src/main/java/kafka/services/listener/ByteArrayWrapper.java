/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.services.listener;

import java.util.Arrays;

/**
 * Simple wrapper class used for wrapping arrays so they can be used as keys
 * inside maps.
 *
 * @author tzimmer
 */
public class ByteArrayWrapper {

    private final byte[] array;
    private final int hashCode;

    public ByteArrayWrapper(byte[] array) {
        this.array = array;
        this.hashCode = Arrays.hashCode(array);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ByteArrayWrapper) {
            return Arrays.equals(array, ((ByteArrayWrapper) obj).array);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * Returns the array.
     *
     * @return Returns the array
     */
    public byte[] getArray() {
        return array;
    }
}
