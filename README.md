# bsc.spark

##1. Introduction
[TODO]
			
##2. Compilation and packaging

###2.1 Prerequisites

####2.1.1 Spark version

IMPORTANT: bsc.spark requires Spark 1.2.0 or greater

####2.1.2 OpenCV

NOTE: pom.xml has a property to select between opencv-249.jar and opencv-300.jar. 

NOTE: Currently the code only works with OpenCV 2.4.9.Some lines of code need to be changed to be able to compile OpenCV 3.0.0. 
The lines are commented with "OPENCV 3.0"

NOTE: See [this](http://personals.ac.upc.edu/rtous/howto_opencv_install_linux.xhtml) for information on how to install OpenCV

Two prerequisites:

1. opencv-249.jar (is required for compilation and has to be vailable to Spark somehow, packaged with the app's jar or through --jars) 
  - A "Class no found..." will be raised otherwise.
  - opencv-249.jar can be found in opencv build directory, within the "bin" folder.
  - It must be added manually to the local Maven repo:
		
          mvn install:install-file -DgroupId=opencv -DartifactId=opencv -Dpackaging=jar -Dversion=2.4.9 -Dfile=/OPENCV_BUILD/bin/opencv-249.jar
		  		          
2. libopencv_java249.so has to be accesible for Spark
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

###3.1 External jars 

If you prefer to decouple the application .jar and the dependencies (instead of building a uber jar containing everything) then you need to make the dependencies available to Spark at runtime. 
The following examples use the --jar option to do that. The "lib" folder contains the dependencies' jars. 
In the case of OpenCV it's not enough to provide the .jar, as Spark will also need the native library (see above for more information).

###3.2 Working with images 

####3.2.1 OpenCV HelloWorld

  $spark-submit --jars lib/opencv-249.jar --class "bsc.spark.image.ExampleOpenCVCheck" --driver-library-path /OPENCV_BUILD/lib/ --master local[4] target/bsc.spark-1.4.1.jar
  
####3.2.3 Read images from sequence files (and OpenCV)
  
  $spark-submit --jars lib/opencv-249.jar --class "bsc.spark.image.ExampleImages2RDD" --driver-library-path /OPENCV_BUILD/lib/ --master local[4] target/bsc.spark-1.4.3.jar INPUTDATAPATH
  
  NOTE: Where INPUTDATAPATH has the following structure (.hsf are Hadoop sequence files):
  
  NOTE: The metadata directory is not necessary for this example
```  
  INPUTDATAPATH|
               |-images
			   |     |-seqfile1.hsf
			   |     |-seqfile2.hsf 
			   |
			   |-metadata
			   |     |-seqfile1.hsf
			   |     |-seqfile2.hsf 
```
NOTE: See [this](http://personals.ac.upc.edu/rtous/howto_spark_opencv.xhtml) for information on how generate sequence files of images and metadata


###3.3 Spark performance testing

####3.3.1 Spark properties:

The list of general Spark properties (not specific to this library) is available [here](http://spark.apache.org/docs/1.3.0/configuration.html) (for v1.3.0). 
Most of the properties that control internal settings have reasonable default values. In case you need to change something there are three ways of proceeding, in order of priority:

	1) Properties set directly on the SparkConf (within the application code)
	2) Flags passed to spark-submit or spark-shell (e.g. --conf spark.shuffle.spill=false --conf "spark.executor.extraJavaOptions=-XX:+PrintGCDetails -XX:+PrintGCTimeStamps")
	3) spark-defaults.conf 
	
When evaluating the performane of a Spark deployment the following properties may be of special interest:

	spark.driver.cores	1	Number of cores to use for the driver process, only in cluster mode.
	spark.driver.maxResultSize	1g	Limit of total size of serialized results of all partitions for each Spark action (e.g. collect). Should be at least 1M, or 0 for unlimited. Jobs will be aborted if the total size is above this limit. Having a high limit may cause out-of-memory errors in driver (depends on spark.driver.memory and memory overhead of objects in JVM). Setting a proper limit can protect the driver from out-of-memory errors.
	spark.driver.memory	512m	Amount of memory to use for the driver process, i.e. where SparkContext is initialized. (e.g. 512m, 2g). 
	Note: In client mode, this config must not be set through the SparkConf directly in your application, because the driver JVM has already started at that point. Instead, please set this through the --driver-memory command line option or in your default properties file.
	spark.executor.memory	512m	Amount of memory to use per executor process, in the same format as JVM memory strings (e.g. 512m, 2g).
	spark.extraListeners	(none)	A comma-separated list of classes that implement SparkListener; when initializing SparkContext, instances of these classes will be created and registered with Spark's listener bus. If a class has a single-argument constructor that accepts a SparkConf, that constructor will be called; otherwise, a zero-argument constructor will be called. If no valid constructor can be found, the SparkContext creation will fail with an exception.
	spark.local.dir	/tmp	Directory to use for "scratch" space in Spark, including map output files and RDDs that get stored on disk. This should be on a fast, local disk in your system. It can also be a comma-separated list of multiple directories on different disks. NOTE: In Spark 1.0 and later this will be overriden by SPARK_LOCAL_DIRS (Standalone, Mesos) or LOCAL_DIRS (YARN) environment variables set by the cluster manager.
	spark.logConf	false	Logs the effective SparkConf as INFO when a SparkContext is started.
	spark.master	(none)
	
	conf.set("spark.akka.frameSize", "2046")
    conf.set("spark.network.timeout", "2400")
    conf.set("spark.akka.threads", "32")
    conf.set("spark.akka.timeout", "2400") 		
	spark.task.maxFailures set to 4

####3.3.2 spark-perf parameters
	
	-spark.eventLog.enabled True (DEFAULT: DISABLED!)
	-spark.storage.memoryFraction 0.66
	-spark.serializer org.apache.spark.serializer.JavaSerializer # or org.apache.spark.serializer.KryoSerializer
	-spark.executor.memory 9g
	
	-spark.locality.wait" 60*1000*1000 #To ensure consistency across runs, we disable delay scheduling
	
	
	-num-trials 1 # How many times to run each experiment. Used to warm up system caches.
	-inter-trial-wait 3 # Extra pause added between trials, in seconds. For runs with large amounts of shuffle data, this gives time for buffer cache write-back.
	
	
	
	# Input persistence strategy (can be "memory", "disk", or "hdfs").
    # NOTE: If "hdfs" is selected, datasets will be re-used across runs of
    #       this script. This means parameters here are effectively ignored if
    #       an existing input dataset is present.
    - persistent-type memory 
	
	
	
	
	
	
	
####3.3.2 sort-by-key

 spark-submit --jars lib/jopt-simple-4.9-beta-1.jar --class "bsc.spark.perf.spark.TestRunner" --master local[4] target/bsc.spark-1.4.3.jar sort-by-key -num-trials 1 -inter-trial-wait 5 -random-seed 5 -num-partitions 400 -reduce-tasks 400 -num-records 2000000 -unique-keys 200 -key-length 10 -unique-values 10000 -value-length 10 -persistent-type memory -storage-location "" 

####3.3.3 kmeans 

  $spark-submit --jars lib/jopt-simple-4.9-beta-1.jar --class "bsc.spark.perf.mllib.TestRunner" --master local[4] target/bsc.spark-1.4.1.jar kmeans -num-centers 5 -num-iterations 10 -num-partitions 10 -num-points 1000 -num-trials 1 -random-seed 5 -num-columns 100 -inter-trial-wait 3

####3.3.4 naive-bayes 

  $spark-submit --jars lib/jopt-simple-4.9-beta-1.jar --class "bsc.spark.perf.mllib.TestRunner" --master local[4] target/bsc.spark-1.4.1.jar naive-bayes -num-trials 1 -inter-trial-wait 3 -num-partitions 400 -random-seed 5 -num-examples 100000 -num-features 10000 -nb-lambda 1.0 -per-negative 0.3 -scale-factor 1.0

