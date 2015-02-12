package bsc.spark.image;

/* SequenceFileTest.java */
import org.apache.spark.api.java.*;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.Function;

import org.apache.hadoop.io.*;
import scala.Tuple2;
import java.util.List;
import org.apache.spark.api.java.function.PairFunction;

import org.opencv.core.Core;
import org.opencv.core.Mat;
//import org.opencv.imgcodecs.Imgcodecs; (opencv 3.0) 
import org.opencv.highgui.*; //opencv 2.4.9    
import org.opencv.utils.Converters;
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;

import java.io.*;


public class Example1 {
    
    /* example usage:

    spark-submit --master local[4] --class bsc.spark.image.Example1 --driver-library-path /home/rtous/aplic/opencv/release/lib target/bsc.spark-1.4.1-jar-with-dependencies.jar /home/rtous/Dropbox/recerca/PROJECTES_OFICIALS/BIGPICTURE/IMPL/BSC_SPARK/inputdata /home/rtous/tmp/out1

    */

    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static void main(String[] args) throws Exception {	 
        String inputDirPath = args[0]; //e.g. "/tmp/datasets
        String outputDirPath = args[1]; //e.g. "/tmp/datasets		
        String imagesSequenceFilePath = inputDirPath+"/images/*.hsf";	
        JavaSparkContext sc = new JavaSparkContext(new SparkConf().setAppName("Example1"));       	
        System.out.println("Testing Spark + OpenCV 2.4.9 with input path "+inputDirPath+"and ouput path "+outputDirPath);

        //JavaPairRDD<String, org.opencv.core.Mat> is our representation for images
        JavaPairRDD<String, org.opencv.core.Mat> images = ImageUtils.imagesSequenceFileToImagesRDD(imagesSequenceFilePath, sc);

        //Do something with the images
        JavaPairRDD<String, String> res = ImageUtils.columnsPerImage(images);
        List<Tuple2<String, String>> resList = res.collect();
        for (Tuple2<String,String> tuple : resList) {
            System.out.println(tuple._1() + ": "+tuple._2());		  
        }                
    }			       	
}

