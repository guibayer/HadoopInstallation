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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import utils.ByteArrayWritable;
import utils.ConsequentPart;
import core.Mediator;
import core.NominalVariable;
import core.Variable;

/**
 * Launches the two stages of MapReduce that generate the rule base (output of MapReduce) and the database. Usage: hadoop jar <jar_file> learner.mapreduce.MapReduceLauncher [-p] [-v] <hdfs://url:port> <parameters_file> <header_file> <input_path> <output_path>
 * @author Mikel Elkano Ilintxeta
 * @version 1.0
 */
public class MapReduceLauncher {
	
	private static long startMs, endMs;
	
	private static final char[] VALID_OPTIONAL_ARGS = new char[]{'p','v'};
    private static final int MAX_OPTIONAL_ARGS = VALID_OPTIONAL_ARGS.length;
    private static final int NUM_PROGRAM_ARGS = 5;
    private static final String COMMAND_STR = "hadoop jar <jar_file> learner.mapreduce.MapReduceLauncher"+printOptionalArgs()+"<hdfs://url:port> <parameters_file> <header_file> <input_path> <output_path>";
    
    /**
     * Returns true if the specified arguments contains a given argument
     * @param args arguments
     * @param arg argument
     * @return true if the specified arguments contains a given argument
     */
    private static boolean containsArg (String[] args, String arg){
        for (int i = 0; i < args.length; i++){
            if (args[i].charAt(0) == '-' && args[i].contains(arg))
            	return true;
        }
        return false;
    }
    
    /**
	 * Main method
	 * @author Mikel Elkano Ilintxeta
	 * @param args command line arguments ([-p]: print output, [-v]: verbose)
	 * @version 1.0
	 */
    public static void main(String[] args) {
    	
    	startMs = System.currentTimeMillis();
    	
    	/**
    	 * READ ARGUMENTS
    	 */

    	Configuration conf = new Configuration();
    	String[] otherArgs = null;
    	try{
	    	otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        
    	}
    	catch(Exception e){
    		System.err.println("\nINITIALIZATION ERROR:\n\n");
    		System.exit(-1);
    	}

    	String[] programArgs = new String[NUM_PROGRAM_ARGS];
        String[] optionalArgs = null;
        
        if (otherArgs.length < NUM_PROGRAM_ARGS || otherArgs.length > (MAX_OPTIONAL_ARGS + NUM_PROGRAM_ARGS)){
            System.err.println("\nUsage: "+COMMAND_STR+"\n");
            System.exit(2);
        }
        if (otherArgs.length > NUM_PROGRAM_ARGS){
            int numOptionalArgs = otherArgs.length - NUM_PROGRAM_ARGS;
            optionalArgs = new String[numOptionalArgs];
            for (int i = 0; i < numOptionalArgs; i++)
                optionalArgs[i] = otherArgs[i];
            for (int i = numOptionalArgs; i < otherArgs.length; i++)
                programArgs[i - otherArgs.length + NUM_PROGRAM_ARGS] = otherArgs[i];
            // Check optional args
            if (!validOptionalArgs(optionalArgs)){
                System.err.println("\nUsage: "+COMMAND_STR+"\n");
                System.exit(2);
            }
        }
        else
            programArgs = otherArgs;
        
        boolean printOutput = (optionalArgs==null)?false:containsArg(optionalArgs,"p");
        boolean verbose = (optionalArgs==null)?false:containsArg(optionalArgs,"v");
        String hdfsLocation = programArgs[0];
        String paramsPath = programArgs[1];
        String headerPath = programArgs[2];
        String inputPath = programArgs[3];
        String outputPath = programArgs[4];
        
        /**
         * SAVE BASIC PARAMETERS
         */
    	Mediator.setConfiguration(conf);
        Mediator.saveHDFSLocation(hdfsLocation);
        Mediator.saveLearnerInputPath(inputPath);
        Mediator.saveLearnerOutputPath(outputPath);
        
        /**
         * STORE ALGORITHM PARAMETERS AND READ HEADER FILE
         */
        
        // Store algorithm parameters
        try{
        	Mediator.storeChiParameters(hdfsLocation+"/"+paramsPath);
        }
        catch(Exception e){
        	System.err.println("ERROR READING PARAMETERS:\n");
        	e.printStackTrace();
        	System.exit(-1);
        }
        
        // Read header file and create variables and linguistic labels
        try{
        	Mediator.readHeaderFile(hdfsLocation+"/"+headerPath);
        }
        catch(Exception e){
        	System.err.println("ERROR READING HEADER FILE:\n");
        	e.printStackTrace();
        	System.exit(-1);
        }
        
        // Read learner configuration
        try{
        	Mediator.readLearnerConfiguration(Mediator.getConfiguration());
        }
        catch(Exception e){
        	System.err.println("ERROR READING CONFIGURATION:\n");
        	e.printStackTrace();
        	System.exit(-1);
        }
        
        // Check number of classes
        if (Mediator.getNumClasses() > 127){
        	System.err.println("\nERROR: The maximum number of classes is 127\n");
        	System.err.println(-1);
        }
        
        // Check cost-sensitive
        if (Mediator.getNumClasses() > 2 && Mediator.useCostSensitive()){
        	System.err.println("\nERROR: Cost-sensitive computation is not allowed in multi-class datasets\n");
        	System.err.println(-1);
        }
        
        /**
         * READ HADOOP CONFIGURATION
         */
        try {
        	Mediator.readHadoopConfiguration();
        }
        catch(Exception e){
        	System.err.println("ERROR READING HADOOP CONFIGURATION:\n");
        	e.printStackTrace();
        	System.exit(-1);
        }
    	
        /**
         * LAUNCH MAP REDUCE
         */
        try {
        	
	        Job job = Job.getInstance(conf);
	
	        job.setJarByClass(MapReduceLauncher.class);
	        if (Mediator.useCostSensitive())
	        	job.setMapperClass(RulesGenerationMapperCS.class);
	        else
	        	job.setMapperClass(RulesGenerationMapper.class);
	        job.setReducerClass(RulesGenerationReducer.class);
	        job.setInputFormatClass(KeyValueTextInputFormat.class);
	        job.setOutputFormatClass(SequenceFileOutputFormat.class);
	        job.setMapOutputKeyClass(ByteArrayWritable.class);
	        job.setMapOutputValueClass(ConsequentPart.class);
	        job.setOutputKeyClass(ByteArrayWritable.class);
	        job.setOutputValueClass(FloatWritable.class);
	        FileInputFormat.addInputPath(job, new Path(Mediator.getHDFSLocation()+"/"+Mediator.getLearnerInputPath()));
	        FileInputFormat.setMaxInputSplitSize(job, Mediator.computeHadoopSplitSize(
	    		Mediator.getHDFSLocation()+"/"+Mediator.getLearnerInputPath(), Mediator.getHadoopNumMappers()));
	        FileInputFormat.setMinInputSplitSize(job, Mediator.computeHadoopSplitSize(
	    		Mediator.getHDFSLocation()+"/"+Mediator.getLearnerInputPath(), Mediator.getHadoopNumMappers()));
	        FileOutputFormat.setOutputPath(job, new Path(Mediator.getHDFSLocation()+"/"+Mediator.getLearnerRuleBasePath()+"_TMP"));

        	job.setNumReduceTasks(1);
	        
	        conf.setLong("mapreduce.task.timeout", Mediator.getHadoopMaxMinsNoUpdate()*60000);
	        
	        job.waitForCompletion(true);
        
        }
        catch(Exception e){
        	System.err.println("ERROR LAUNCHING MAPREDUCE:\n");
        	e.printStackTrace();
        	System.exit(-1);
        }
    	
    	/**
         * MERGE OUTPUT FILES
         */
    	try {
    		writeFinalRuleBase(printOutput);
    	} catch (Exception e) {
			System.err.println("\nERROR MERGING STAGE 2 OUTPUT FILES");
			e.printStackTrace();
		}
    	
    	/**
    	 * WRITE DATABASE
    	 */
    	try {
    		writeDataBase(); // Save data base
    		writeDataBaseText (printOutput); // Write database in a human-readable format
    	}
    	catch(Exception e){
    		System.err.println("\nERROR WRITING DATA BASE\n");
    		e.printStackTrace();
    	}
    	
    	/**
    	 * WRITE EXECUTION TIME
    	 */
    	writeExecutionTime();
    	        
    }
    
    /**
     * Returns the list of optional args
     * @return list of optional args
     */
    private static String printOptionalArgs() {
        String output = "";
        for (int i = 0; i < MAX_OPTIONAL_ARGS; i++)
            output += " [-" + VALID_OPTIONAL_ARGS[i] + "]";
        return output + " ";
    }
    
    /**
     * Returns true if the specified optional arguments are valid
     * @param args optional arguments
     * @return true if the specified optional arguments are valid
     */
    private static boolean validOptionalArgs (String[] args){
        boolean validChar;
        // Iterate over args
        for (int i = 0; i < args.length; i++){
            if (args[i].charAt(0)!='-')
            	return false;
            // Iterate over characters (to read combined arguments)
            for (int charPos = 1; charPos < args[i].length(); charPos++){
            	validChar = false;
            	for (int j = 0; j < VALID_OPTIONAL_ARGS.length; j++){
	                if (args[i].charAt(charPos) == VALID_OPTIONAL_ARGS[j]){
	                    validChar = true;
	                    break;
	                }
            	}
            	if (!validChar)
            		return false;
        	}
        }
        return true;
    }
    
    /**
     * Writes the database in a file (including class labels)
     * @throws IOException 
     */
    private static void writeDataBase() throws IOException{
    	
    	// Write variables and class labels
    	FileSystem fs = FileSystem.get(Mediator.getConfiguration());
	    ObjectOutputStream objectOutputStream = new ObjectOutputStream(fs.create(new Path(Mediator.getHDFSLocation()+"/"+Mediator.getLearnerDatabasePath())));
	    objectOutputStream.writeObject(Mediator.getVariables());
	    objectOutputStream.writeObject(Mediator.getClassLabels());
	    objectOutputStream.close();
	    fs.close();
    	
    }
    
    /**
     * Writes the data base in text format in a file (specified by Mediator.DATA_BASE_OUTPUT_PATH)
     * @param printStdOut if true the data base is printed in the standard output
     * @throws IOException 
     */
    private static void writeDataBaseText (boolean printStdOut) throws IOException {
    	
    	Path pt = new Path(Mediator.getHDFSLocation()+Mediator.getLearnerDatabasePath()+"_text.txt");
        FileSystem fs = FileSystem.get(Mediator.getConfiguration());
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fs.create(pt,true)));
        bw.write("\nDATA BASE ("+Mediator.getNumVariables()+" variables):\n\n");
        if (printStdOut)
        	System.out.println("\nDATA BASE ("+Mediator.getNumVariables()+" variables):\n");
        for (Variable variable:Mediator.getVariables()){
        	bw.write(variable+"\n");
        	if (printStdOut)
        		System.out.println(variable);
        }
        bw.close();
    	
    }
    
    /**
     * Writes total execution time
     */
    private static void writeExecutionTime() {
    	
    	endMs = System.currentTimeMillis();
    	long elapsed = endMs - startMs;
    	long hours = elapsed / 3600000;
        long minutes = (elapsed % 3600000) / 60000;
        long seconds = ((elapsed % 3600000) % 60000) / 1000;
        
        try {
        	
        	FileSystem fs = FileSystem.get(Mediator.getConfiguration());
        	Path outputPath = new Path(Mediator.getHDFSLocation()+"/"+Mediator.getLearnerOutputPath()+"/time.txt");
        	OutputStream os = fs.create(outputPath);
        	BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
        	
        	/**
        	 * Write total execution time
        	 */
        	bw.write("Total execution time (hh:mm:ss): "+String.format("%02d",hours)+":"+String.format("%02d",minutes)+":"+
    			String.format("%02d",seconds)+" ("+(elapsed/1000)+" seconds)\n");
        	
        	/**
        	 *  Write Mappers execution time avg.
        	 */
        	Path inputPath = new Path(Mediator.getHDFSLocation()+"/"+Mediator.getLearnerOutputPath()+"/"+Mediator.TIME_STATS_DIR);
        	FileStatus[] status = fs.listStatus(inputPath);
        	BufferedReader br = null;
        	String buffer;
        	long sumMappers = 0;
        	int numMappers = 0;
        	for (FileStatus fileStatus:status){
        		// Read Stage 1
        		if (fileStatus.getPath().getName().contains("mapper")){
        			numMappers ++;
        			br=new BufferedReader(new InputStreamReader(fs.open(fileStatus.getPath())));
        			buffer = br.readLine();
        			sumMappers += Long.parseLong(buffer.substring(buffer.indexOf(":")+1).trim());
        		}
        		br.close();
        	}
        	// Write AVG
        	elapsed = sumMappers / numMappers;
        	hours = elapsed / 3600;
            minutes = (elapsed % 3600) / 60;
            seconds = (elapsed % 3600) % 60;
            bw.write("Mappers avg. execution time (hh:mm:ss): "+String.format("%02d",hours)+":"+String.format("%02d",minutes)+":"+
        			String.format("%02d",seconds)+" ("+elapsed+" seconds)\n");
        	
        	bw.close();
        	os.close();
        	
        	// Remove time stats directory
        	fs.delete(inputPath,true);
        	
        }
        catch(Exception e){
        	System.err.println("\nERROR WRITING EXECUTION TIME");
			e.printStackTrace();
        }
    	
    }
    
    /**
     * Writes the final rule base in a human-readable file
     * @param printStdOut if true the rule base is printed in the standard output
     * @throws IOException 
     * @throws IllegalArgumentException 
     */
    private static void writeFinalRuleBase (boolean printStdOut) throws IllegalArgumentException, IOException{
    	
    	FileSystem fs = FileSystem.get(Mediator.getConfiguration());
    	Path textPath = new Path(Mediator.getHDFSLocation()+Mediator.getLearnerRuleBasePath()+"_text.txt");
        BufferedWriter bwText = new BufferedWriter(new OutputStreamWriter(fs.create(textPath,true)));
        FileStatus[] status = fs.listStatus(new Path(Mediator.getHDFSLocation()+Mediator.getLearnerRuleBasePath()+"_TMP"));
    	Writer writer = SequenceFile.createWriter(Mediator.getConfiguration(), 
    			Writer.file(new Path(Mediator.getHDFSLocation()+Mediator.getLearnerRuleBasePath())), 
    			Writer.keyClass(ByteArrayWritable.class), Writer.valueClass(FloatWritable.class));
        Reader reader;
		ByteArrayWritable rule = new ByteArrayWritable();
		FloatWritable ruleWeight = new FloatWritable();
		int id = 0;
        
        bwText.write("\nRULE BASE:\n\n");
        if (printStdOut)
        	System.out.println("\nRULE BASE:\n");
    	
    	// Read all sequence files from stage 2
    	for (FileStatus fileStatus:status){
    		
    		if (!fileStatus.getPath().getName().contains("_SUCCESS")){
    		
	    		// Open sequence file
	    		reader = new Reader(Mediator.getConfiguration(), Reader.file(fileStatus.getPath()));
	            
	            // Read all rules
	            while (reader.next(rule, ruleWeight)) {
	            	
	            	// Write rule in the output sequence file
	            	writer.append(rule, ruleWeight);
	            	
	            	bwText.write("Rule ("+id+"): IF ");
	            	if (printStdOut)
	            		System.out.print("Rule ("+id+"): IF ");
	            	
	            	// Write antecedents
	            	for (int i = 0; i < rule.getBytes().length - 2; i++){
	            		
	            		bwText.write(Mediator.getVariables()[i].getName()+" IS ");
	            		if (printStdOut)
	                		System.out.print(Mediator.getVariables()[i].getName()+" IS ");
	            		if (Mediator.getVariables()[i] instanceof NominalVariable){
	            			bwText.write(((NominalVariable)Mediator.getVariables()[i]).getNominalValue(rule.getBytes()[i])+" AND ");
	            			if (printStdOut)
	                    		System.out.print(((NominalVariable)Mediator.getVariables()[i]).getNominalValue(rule.getBytes()[i])+" AND ");
	            		}
	        			else {
	        				bwText.write("L_"+rule.getBytes()[i]+" AND ");
	        				if (printStdOut)
	                    		System.out.print("L_"+rule.getBytes()[i]+" AND ");
	        			}
	            		
	            	}
	            	
	            	// Write the last antecedent
	            	bwText.write(Mediator.getVariables()[rule.getBytes().length-2].getName()+" IS ");
	            	if (printStdOut)
	            		System.out.print(Mediator.getVariables()[rule.getBytes().length-2].getName()+" IS ");
	            	if (Mediator.getVariables()[rule.getBytes().length-2] instanceof NominalVariable){
	            		bwText.write(((NominalVariable)Mediator.getVariables()[rule.getBytes().length-2]).getNominalValue(rule.getBytes()[rule.getBytes().length-2]));
	            		if (printStdOut)
	                		System.out.print(((NominalVariable)Mediator.getVariables()[rule.getBytes().length-2]).getNominalValue(rule.getBytes()[rule.getBytes().length-2]));
	            	}
	        		else {
	        			bwText.write("L_"+rule.getBytes()[rule.getBytes().length-2]);
	        			if (printStdOut)
	                		System.out.print("L_"+rule.getBytes()[rule.getBytes().length-2]);
	        		}
	            	
	            	// Write the class and rule weight
	            	bwText.write(" THEN CLASS = "+Mediator.getClassLabel(rule.getBytes()[rule.getBytes().length-1]));
	            	bwText.write(" WITH RW = "+ruleWeight.get()+"\n\n");
	            	if (printStdOut){
	            		System.out.print(" THEN CLASS = "+Mediator.getClassLabel(rule.getBytes()[rule.getBytes().length-1]));
	            		System.out.print(" WITH RW = "+ruleWeight.get()+"\n\n");
	            	}
	
	            	id++;
	            	
	            }
	            
	            reader.close();
            
    		}
    		
    	}
    	
    	bwText.close();
        writer.close();
    	
    	// Remove temporary rule base
    	fs.delete(new Path(Mediator.getHDFSLocation()+Mediator.getLearnerRuleBasePath()+"_TMP"),true);
    	
    }
    
}
