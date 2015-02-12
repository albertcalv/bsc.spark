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


//public class ImageRDD extends JavaPairRDD<Text,org.opencv.core.Mat> {
public class ImageUtils {

    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }    	
        
    public static JavaPairRDD<String,org.opencv.core.Mat> imagesSequenceFileToImagesRDD(String imagesSequenceFilesPath, JavaSparkContext sc) throws Exception {
	    //1) get a non serializable RDD with the images
        JavaPairRDD<Text,BytesWritable> imagesRDD_ = sc.sequenceFile(imagesSequenceFilesPath, Text.class, BytesWritable.class); 	
		
        //2) get a serializable RDD with the information extracted from the images
        JavaPairRDD<String, org.opencv.core.Mat> imagesRDD = imagesRDD_.mapToPair(
	        new PairFunction<Tuple2<Text,BytesWritable>,String,org.opencv.core.Mat>() {
		        @Override
		        public Tuple2<String, org.opencv.core.Mat> call(Tuple2<Text, BytesWritable> kv) throws Exception {   
				        String filename = kv._1.toString();
				        String filenameWithoutExtension = FilenameUtils.getName(filename);      
				        Mat image = byteswritableToOpenCVMat(kv._2);        
				        return new Tuple2(filenameWithoutExtension, image);
		        }
	        }
        );

        return imagesRDD;
    }

    public static JavaPairRDD<String, String> columnsPerImage(JavaPairRDD<String, org.opencv.core.Mat> images) throws Exception { 			       
        JavaPairRDD<String, String> res = images.mapToPair(
	        new PairFunction<Tuple2<String,org.opencv.core.Mat>,String,String>() {
		        @Override
		        public Tuple2<String, String> call(Tuple2<String, org.opencv.core.Mat> kv) throws Exception { 				               
				        return new Tuple2(kv._1, kv._2.cols()+"");
		        }
	        }
        );
        return res;
    }
    
    public static Mat byteswritableToOpenCVMat(BytesWritable inputBW)  throws Exception {
	    byte[] imageFileBytes = inputBW.getBytes();
	    Mat img = new Mat();
	    Byte[] bigByteArray = new Byte[inputBW.getLength()];
	    for (int i=0; i<inputBW.getLength(); i++)						
		    bigByteArray[i] = new Byte(imageFileBytes[i]);			
	    List<Byte> matlist = Arrays.asList(bigByteArray);		
	    img = Converters.vector_char_to_Mat(matlist);
	    //img = Imgcodecs.imdecode(img, Imgcodecs.CV_LOAD_IMAGE_COLOR); OPENCV 3.0
	    img = Highgui.imdecode(img, Highgui.CV_LOAD_IMAGE_COLOR); 		
	    return img;
    }

	
}

