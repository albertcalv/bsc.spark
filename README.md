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

####3.3.1 sort-by-key

 spark-submit --jars lib/jopt-simple-4.9-beta-1.jar --class "bsc.spark.perf.spark.TestRunner" --master local[4] target/bsc.spark-1.4.3.jar sort-by-key -num-trials 1 -inter-trial-wait 5 -random-seed 5 -num-partitions 400 -reduce-tasks 400 -num-records 2000000 -unique-keys 200 -key-length 10 -unique-values 10000 -value-length 10 -persistent-type memory -storage-location "" 

####3.3.2 kmeans 

  $spark-submit --jars lib/jopt-simple-4.9-beta-1.jar --class "bsc.spark.perf.mllib.TestRunner" --master local[4] target/bsc.spark-1.4.1.jar kmeans -num-centers 5 -num-iterations 10 -num-partitions 10 -num-points 1000 -num-trials 1 -random-seed 5 -num-columns 100 -inter-trial-wait 3

####3.3.3 naive-bayes 

  $spark-submit --jars lib/jopt-simple-4.9-beta-1.jar --class "bsc.spark.perf.mllib.TestRunner" --master local[4] target/bsc.spark-1.4.1.jar naive-bayes -num-trials 1 -inter-trial-wait 3 -num-partitions 400 -random-seed 5 -num-examples 100000 -num-features 10000 -nb-lambda 1.0 -per-negative 0.3 -scale-factor 1.0

####3.3.5 spark-perf parameters
	
	-spark.eventLog.enabled True (DEFAULT: DISABLED!)
	-spark.storage.memoryFraction 0.66
	-spark.serializer org.apache.spark.serializer.JavaSerializer # or org.apache.spark.serializer.KryoSerializer
	-spark.executor.memory 9g	
	-spark.locality.wait" 60*1000*1000 #To ensure consistency across runs, we disable delay scheduling		
	-num-trials 1 # How many times to run each experiment. Used to warm up system caches.
	-inter-trial-wait 3 # Extra pause added between trials, in seconds. For runs with large amounts of shuffle data, this gives time for buffer cache write-back.	
    - persistent-type memory # Input persistence strategy (can be "memory", "disk", or "hdfs").
	
  
####3.3.1 Spark properties:

The list of general Spark properties (not specific to this library) is available [here](http://spark.apache.org/docs/1.3.0/configuration.html) (for v1.3.0). 
Most of the properties that control internal settings have reasonable default values. In case you need to change something there are three ways of proceeding, in order of priority:

1. Properties set directly on the SparkConf (within the application code)
2. Flags passed to spark-submit or spark-shell (e.g. --conf spark.shuffle.spill=false --conf "spark.executor.extraJavaOptions=-XX:+PrintGCDetails -XX:+PrintGCTimeStamps")
3. spark-defaults.conf 
	
When evaluating the performane of a Spark deployment the following properties may be of special interest:

Application Properties:

-spark.driver.maxResultSize	1g	Limit of total size of serialized results of all partitions for each Spark action (e.g. collect). Should be at least 1M, or 0 for unlimited. Jobs will be aborted if the total size is above this limit. Having a high limit may cause out-of-memory errors in driver (depends on spark.driver.memory and memory overhead of objects in JVM). Setting a proper limit can protect the driver from out-of-memory errors.
-spark.driver.memory	512m	Amount of memory to use for the driver process, i.e. where SparkContext is initialized. (e.g. 512m, 2g). 
-spark.executor.memory	512m	Amount of memory to use per executor process, in the same format as JVM memory strings (e.g. 512m, 2g).
-spark.local.dir	/tmp	Directory to use for "scratch" space in Spark, including map output files and RDDs that get stored on disk. This should be on a fast, local disk in your system. It can also be a comma-separated list of multiple directories on different disks. NOTE: In Spark 1.0 and later this will be overriden by SPARK_LOCAL_DIRS (Standalone, Mesos) or LOCAL_DIRS (YARN) environment variables set by the cluster manager.
-spark.logConf	false	Logs the effective SparkConf as INFO when a SparkContext is started.

Shuffle Behavior
	
	spark.reducer.maxMbInFlight	48	Maximum size (in megabytes) of map outputs to fetch simultaneously from each reduce task. Since each output requires us to create a buffer to receive it, this represents a fixed memory overhead per reduce task, so keep it small unless you have a large amount of memory.
	spark.shuffle.blockTransferService	netty	Implementation to use for transferring shuffle and cached blocks between executors. There are two implementations available: netty and nio. Netty-based block transfer is intended to be simpler but equally efficient and is the default option starting in 1.2.
	spark.shuffle.compress	true	Whether to compress map output files. Generally a good idea. Compression will use spark.io.compression.codec.
	spark.shuffle.consolidateFiles	false	If set to "true", consolidates intermediate files created during a shuffle. Creating fewer files can improve filesystem performance for shuffles with large numbers of reduce tasks. It is recommended to set this to "true" when using ext4 or xfs filesystems. On ext3, this option might degrade performance on machines with many (>8) cores due to filesystem limitations.
	spark.shuffle.file.buffer.kb	32	Size of the in-memory buffer for each shuffle file output stream, in kilobytes. These buffers reduce the number of disk seeks and system calls made in creating intermediate shuffle files.
	spark.shuffle.io.maxRetries	3	(Netty only) Fetches that fail due to IO-related exceptions are automatically retried if this is set to a non-zero value. This retry logic helps stabilize large shuffles in the face of long GC pauses or transient network connectivity issues.
	spark.shuffle.io.numConnectionsPerPeer	1	(Netty only) Connections between hosts are reused in order to reduce connection buildup for large clusters. For clusters with many hard disks and few hosts, this may result in insufficient concurrency to saturate all disks, and so users may consider increasing this value.
	spark.shuffle.io.preferDirectBufs	true	(Netty only) Off-heap buffers are used to reduce garbage collection during shuffle and cache block transfer. For environments where off-heap memory is tightly limited, users may wish to turn this off to force all allocations from Netty to be on-heap.
	spark.shuffle.io.retryWait	5	(Netty only) Seconds to wait between retries of fetches. The maximum delay caused by retrying is simply maxRetries * retryWait, by default 15 seconds.
	spark.shuffle.manager	sort	Implementation to use for shuffling data. There are two implementations available: sort and hash. Sort-based shuffle is more memory-efficient and is the default option starting in 1.2.
	spark.shuffle.memoryFraction	0.2	Fraction of Java heap to use for aggregation and cogroups during shuffles, if spark.shuffle.spill is true. At any given time, the collective size of all in-memory maps used for shuffles is bounded by this limit, beyond which the contents will begin to spill to disk. If spills are often, consider increasing this value at the expense of spark.storage.memoryFraction.
	spark.shuffle.sort.bypassMergeThreshold	200	(Advanced) In the sort-based shuffle manager, avoid merge-sorting data if there is no map-side aggregation and there are at most this many reduce partitions.
	spark.shuffle.spill	true	If set to "true", limits the amount of memory used during reduces by spilling data out to disk. This spilling threshold is specified by spark.shuffle.memoryFraction.
	spark.shuffle.spill.compress

Compression and Serialization

	spark.broadcast.compress	true	Whether to compress broadcast variables before sending them. Generally a good idea.
	spark.closure.serializer	org.apache.spark.serializer. JavaSerializer	Serializer class to use for closures. Currently only the Java serializer is supported.
	spark.io.compression.codec	snappy	The codec used to compress internal data such as RDD partitions, broadcast variables and shuffle outputs. By default, Spark provides three codecs: lz4, lzf, and snappy. You can also use fully qualified class names to specify the codec, e.g. org.apache.spark.io.LZ4CompressionCodec, org.apache.spark.io.LZFCompressionCodec, and org.apache.spark.io.SnappyCompressionCodec.
	spark.io.compression.lz4.block.size	32768	Block size (in bytes) used in LZ4 compression, in the case when LZ4 compression codec is used. Lowering this block size will also lower shuffle memory usage when LZ4 is used.
	spark.io.compression.snappy.block.size	32768	Block size (in bytes) used in Snappy compression, in the case when Snappy compression codec is used. Lowering this block size will also lower shuffle memory usage when Snappy is used.
	spark.kryo.classesToRegister	(none)	If you use Kryo serialization, give a comma-separated list of custom class names to register with Kryo. See the tuning guide for more details.
	spark.kryo.referenceTracking	true	Whether to track references to the same object when serializing data with Kryo, which is necessary if your object graphs have loops and useful for efficiency if they contain multiple copies of the same object. Can be disabled to improve performance if you know this is not the case.
	spark.kryo.registrationRequired	false	Whether to require registration with Kryo. If set to 'true', Kryo will throw an exception if an unregistered class is serialized. If set to false (the default), Kryo will write unregistered class names along with each object. Writing class names can cause significant performance overhead, so enabling this option can enforce strictly that a user has not omitted classes from registration.
	spark.kryo.registrator	(none)	If you use Kryo serialization, set this class to register your custom classes with Kryo. This property is useful if you need to register your classes in a custom way, e.g. to specify a custom field serializer. Otherwise spark.kryo.classesToRegister is simpler. It should be set to a class that extends KryoRegistrator. See the tuning guide for more details.
	spark.kryoserializer.buffer.max.mb	64	Maximum allowable size of Kryo serialization buffer, in megabytes. This must be larger than any object you attempt to serialize. Increase this if you get a "buffer limit exceeded" exception inside Kryo.
	spark.kryoserializer.buffer.mb	0.064	Initial size of Kryo's serialization buffer, in megabytes. Note that there will be one buffer per core on each worker. This buffer will grow up to spark.kryoserializer.buffer.max.mb if needed.
	spark.rdd.compress	false	Whether to compress serialized RDD partitions (e.g. for StorageLevel.MEMORY_ONLY_SER). Can save substantial space at the cost of some extra CPU time.
	spark.serializer	org.apache.spark.serializer. JavaSerializer	Class to use for serializing objects that will be sent over the network or need to be cached in serialized form. The default of Java serialization works with any Serializable Java object but is quite slow, so we recommend using org.apache.spark.serializer.KryoSerializer and configuring Kryo serialization when speed is necessary. Can be any subclass of org.apache.spark.Serializer.
	spark.serializer.objectStreamReset	100

Execution Behavior

	spark.broadcast.blockSize	4096	Size of each piece of a block in kilobytes for TorrentBroadcastFactory. Too large a value decreases parallelism during broadcast (makes it slower); however, if it is too small, BlockManager might take a performance hit.
	spark.broadcast.factory	org.apache.spark.broadcast. TorrentBroadcastFactory	Which broadcast implementation to use.
	spark.cleaner.ttl	(infinite)	Duration (seconds) of how long Spark will remember any metadata (stages generated, tasks generated, etc.). Periodic cleanups will ensure that metadata older than this duration will be forgotten. This is useful for running Spark for many hours / days (for example, running 24/7 in case of Spark Streaming applications). Note that any RDD that persists in memory for more than this duration will be cleared as well.
	spark.default.parallelism	For distributed shuffle operations like reduceByKey and join, the largest number of partitions in a parent RDD. 
								For operations like parallelize with no parent RDDs, it depends on the cluster manager:
									Local mode: number of cores on the local machine
									Mesos fine grained mode: 8
									Others: total number of cores on all executor nodes or 2, whichever is larger
									Default number of partitions in RDDs returned by transformations like join, reduceByKey, and parallelize when not set by user.
	spark.executor.heartbeatInterval	10000	Interval (milliseconds) between each executor's heartbeats to the driver. Heartbeats let the driver know that the executor is still alive and update it with metrics for in-progress tasks.
	spark.files.fetchTimeout	60	Communication timeout to use when fetching files added through SparkContext.addFile() from the driver, in seconds.
	spark.files.overwrite	false	Whether to overwrite files added through SparkContext.addFile() when the target file exists and its contents do not match those of the source.
	spark.hadoop.cloneConf	false	If set to true, clones a new Hadoop Configuration object for each task. This option should be enabled to work around Configuration thread-safety issues (see SPARK-2546 for more details). This is disabled by default in order to avoid unexpected performance regressions for jobs that are not affected by these issues.
	spark.hadoop.validateOutputSpecs	true	If set to true, validates the output specification (e.g. checking if the output directory already exists) used in saveAsHadoopFile and other variants. This can be disabled to silence exceptions due to pre-existing output directories. We recommend that users do not disable this except if trying to achieve compatibility with previous versions of Spark. Simply use Hadoop's FileSystem API to delete output directories by hand. This setting is ignored for jobs generated through Spark Streaming's StreamingContext, since data may need to be rewritten to pre-existing output directories during checkpoint recovery.
	spark.storage.memoryFraction	0.6	Fraction of Java heap to use for Spark's memory cache. This should not be larger than the "old" generation of objects in the JVM, which by default is given 0.6 of the heap, but you can increase it if you configure your own old generation size.
	spark.storage.memoryMapThreshold	2097152	Size of a block, in bytes, above which Spark memory maps when reading a block from disk. This prevents Spark from memory mapping very small blocks. In general, memory mapping has high overhead for blocks close to or below the page size of the operating system.
	spark.storage.unrollFraction	0.2	Fraction of spark.storage.memoryFraction to use for unrolling blocks in memory. This is dynamically allocated by dropping existing blocks when there is not enough free storage space to unroll the new block in its entirety.
	spark.tachyonStore.baseDir	System.getProperty("java.io.tmpdir")	Directories of the Tachyon File System that store RDDs. The Tachyon file system's URL is set by spark.tachyonStore.url. It can also be a comma-separated list of multiple directories on Tachyon file system.
	spark.tachyonStore.url	tachyon://localhost:19998	The URL of the underlying Tachyon file system in the TachyonStore.

Networking

	spark.akka.failure-detector.threshold	300.0	This is set to a larger value to disable failure detector that comes inbuilt akka. It can be enabled again, if you plan to use this feature (Not recommended). This maps to akka's `akka.remote.transport-failure-detector.threshold`. Tune this in combination of `spark.akka.heartbeat.pauses` and `spark.akka.heartbeat.interval` if you need to.
	spark.akka.frameSize	10	Maximum message size to allow in "control plane" communication (for serialized tasks and task results), in MB. Increase this if your tasks need to send back large results to the driver (e.g. using collect() on a large dataset).
	spark.akka.heartbeat.interval	1000	This is set to a larger value to disable the transport failure detector that comes built in to Akka. It can be enabled again, if you plan to use this feature (Not recommended). A larger interval value in seconds reduces network overhead and a smaller value ( ~ 1 s) might be more informative for Akka's failure detector. Tune this in combination of `spark.akka.heartbeat.pauses` if you need to. A likely positive use case for using failure detector would be: a sensistive failure detector can help evict rogue executors quickly. However this is usually not the case as GC pauses and network lags are expected in a real Spark cluster. Apart from that enabling this leads to a lot of exchanges of heart beats between nodes leading to flooding the network with those.
	spark.akka.heartbeat.pauses	6000	This is set to a larger value to disable the transport failure detector that comes built in to Akka. It can be enabled again, if you plan to use this feature (Not recommended). Acceptable heart beat pause in seconds for Akka. This can be used to control sensitivity to GC pauses. Tune this along with `spark.akka.heartbeat.interval` if you need to.
	spark.akka.threads	4	Number of actor threads to use for communication. Can be useful to increase on large clusters when the driver has a lot of CPU cores.
	spark.akka.timeout	100	Communication timeout between Spark nodes, in seconds.
	spark.blockManager.port	(random)	Port for all block managers to listen on. These exist on both the driver and the executors.
	spark.broadcast.port	(random)	Port for the driver's HTTP broadcast server to listen on. This is not relevant for torrent broadcast.
	spark.driver.host	(local hostname)	Hostname or IP address for the driver to listen on. This is used for communicating with the executors and the standalone Master.
	spark.driver.port	(random)	Port for the driver to listen on. This is used for communicating with the executors and the standalone Master.
	spark.executor.port	(random)	Port for the executor to listen on. This is used for communicating with the driver.
	spark.fileserver.port	(random)	Port for the driver's HTTP file server to listen on.
	spark.network.timeout	120	Default timeout for all network interactions, in seconds. This config will be used in place of spark.core.connection.ack.wait.timeout, spark.akka.timeout, spark.storage.blockManagerSlaveTimeoutMs or spark.shuffle.io.connectionTimeout, if they are not configured.
	spark.port.maxRetries	16	Default maximum number of retries when binding to a port before giving up.
	spark.replClassServer.port	(random)	Port for the driver's HTTP class server to listen on. This is only relevant for the Spark shell.
	
Scheduling

	spark.cores.max	(not set)	When running on a standalone deploy cluster or a Mesos cluster in "coarse-grained" sharing mode, the maximum amount of CPU cores to request for the application from across the cluster (not from each machine). If not set, the default will be spark.deploy.defaultCores on Spark's standalone cluster manager, or infinite (all available cores) on Mesos.
	spark.localExecution.enabled	false	Enables Spark to run certain jobs, such as first() or take() on the driver, without sending tasks to the cluster. This can make certain jobs execute very quickly, but may require shipping a whole partition of data to the driver.
	spark.locality.wait	3000	Number of milliseconds to wait to launch a data-local task before giving up and launching it on a less-local node. The same wait will be used to step through multiple locality levels (process-local, node-local, rack-local and then any). It is also possible to customize the waiting time for each level by setting spark.locality.wait.node, etc. You should increase this setting if your tasks are long and see poor locality, but the default usually works well.
	spark.locality.wait.node	spark.locality.wait	Customize the locality wait for node locality. For example, you can set this to 0 to skip node locality and search immediately for rack locality (if your cluster has rack information).
	spark.locality.wait.process	spark.locality.wait	Customize the locality wait for process locality. This affects tasks that attempt to access cached data in a particular executor process.
	spark.locality.wait.rack	spark.locality.wait	Customize the locality wait for rack locality.
	spark.scheduler.maxRegisteredResourcesWaitingTime	30000	Maximum amount of time to wait for resources to register before scheduling begins (in milliseconds).
	spark.scheduler.minRegisteredResourcesRatio	0.8 for YARN mode; 0.0 otherwise	The minimum ratio of registered resources (registered resources / total expected resources) (resources are executors in yarn mode, CPU cores in standalone mode) to wait for before scheduling begins. Specified as a double between 0.0 and 1.0. Regardless of whether the minimum ratio of resources has been reached, the maximum amount of time it will wait before scheduling begins is controlled by config spark.scheduler.maxRegisteredResourcesWaitingTime.
	spark.scheduler.mode	FIFO	The scheduling mode between jobs submitted to the same SparkContext. Can be set to FAIR to use fair sharing instead of queueing jobs one after another. Useful for multi-user services.
	spark.scheduler.revive.interval	1000	The interval length for the scheduler to revive the worker resource offers to run tasks (in milliseconds).
	spark.speculation	false	If set to "true", performs speculative execution of tasks. This means if one or more tasks are running slowly in a stage, they will be re-launched.
	spark.speculation.interval	100	How often Spark will check for tasks to speculate, in milliseconds.
	spark.speculation.multiplier	1.5	How many times slower a task is than the median to be considered for speculation.
	spark.speculation.quantile	0.75	Percentage of tasks which must be complete before speculation is enabled for a particular stage.
	spark.task.cpus	1	Number of cores to allocate for each task.
	spark.task.maxFailures	4	Number of individual task failures before giving up on the job. Should be greater than or equal to 1. Number of allowed retries = this value - 1.	
	
			
	


	
	
	
	