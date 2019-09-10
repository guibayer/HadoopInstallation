package com.hadooptest;

import java.io.IOException;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
//import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.reduce.LongSumReducer;

public class RecordCount extends Configured implements Tool{
	
	private static class RecordMapper extends Mapper<LongWritable, Text, Text,LongWritable>{
		public void map(LongWritable lineOffset, Text record, Context output)
		throws IOException,InterruptedException {   //Implementação de uma classe mapper
			output.write(new Text("Count"), new LongWritable(1)); //A cada interação cria uma linha com Count  1
		}
		
	}
	
	
	
	public int run(String[] arg0) throws Exception {
		Job job = Job.getInstance(getConf()); //Cria a tarefa(Buscando os arquivos de configuração)
		
		
		job.setMapperClass(RecordMapper.class);  //Classe de Mapeamento a ser utilizada
		//job.setMapOutputKeyClass(Text.class);	//Não precisa ser especificado pois é o mesmo utilizado no reduce(mostrado a baixo)
												//Só é modificado se os tipos de map e de reduce forem diferentes
		//job.setMapOutputValueClass(LongWritable.class);	
		
		job.setReducerClass(LongSumReducer.class);  //Classe "Reduce" a ser utilizada
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);	
		
		job.setNumReduceTasks(1);
		
		job.setInputFormatClass(TextInputFormat.class);	//Formato de Arquivo a ser analisado(No exemplo é apenas demonstrado o padrão)
		job.setOutputFormatClass(TextOutputFormat.class);  //Formato de Arquivo de saída(No exemplo é apenas demonstrado o padrão)
		
		//Define o local dos arquivos a serem lidos
		FileInputFormat.setInputPaths(job, new Path(arg0[0]));
		//FileInputFormat.addInputPath(job, new Path("C:/Users/GuilhermePlay/Documents/Data/In/06740844.txt"));
		
		//Define o local dos arquivos após análise
		FileOutputFormat.setOutputPath(job, new Path(arg0[1]));
		
		return job.waitForCompletion(true) ? 0 : 1; //Espera a tarefa terminar
	
	}
	
	public static void main(String args[]) throws Exception{
		System.exit(ToolRunner.run(new RecordCount() /*define qual a classe a ser executada(Neste caso RecordCount)*/
													, args)); //Executa o método run citado a cima
		
	}
	
}
