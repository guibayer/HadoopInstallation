/*
 * Copyright (C) 2014 Mikel Elkano Ilintxeta
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.apache.hadoop.io.WritableComparable;

/**
 * Implementation of a serializable byte array
 * @author Mikel Elkano Ilintxeta
 * @version 1.0
 */
public class ByteArrayWritable implements WritableComparable<ByteArrayWritable>, Serializable {
    
	private byte[] bytes;
	private int i;
	
	/**
     * Default constructor
     */
    public ByteArrayWritable() {}
    
    /**
     * Constructs a new serializable array of one element
     * @param data input byte
     */
    public ByteArrayWritable(byte data) {
        this.bytes = new byte[]{data};
    }

	/**
     * Constructs a new serializable array from the input byte array
     * @param data input byte array
     */
    public ByteArrayWritable(byte[] data) {
        this.bytes = data;
    }
    
    /*
    @Override
	public int compareTo(ByteArrayWritable o) {
    	return new String(bytes).compareTo(new String(o.getBytes()));
	}
	*/
    
    /**
     * Adds a new element to the array
     * @param element element to be added
     */
    public void add (byte element){
    	
    	byte[] newBytes = new byte[bytes.length+1];
    	
    	for (i = 0; i < bytes.length; i++)
    		newBytes[i] = bytes[i];
    	newBytes[bytes.length] = element;
    	
    	bytes = newBytes;
    	
    }
    
    @Override
	public int compareTo(ByteArrayWritable o) {
    	int i = 0;
    	if (bytes.length == o.getBytes().length){
			while (i < bytes.length && bytes[i]==o.getBytes()[i])
				i++;
			if (i >= bytes.length)
				return 0;
		}
    	else if (bytes.length < o.getBytes().length){
			while (i < bytes.length && bytes[i]==o.getBytes()[i])
				i++;
			if (i >= bytes.length)
				return -1;
		}
		else{
			while (i < o.getBytes().length && bytes[i]==o.getBytes()[i])
				i++;
			if (i >= o.getBytes().length)
				return 1;
		}
		if (bytes[i] > o.getBytes()[i])
			return 1;
		else
			return -1;
	}

    /**
     * Returns the byte array
     * @return byte array
     */
    public byte[] getBytes() {
        return bytes;
    }
    
    @Override
    public int hashCode(){
    	return new String(bytes).hashCode();
    }
    
    @Override
    public void readFields(DataInput in) throws IOException {
    	
        int length = in.readInt();

        bytes = new byte[length];

        for(int i = 0; i < length; i++)
            bytes[i] = in.readByte();
        
    }

    /**
     * Sets the byte array
     * @param data input byte array
     */
    public void setData(byte[] data) {
        this.bytes = data;
    }

    @Override
    public String toString(){
    	
    	String output = "Bytes: ";
    	
    	for (int i = 0; i < bytes.length; i++)
    		output += bytes[i] + " | ";
    	
    	return output;
    	
    }
    
    @Override
    public void write(DataOutput out) throws IOException {
    	
        int length = 0;
        
        if(bytes != null)
            length = bytes.length;

        out.writeInt(length);

        for(int i = 0; i < length; i++)
            out.writeByte(bytes[i]);
        
    }
    
}
