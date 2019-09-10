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

package learner;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.mapreduce.Reducer;

import utils.ByteArrayWritable;
import utils.ConsequentPart;

/**
 * Reducer class used to gather rules
 * @author Mikel Elkano Ilintxeta
 * @version 1.0
 */
public class RulesGenerationReducer extends Reducer<ByteArrayWritable, ConsequentPart, ByteArrayWritable, FloatWritable> {

	private Iterator<ConsequentPart> iterator;
	private ConsequentPart consequent;
	private byte classIndex;
	private float ruleWeight;
	
    @Override
    public void reduce(ByteArrayWritable key, Iterable<ConsequentPart> values, Context context) throws IOException, InterruptedException {
        
    	iterator = values.iterator();
    	
    	// Get the maximum rule weight and the class of the rule
    	classIndex = -1;
    	ruleWeight = 0;
    	while (iterator.hasNext()){
    		consequent = iterator.next();
    		if (consequent.getRuleWeight() > ruleWeight){
    			classIndex = consequent.getClassIndex();
    			ruleWeight = consequent.getRuleWeight();
    		}
    	}
    	
    	key.add(classIndex);
    	
    	/*
    	 * Key: Antecedents and class of the rule
    	 * Value: Rule weight
    	 */
    	context.write(key, new FloatWritable(ruleWeight));
        
    }
    
}

