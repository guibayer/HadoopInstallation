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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import utils.ByteArrayWritable;
import utils.ConsequentPart;
import core.FuzzyRule;
import core.FuzzyVariable;
import core.Mediator;

/**
 * Cost-sensitive version of RulesGenerationMapper
 * @author Mikel Elkano Ilintxeta
 * @version 1.0
 */
public class RulesGenerationMapperCS extends Mapper<Text, Text, ByteArrayWritable, ConsequentPart>{
    
	/**
	 * Rule Base
	 */
	private byte[][] ruleBase; // Antecedents of each rule
	private ArrayList<Byte>[] rulesClasses; // Classes of each rule
	private float[][] matchingDegrees; // Matching degrees of the classes of each rule
	private float[] classCost; // Cost associated to each class
	
	/**
	 * Dataset
	 */
	private String[][] inputValues; // Input values of the instances of the current split of the dataset
	private byte[] classLabels; // Indices of the instances class labels
	
	/**
	 * Temporary structures
	 */
	private HashMap<ByteArrayWritable,ArrayList<Byte>> ruleBaseTmp; // Key: antecedents of the rule, Value: Classes of the rule
	private float[][] membershipDegrees; // Pre-computed membership degrees of a given example
	private String[] input;
	private byte classIndex;
	private ArrayList<String[]> inputValuesTmp;
	private ArrayList<Byte> classLabelsTmp;
	private ByteArrayWritable newRule;
	private ArrayList<Byte> classEntry;
	private StringTokenizer st;
	
	/**
	 * Counters
	 */
	private int i,j,k;
	private long startMs, endMs;
	
	@Override
	protected void cleanup (Context context) throws IOException, InterruptedException{
		
		/**
		 *  Transform instances into an array
		 */
		inputValues = new String[inputValuesTmp.size()][Mediator.getNumVariables()];
		classLabels = new byte[classLabelsTmp.size()];
		for (i = 0; i < inputValuesTmp.size(); i++)
			inputValues[i] = inputValuesTmp.get(i);
		for (i = 0; i < classLabelsTmp.size(); i++)
			classLabels[i] = classLabelsTmp.get(i);
		inputValuesTmp.clear();
		inputValuesTmp = null;
		classLabelsTmp.clear();
		classLabelsTmp = null;
		
		/**
		 *  Transform the rule base into arrays
		 */
		matchingDegrees = new float[classLabels.length][Mediator.getNumClasses()];
    	for (i = 0; i < classLabels.length; i++)
    		for (j = 0; j < Mediator.getNumClasses(); j++)
    			matchingDegrees[i][j] = 0.0f;
    	membershipDegrees = new float[Mediator.getNumVariables()][Mediator.getNumLinguisticLabels()];
    	rulesClasses = new ArrayList[classLabels.length];
    	ruleBase = new byte[classLabels.length][Mediator.getNumVariables()];
    	Iterator<Entry<ByteArrayWritable,ArrayList<Byte>>> iterator = ruleBaseTmp.entrySet().iterator();
		Entry<ByteArrayWritable,ArrayList<Byte>> ruleEntry;
		i = 0;
		while (iterator.hasNext()){
			ruleEntry = iterator.next();
			ruleBase[i] = ruleEntry.getKey().getBytes(); // Antecedents of the rule
			rulesClasses[i] = ruleEntry.getValue(); // Classes of the rule
			i++;
		}
    	
		/**
		 *  Compute the matching degree of all the examples with all the rules
		 */
		byte label;
		int progress = 0;
		for (i = 0; i < inputValues.length; i++){
			
			// Compute the membership degree of the current value to all linguistic labels
			for (j = 0; j < Mediator.getNumVariables(); j++) {
	    		if (Mediator.getVariables()[j] instanceof FuzzyVariable)
	    			for (label = 0; label < Mediator.getNumLinguisticLabels(); label++)
	    				membershipDegrees[j][label] = FuzzyRule.computeMembershipDegree(j,label,inputValues[i][j]);
			}
			// Compute the matching degree of the example with all rules
			for (j = 0; j < ruleBase.length; j++)
				matchingDegrees[j][classLabels[i]] += FuzzyRule.computeMatchingDegree(
					membershipDegrees, ruleBase[j], inputValues[i]) * classCost[classLabels[i]];
			
			// Update progress
			if (i % 1000 == 0){
				progress = (i / inputValues.length) * 100;
				context.setStatus("Cleanup: "+progress+"% completed");
				context.progress();
			}
			
		}
		
		/**
		 * Compute the rule weight of each rule and solve the conflicts
		 */
		
		float currentRW, ruleWeight, sum, sumOthers;
		
		for (i = 0; i < ruleBase.length; i++){
			
			ruleWeight = 0.0f;
	    	classIndex = -1;
	    	sum = 0.0f;
	    	
	    	for (j = 0; j < matchingDegrees[i].length; j++)
	    		sum += matchingDegrees[i][j];
			for (j = 0; j < matchingDegrees[i].length; j++){
				if (rulesClasses[i].contains((byte)j)){
					sumOthers = 0;
					for (k = 0; k < matchingDegrees[i].length; k++){
						if (j != k)
							sumOthers += matchingDegrees[i][k];
					}
					currentRW = (matchingDegrees[i][j] - sumOthers) / sum;
					if (currentRW > ruleWeight){
						ruleWeight = currentRW;
						classIndex = (byte)j;
					}
				}
			}
			
			if (ruleWeight > 0) {
		    	
		    	/*
		    	 * Key: Antecedents and class of the rule
		    	 * Value: Rule weight
		    	 */
		    	context.write(new ByteArrayWritable(ruleBase[i]), new ConsequentPart(classIndex,ruleWeight));
	    	
			}
		
		}
		
		/**
		 *  Write execution time
		 */
		endMs = System.currentTimeMillis();
		long mapperID = context.getTaskAttemptID().getTaskID().getId();
		try {
        	FileSystem fs = FileSystem.get(Mediator.getConfiguration());
        	Path file = new Path(Mediator.getHDFSLocation()+"/"+Mediator.getLearnerOutputPath()+"/"+Mediator.TIME_STATS_DIR+"/mapper"+mapperID+".txt");
        	OutputStream os = fs.create(file);
        	BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
        	bw.write("Execution time (seconds): "+((endMs-startMs)/1000));
        	bw.close();
        	os.close();
        }
        catch(Exception e){
        	System.err.println("\nMAPPER: ERROR WRITING EXECUTION TIME");
			e.printStackTrace();
        }
		
	}
	
	@Override
    public void map(Text key, Text value, Context context) throws IOException, InterruptedException {
		
		// Add instance
		st = new StringTokenizer(key.toString()+value.toString(), ", ");
		input = new String[Mediator.getNumVariables()];
		inputValuesTmp.add(input);
		i = 0;
		while (st.countTokens() > 1){
			input[i] = st.nextToken();
			i++;
		}
		classIndex = Mediator.getClassIndex(st.nextToken());
		classLabelsTmp.add(classIndex);
		
		// Generate a new fuzzy rule
		byte[] antecedents = FuzzyRule.getRuleFromExample(input);
        
        // Add rule/class if does not exist
        newRule = new ByteArrayWritable(antecedents);
        classEntry = ruleBaseTmp.get(newRule);
        if (classEntry == null){
        	classEntry = new ArrayList<Byte>();
        	ruleBaseTmp.put(newRule, classEntry);
        }
        if (!classEntry.contains(classIndex))
        	classEntry.add(classIndex);
        
    }
	
	@Override
	protected void setup(Context context) throws InterruptedException, IOException{

		super.setup(context);
		
		try {
			Mediator.readLearnerConfiguration(context.getConfiguration());
		}
		catch(Exception e){
			System.err.println("\nSTAGE 1: ERROR READING CONFIGURATION\n");
			e.printStackTrace();
			System.exit(-1);
		}
		
		startMs = System.currentTimeMillis();
		
		ruleBaseTmp = new HashMap<ByteArrayWritable,ArrayList<Byte>>();
		inputValuesTmp = new ArrayList<String[]>();
		classLabelsTmp = new ArrayList<Byte>();
		
		/**
		 * Compute the cost of each class
		 */
		classCost = new float[2];
		long[] numExamples = Mediator.getClassNumExamples();
		if (numExamples[0] > numExamples[1]){
			classCost[0] = 1.0f;
			classCost[1] = ((float)numExamples[0]) / ((float)numExamples[1]);
		}
		else if (numExamples[0] < numExamples[1]){
			classCost[0] = ((float)numExamples[1]) / ((float)numExamples[0]);
			classCost[1] = 1.0f;
		}
		else {
			classCost[0] = 1.0f;
			classCost[1] = 1.0f;
		}
	
	}
    
}
