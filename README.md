# bsc.spark

##1. Introduction
[TODO]
			
##2. Compilation and packaging

###2.1 Prerequisites

####2.1.1 Spark version

    IMPORTANT: bsc.spark requires Spark 1.2.0 or greater

####2.1.2 OpenCV

    NOTE: pom.xml has a property to select between opencv-249.jar and opencv-300.jar. 
	NOTE: Currently the code only works with OpenCV 2.4.9.
		  Some lines of code need to be changed to be able to compile OpenCV 3.0.0.
	      The lines are commented with "OPENCV 3.0"

    Two prerequisites:

    1) opencv-249.jar (is required for compilation and has to be vailable to Spark somehow, packaged with the app's jar or through --jars) 
        - A "Class no found..." will be raised otherwise.
        - opencv-249.jar can be found in opencv build directory, within the "bin" folder.
        - It must be added manually to the local Maven repo:
		
          mvn install:install-file -DgroupId=opencv -DartifactId=opencv -Dpackaging=jar -Dversion=2.4.9 -Dfile=/OPENCV_BUILD/bin/opencv-249.jar
		  		          
    2) libopencv_java249.so has to be accesible for Spark
        - A UnsatisfiedLinkError will be raised otherwise.
        - The file can be found in opencv build directory, within the "lib" folder.
        - One alternative is to copy the file within /usr/lib/ (Spark will look there)
        - Another alternative is to use the "driver-library-path" spark-submit directive this way:
        
		--driver-library-path /OPENCV_BUILD/lib
		NOTE: This also works in Windows
		NOTE: In Windows the native library can be found in OPENCV_BUILD\build\java\x64

###2.3 Compilation and packaging with Maven

	$mvn package

	If for some reason you need to create an uber jar, an application jar including all the dependencies, it may be obtained this way:

    $mvn clean compile assembly:single
	
	NOTE: when using an uber jar, add or remove the "provided" directive from pom.xml to exclude or include dependencies. 		

	
##3. Testing

###3.1 kmeans 

  $spark-submit --jars lib/jopt-simple-4.9-beta-1.jar --class "bsc.spark.perf.mllib.TestRunner" --master local[4] target/bsc.spark-1.4.1.jar kmeans -num-centers 5 -num-iterations 10 -num-partitions 10 -num-points 1000 -num-trials 1 -random-seed 5 -num-columns 100 -inter-trial-wait 3

###3.2 naive-bayes 

  $spark-submit --jars lib/jopt-simple-4.9-beta-1.jar --class "bsc.spark.perf.mllib.TestRunner" --master local[4] target/bsc.spark-1.4.1.jar naive-bayes -num-trials 1 -inter-trial-wait 3 -num-partitions 400 -random-seed 5 -num-examples 100000 -num-features 10000 -nb-lambda 1.0 -per-negative 0.3 -scale-factor 1.0

###3.3 OpenCV check

  $spark-submit --jars lib/opencv-249.jar --class "bsc.spark.image.ExampleOpenCVCheck" --driver-library-path /OPENCV_BUILD/lib/ --master local[4] target/bsc.spark-1.4.1.jar
  
###3.4 Read images from sequence files (and OpenCV)

  $spark-submit --jars lib/opencv-249.jar --class "bsc.spark.image.instagram.InstagramImageUtils" --driver-library-path /OPENCV_BUILD/lib/ --master local[4] target/bsc.spark-1.4.1.jar INPUTDATAPATH OUTPUTDATAPATH


    
