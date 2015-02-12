package bsc.spark.image.instagram;

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


public class InstagramImageUtils {

  static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

  public static void main(String[] args) {	 
  
  
	String inputDirPath = args[0]; //e.g. "/tmp/datasets
	String outputDirPath = args[1]; //e.g. "/tmp/datasets
				
    String imagesSequenceFile = inputDirPath+"/images/*.hsf";
	String metadataSequenceFile = inputDirPath+"/metadata/*.hsf";
    SparkConf conf = new SparkConf().setAppName("SequenceFile Test");    	
    JavaSparkContext sc = new JavaSparkContext(conf);       	
	
	System.out.println("Testing Spark + OpenCV 2.4.9 with input path "+inputDirPath+"and ouput path "+outputDirPath);
	
	//1) get a non serializable RDD with the images
    JavaPairRDD<Text,BytesWritable> imagesRDD_ = sc.sequenceFile(imagesSequenceFile, Text.class, BytesWritable.class); 	
			
	//2) get a serializable RDD with the information extracted from the images
	JavaPairRDD<String, String> imagesRDD = imagesRDD_.mapToPair(
		new PairFunction<Tuple2<Text,BytesWritable>,String,String>() {
			@Override
			public Tuple2<String, String> call(Tuple2<Text, BytesWritable> kv) throws Exception {   
					String filename = kv._1.toString();
					String filenameWithoutExtension = FilenameUtils.getName(filename);      
					Mat image = byteswritableToOpenCVMat(kv._2);        
					return new Tuple2(filenameWithoutExtension, image.cols()+"");
			}
		}
	);
		

    //1) get a non serializable RDD with the metadata
    JavaPairRDD<Text,BytesWritable> metadataRDD_ = sc.sequenceFile(metadataSequenceFile, Text.class, BytesWritable.class);
	JavaPairRDD<String, String> metadataRDD = metadataRDD_.mapToPair(
		new PairFunction<Tuple2<Text,BytesWritable>,String,String>() {
			@Override
			public Tuple2<String, String> call(Tuple2<Text, BytesWritable> kv) throws Exception {        
				String metadataString = byteswritableToString(kv._2);
				String userName = "";
				try {
					//System.out.println("parsing:"+metadataString);
					JSONParser parser = new JSONParser();
					JSONObject jsonObject  = (JSONObject) parser.parse(metadataString.trim());
					JSONObject userObj = (JSONObject) jsonObject.get("user");
					String id = (String) userObj.get("name");
					userName = (String) userObj.get("username");
					String fullName = (String) userObj.get("full_name");
					String profilePicture = (String) userObj.get("profile_picture");
					/*JSONArray tags = (JSONArray) jsonObject.get("tags");
					Iterator<String> iterator = tags.iterator();
					while (iterator.hasNext()) {
						System.out.println(iterator.next());*/
				 } catch (Exception e) {
						System.out.println("error parsing file "+kv._1.toString()+": "+e.getMessage());
						e.printStackTrace();
				}

				String filename = kv._1.toString();
				String filenameWithoutExtension = FilenameUtils.getBaseName(filename); 				        
				return new Tuple2(filenameWithoutExtension, userName);
			}
		}
	);	 
		//print the metadataRDD             
    /*List<Tuple2<String, String>> output2 = metadataRDD.collect();
		for (Tuple2<String,String> tuple : output2) {
			System.out.println(tuple._1() + ": "+tuple._2());		  
		}*/	  

	 JavaPairRDD<String, Tuple2<String, String>> joinRDD = imagesRDD.join(metadataRDD);
 	 //print the metadataRDD            
	 List<Tuple2<String, Tuple2<String, String>>> output3 = joinRDD.collect();
	 for (Tuple2<String,Tuple2<String, String>> tuple : output3) {
	 	System.out.println(tuple._1() + ": "+tuple._2()._1() + ": "+tuple._2()._2());		  
	 }
	 System.out.println("count = "+joinRDD.count());
	 System.out.println("Saving results to "+outputDirPath+"...");
	 joinRDD.saveAsTextFile(outputDirPath);
  }		
	
    /* HELPER FUNCTIONS */
    /*
	public static Mat featurize(BytesWritable inputBW)  throws Exception {
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
	}*/

	/* HELPER FUNCTIONS */    
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
    
    //metadata
	public static String byteswritableToString(BytesWritable inputBW)  throws Exception {
		byte[] imageFileBytes = inputBW.getBytes();			
		String metadataString = new String(imageFileBytes, 0, inputBW.getLength(), java.nio.charset.Charset.forName("Cp1252"));		
		//String metadataString = new String(imageFileBytes, inputBW.getLength());		
		return metadataString;
	}	
}

