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

package core;

import java.util.ArrayList;

/**
 * Represents a rule base
 * @author Mikel Elkano Ilintxeta
 * @version 1.0
 */
public class RuleBase {
	
	public static final byte FRM_WINNING_RULE = 0;
	public static final byte FRM_ADDITIVE_COMBINATION = 1;
    
    /**
     * Rule base
     */
    private ArrayList<FuzzyRule> ruleBase;
    
    public RuleBase (){
        
        ruleBase = new ArrayList<FuzzyRule>();
        
    }
    
    /**
     * Creates a new fuzzy rule from an instance
     * @param instanceStr string of the instance used to create a single rule
     */
    public void addFuzzyRule (FuzzyRule newFuzzyRule){
        ruleBase.add(newFuzzyRule);
    }
    
    /**
     * Classifies an example
     * @param frm fuzzy reasoning method to be used (0: winning rule, 1: additive combination)
     * @param example input example
     * @return predicted class
     */
    public byte classify (byte frm, String[] example){
    	if (frm == FRM_WINNING_RULE)
    		return (byte)FRM_WR(example)[0];
    	else
    		return (byte)FRM_AC(example)[0];
    }
    
    /**
     * Additive Combination Fuzzy Reasoning Method
     * @param example input example
     * @return a double array where [0] is the predicted class index and [1] is the confidence degree
     */
    private double[] FRM_AC (String[] example){
    	
    	double[] output = new double[2];
    	output[0] = Mediator.getMostFrequentClass(); // Default class
    	output[1] = 0.0; // Default confidence
    	
    	double[] classDegree = new double[Mediator.getNumClasses()];
    	for (byte i = 0; i < classDegree.length; i++) classDegree[i] = 0.0;
    	
    	double degree;
    	
    	// Compute the confidence of each class
    	for (FuzzyRule rule:ruleBase) {
    		degree = rule.computeAssociationDegree(example);
    		classDegree[rule.getClassIndex()] += degree;
    	}
    	
    	// Get the class with the highest confidence
    	for (byte i = 0; i < classDegree.length; i++) {
    		if (classDegree[i] > output[1]) {
    			output[0] = i;
    			output[1] = classDegree[i];
    		}
    	}
    	
    	return output;
    	
    }
    
    /**
     * Winning Rule Fuzzy Reasoning Method
     * @param example input example
     * @return a double array where [0] is the predicted class index and [1] is the confidence degree
     */
    private double[] FRM_WR (String[] example){
    	
    	double[] output = new double[2];
    	output[0] = Mediator.getMostFrequentClass(); // Default class
    	output[1] = 0.0; // Default confidence
    	
    	double degree;
    	
    	// Get the class with the rule with highest association degree
    	for (FuzzyRule rule:ruleBase) {
    		degree = rule.computeAssociationDegree(example);
    		if (degree > output[1]){
    			output[0] = rule.getClassIndex();
    			output[1] = degree;
    		}
    	}
    	
    	return output;
    	
    }
    
    /**
     * Returns the rule base of this classifier
     * @return rule base of this classifier
     */
    public ArrayList<FuzzyRule> getRuleBase (){
        return ruleBase;
    }

}
