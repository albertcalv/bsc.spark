package bsc.spark;

import org.apache.spark.api.java.*;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.Function;
import org.apache.hadoop.io.*;
import scala.Tuple2;
import java.util.List;
import java.util.ArrayList;
import org.apache.spark.api.java.function.PairFunction;
import java.util.Arrays;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;


public class Spark2Gnuplot {
	/*public static void writeTest(List<Tuple2<String, scala.Int>> input, String outputFile) throws Exception {			
		Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile)));
		//out.write("#\tv1\tv2");				
		for (Tuple2<String, scala.Int> t : input) {
		  out.write("\n");
		  out.write(t._1+"\t"+t._2);
		}
		out.close();
	}*/
	
	public static void writeStringInt(org.apache.spark.rdd.RDD<Tuple2<String, scala.Int>> input, String outputFile) throws Exception {			
		Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile)));								
		scala.collection.Iterator<Tuple2<String, scala.Int>> i = input.toLocalIterator();
		while (i.hasNext())  {
			Tuple2<String, scala.Int> t = (Tuple2<String, scala.Int>) i.next();
			out.write("\n"+t._1+"\t"+t._2);			
		}		
		out.close();
	}	

	public static void writeIntInt(org.apache.spark.rdd.RDD<Tuple2<scala.Int, scala.Int>> input, String outputFile) throws Exception {			
		Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile)));							
		scala.collection.Iterator<Tuple2<scala.Int, scala.Int>> i = input.toLocalIterator();
		while (i.hasNext())  {		
			Tuple2<scala.Int, scala.Int> t = (Tuple2<scala.Int, scala.Int>) i.next();
			out.write("\n"+t._1+"\t"+t._2);			
		}							
		out.close();
	}	

	public static void writeString(org.apache.spark.rdd.RDD<String> input, String outputFile) throws Exception {			
		Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile)));							
		scala.collection.Iterator<String> i = input.toLocalIterator();
		while (i.hasNext())  {		
			String s = (String) i.next();
			out.write("\n"+s);			
		}							
		out.close();
	}	

	public static String unixTime2String(String unixTimeString, String format) {	
		//e.g. format = "yyyy-MM-dd HH:mm:ss"
		long unixTime = Long.valueOf(unixTimeString).longValue();
		Calendar mydate = GregorianCalendar.getInstance(TimeZone.getTimeZone("Europe/Madrid")); 
		mydate.setTimeInMillis(unixTime*1000);
		SimpleDateFormat f = new SimpleDateFormat(format);
		return f.format(mydate.getTime());
	}		
	
	/*public static void main(String [ ] args) throws Exception {			
		ArrayList<Tuple2<String, scala.Int>> input = new ArrayList<Tuple2<String, scala.Int>>();
		input.add(new Tuple2("word1",3));
		input.add(new Tuple2("word2",777));
		input.add(new Tuple2("word3",4));
		writeTest(input, "test_output.txt");
	}*/
}