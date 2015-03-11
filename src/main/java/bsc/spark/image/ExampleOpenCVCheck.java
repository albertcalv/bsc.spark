package bsc.spark.image;

import org.apache.spark.api.java.*;
import org.apache.spark.SparkConf;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import java.util.Arrays;
import org.opencv.core.CvType;
import org.opencv.core.Scalar;

public class ExampleOpenCVCheck {
        
    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static void main(String[] args) throws Exception {	         
        JavaSparkContext sc = new JavaSparkContext(new SparkConf().setAppName("ExampleOpenCVCheck"));       	        
        System.out.println("Welcome to OpenCV " + Core.VERSION);
		Mat m = new Mat(5, 10, CvType.CV_8UC1, new Scalar(0));
		System.out.println("OpenCV Mat: " + m);
		Mat mr1 = m.row(1);
		mr1.setTo(new Scalar(1));
		Mat mc5 = m.col(5);
		mc5.setTo(new Scalar(5));
		System.out.println("OpenCV Mat data:\n" + m.dump());            
    }			       	
}

