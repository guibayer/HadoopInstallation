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

import org.apache.hadoop.io.Writable;

/**
 * Represents the consequent part of the rule, including the class and the rule weight
 * @author Mikel
 *
 */
public class ConsequentPart implements Writable, Serializable {

	private byte classIndex;
	private float ruleWeight;
	
	/**
	 * Default constructor
	 */
	public ConsequentPart () {}
	
	/**
	 * Constructs a new consequent
	 * @param classIndex index of the class of the rule
	 * @param ruleWeight weight of the rule
	 */
	public ConsequentPart (byte classIndex, float ruleWeight){
		this.classIndex = classIndex;
		this.ruleWeight = ruleWeight;
	}
	
	/**
	 * Returns the class index
	 * @return class index
	 */
	public byte getClassIndex() {
		return classIndex;
	}
	
	/**
	 * Returns the rule weight
	 * @return rule weight
	 */
	public float getRuleWeight() {
		return ruleWeight;
	}
	
	@Override
    public void readFields(DataInput in) throws IOException {
    	
        classIndex = in.readByte();
        ruleWeight = in.readFloat();
        
    }
	
	@Override
    public void write(DataOutput out) throws IOException {
    	
        out.writeByte(classIndex);
        out.writeFloat(ruleWeight);
        
    }
	
}
