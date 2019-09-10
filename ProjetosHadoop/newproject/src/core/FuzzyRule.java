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

import java.util.StringTokenizer;

/**
 * Represents a fuzzy rule
 * @author Mikel Elkano Ilintxeta
 * @version 1.0
 */
public class FuzzyRule {
	
	private static int i;
    
    /**
     * Returns the matching degree of the input example with the specified antecedents
     * @param membershipDegrees pre-computed membership degrees (the key of the first hash map is the variable index, and the one of the second hash map is the label index)
     * @param antecedents antecedents of the rule
     * @param example input example
     * @return matching degree of the input example with the specified antecedents
     */
    public static float computeMatchingDegree (float[][] membershipDegrees, byte[] antecedents, String[] example){
    	
    	float matching = 1.0f;
        
        // Compute matching degree
        for (i = 0; i < example.length; i++){
        	// If it is a nominal value and it is not equal to the antecedent, then there is no matching
        	if (Mediator.getVariables()[i] instanceof FuzzyVariable)
        		matching *= membershipDegrees[i][antecedents[i]];
        	else {
        		if (!((NominalVariable)Mediator.getVariables()[i]).
        				getNominalValue(antecedents[i]).contentEquals(example[i]))
        			return 0.0f;
        	}
        		
        }
        
        return matching;
    	
    }
    
    /**
     * Computes the membership degree of the input value to the specified fuzzy set
     * @param variable variable index
     * @param label linguistic label index
     * @param value input value
     * @return membership degree of the input value to the specified fuzzy set
     */
    public static float computeMembershipDegree (int variable, byte label, String value){
    	
    	if (Mediator.getVariables()[variable] instanceof NominalVariable){
    		if (!((NominalVariable)Mediator.getVariables()[variable]).
    				getNominalValue(label).contentEquals(value))
    			return 0.0f;
    		else
    			return 1.0f;
    	}
    	else
    		return (float)((FuzzyVariable)Mediator.getVariables()[variable]).getFuzzySets()[label].
    				getMembershipDegree(Double.parseDouble(value));
    	
    }
    
    /**
     * Returns a new rule represented by a byte array containing the index of antecedents and the class index (at last position of the array)
     * @param inputValues input string representing the example
     * @return a new rule represented by a byte array containing the index of antecedents and the class index (at last position of the array)
     */
    public static byte[] getRuleFromExample (String[] inputValues){

        Variable[] variables = Mediator.getVariables();
        byte[] labels = new byte[variables.length];
        // Get each attribute label
        for (i = 0; i < variables.length; i++)
        	labels[i] = variables[i].getLabelIndex(inputValues[i]);
        
        return labels;
        
    }
    
    /**
     * Index of each antecedent (linguistic label index in case of fuzzy variable and nominal value index in case of nominal variable)
     */
    private byte[] antecedents;
    
    /**
     * Rule weight
     */
    private float ruleWeight;
    
    /**
     * Class index
     */
    private byte classIndex;
    
    /**
     * Creates a new fuzzy rule from an array of antecedents, a class index, and a rule weight
     * @param antecedents antecedents of the rule
     * @param classIndex class index of the rule
     * @param ruleWeight rule weight
     */
    public FuzzyRule (byte[] antecedents, byte classIndex, float ruleWeight){
    	
    	this.antecedents = antecedents;
    	this.classIndex = classIndex;
    	this.ruleWeight = ruleWeight;
    	
    }
    
    /**
     * Creates a new rule from an instance (using all variables of the problem)
     * @param instanceStr string of the instance used to create a single rule
     */
    public FuzzyRule (String instanceStr){
        
        // Read values from string
        StringTokenizer st = new StringTokenizer(instanceStr, ", ");
        String [] inputValues = new String[st.countTokens()-1];
        int i = 0;
        while (st.countTokens()>1){
            inputValues[i] = st.nextToken();
            i++;
        }
        String classLabel = st.nextToken();
        classIndex = Mediator.getClassIndex(classLabel);

        // Generate the fuzzy rule
        Variable[] variables = Mediator.getVariables();
        antecedents = new byte[variables.length];
        for (int index = 0; index < variables.length; index++)
        	antecedents[index] = variables[index].getLabelIndex(inputValues[index]);
        
        ruleWeight = -1.0f;
        
    }
    
    /**
     * Returns the association degree of the input example with this rule
     * @param example input example
     * @return association degree of the input example with this rule
     */
    public float computeAssociationDegree (String[] example){
        return computeMatchingDegree(example)*ruleWeight;
    }
    
    /**
     * Returns the matching degree of the input example with the antecedent part of the rule
     * @param example input example (values of nominal attributes must be the nominal value index)
     * @return matching degree of the input example with the antecedent part of the rule
     */
    public float computeMatchingDegree (String[] example){
        
        float matching = 1.0f;
        
        for (int i = 0; i < example.length; i++){
        	// If it is a nominal value and it is not equal to the antecedent, then there is no matching
        	if (Mediator.getVariables()[i] instanceof NominalVariable){
        		if (!((NominalVariable)Mediator.getVariables()[i]).
        				getNominalValue(antecedents[i]).contentEquals(example[i]))
        			return 0.0f;
        	}
        	else
        		matching *= ((FuzzyVariable)Mediator.getVariables()[i]).
        			getFuzzySets()[antecedents[i]].getMembershipDegree(Double.parseDouble(example[i]));
        }
        
        return matching;
        
    }
    
    /**
     * Returns the label index of the antecedent in the specified position
     * @param position position of the antecedent
     * @return label index of the antecedent in the specified position
     */
    public byte getAntecedent (int position){
        return antecedents[position];
    }
    
    /**
     * Returns the rule class index
     * @return rule class index
     */
    public byte getClassIndex (){
        return classIndex;
    }
    
    /**
     * Returns the rule weight
     * @return rule weight
     */
    public float getRuleWeight (){
        return ruleWeight;
    }
    
    @Override
    public String toString (){

        String output = "IF ";
        
        for (int i = 0; i < antecedents.length - 1; i++){
            output += Mediator.getVariables()[i].getName() + " IS ";
            if (Mediator.getVariables()[i] instanceof FuzzyVariable)
            	output += "L_" + antecedents[i] + " AND ";
            else
            	output += ((NominalVariable)Mediator.getVariables()[i]).getNominalValue(antecedents[i]) + " AND ";
        }
        output += Mediator.getVariables()[antecedents.length-1].getName() + " IS ";
        if (Mediator.getVariables()[antecedents.length-1] instanceof FuzzyVariable)
        	output += "L_" + antecedents[antecedents.length-1];
        else
        	output += ((NominalVariable)Mediator.getVariables()[antecedents.length-1]).getNominalValue(antecedents[antecedents.length-1]);
        
        output += " THEN CLASS = " + Mediator.getClassLabel(classIndex) + " WITH RW = "+ruleWeight;
        
        return output;
        
    }

}
