package bsc.spark.examples;

import org.apache.spark.api.java.*;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.Function;

import org.apache.hadoop.io.*;
import scala.Tuple2;
import java.util.Arrays;
import java.util.List;
import org.apache.spark.api.java.function.*;


public class WordCount { 

  public static void main(String[] args) {	     
	String inputDirPath = args[0]; //input dir 
	String outputDirPath = args[1]; //output dir (shoulnd'n exist yet)				    
    SparkConf conf = new SparkConf().setAppName("SequenceFile Test");    	
    JavaSparkContext sc = new JavaSparkContext(conf);       				
	JavaRDD<String> file = sc.textFile(inputDirPath+"/*.txt");
    JavaRDD<String> words = file.flatMap(new FlatMapFunction<String, String>() {
      public Iterable<String> call(String s) { return Arrays.asList(s.split(" ")); }
    });
    JavaPairRDD<String, Integer> pairs = words.mapToPair(new PairFunction<String, String, Integer>() {
      public Tuple2<String, Integer> call(String s) { return new Tuple2<String, Integer>(s, 1); }
    });
    JavaPairRDD<String, Integer> counts = pairs.reduceByKey(new Function2<Integer, Integer, Integer>() {
      public Integer call(Integer a, Integer b) { return a + b; }
    });    

    //Save results
	counts.saveAsTextFile(outputDirPath);
  }	
}

