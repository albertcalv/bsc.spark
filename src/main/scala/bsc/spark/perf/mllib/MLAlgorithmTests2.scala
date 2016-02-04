package bsc.spark.perf.mllib

import org.json4s.JsonDSL._
import org.json4s.JsonAST._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.mllib.classification._
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.clustering.{KMeansModel, KMeans}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.recommendation.{Rating, MatrixFactorizationModel, ALS}
import org.apache.spark.mllib.regression._
import org.apache.spark.mllib.tree.impurity.{Variance, Gini}
import org.apache.spark.mllib.tree.RandomForest
import org.apache.spark.mllib.tree.model.RandomForestModel
import org.apache.spark.rdd.RDD

import bsc.spark.perf.mllib.util.{DataLoader, DataGenerator}

/*
  NaiveBayesGenToDisk and classificationTeetsGenToDisk
*/

//Classification
class NaiveBayesGenToDisk(sc: SparkContext) extends ClassificationTestsGenToDisk(sc) {
  override def runTest(rdd: RDD[LabeledPoint]): NaiveBayesModel = {
    return null
  }
}

abstract class ClassificationTestsGenToDisk(sc: SparkContext) extends PerfTest {

  def runTest(rdd: RDD[LabeledPoint]): NaiveBayesModel

  val NUM_EXAMPLES =  ("num-examples",   "number of examples for regression tests")
  val NUM_FEATURES =  ("num-features",   "number of features of each example for regression tests")
  val THRESHOLD =  ("per-negative",   "probability for a negative label during data generation")
  val SCALE =  ("scale-factor",   "scale factor for the noise during data generation")
  val SMOOTHING =     ("nb-lambda",   "the smoothing parameter lambda for Naive Bayes")
  val PATH = ("path", "data directory path")

  intOptions = intOptions ++ Seq(NUM_FEATURES)
  doubleOptions = doubleOptions ++ Seq(THRESHOLD, SCALE, SMOOTHING)
  longOptions = longOptions ++ Seq(NUM_EXAMPLES)
  stringOptions = stringOptions ++ Seq(PATH)
  val options = intOptions ++ stringOptions ++ booleanOptions ++ longOptions ++ doubleOptions
  addOptionsToParser()

  var rdd: RDD[Vector] = _
  var testRdd: RDD[Vector] = _

  def validate(model: NaiveBayesModel, rdd: RDD[LabeledPoint]): Double = {
    val numPoints = rdd.cache().count()
    //val error = model.computeCost(rdd)
    val error = 100
    math.sqrt(error / numPoints)

  }

  override def createInputData(seed: Long) = {
    val numPartitions: Int = intOptionValue(NUM_PARTITIONS)
    val numExamples: Long = longOptionValue(NUM_EXAMPLES)
    val numFeatures: Int = intOptionValue(NUM_FEATURES)
    val threshold: Double = doubleOptionValue(THRESHOLD)
    val sf: Double = doubleOptionValue(SCALE)
    val path: String = stringOptionValue(PATH)

    val data = DataGenerator.generateClassificationLabeledPoints(sc,
      math.ceil(numExamples * 1.25).toLong, numFeatures, threshold, sf, numPartitions, seed)

    //RUBEN
    println("SAVING DATA TO DISK...")
    val dataStrings = data.map(v => v.toString())
    dataStrings.saveAsTextFile(path)
    println("DATA SAVED.")
    //RUBEN
  }

  override def run(): JValue = {
    var start = System.currentTimeMillis()
    //val model = runTest(rdd)
    val trainingTime = (System.currentTimeMillis() - start).toDouble / 1000.0

    start = System.currentTimeMillis()
    //val trainingMetric = validate(model, rdd)
    val trainingMetric = 0
    val testTime = (System.currentTimeMillis() - start).toDouble / 1000.0

    //val testMetric = validate(model, testRdd)
    val testMetric = 0
    Map("trainingTime" -> trainingTime, "testTime" -> testTime)
  }
}

/*
   NaiveBayesTestFromDisk and ClassificationTestsFromDisk
*/
class NaiveBayesTestFromDisk(sc: SparkContext) extends ClassificationTestsFromDisk(sc) {
  override def runTest(rdd: RDD[LabeledPoint]): NaiveBayesModel = {
    val lambda = doubleOptionValue(SMOOTHING)
    NaiveBayes.train(rdd, lambda)
    return null
  }
}

abstract class ClassificationTestsFromDisk(sc: SparkContext) extends PerfTest {

  def runTest(rdd: RDD[LabeledPoint]): NaiveBayesModel

  val NUM_EXAMPLES =  ("num-examples",   "number of examples for regression tests")
  val NUM_FEATURES =  ("num-features",   "number of features of each example for regression tests")
  val THRESHOLD =  ("per-negative",   "probability for a negative label during data generation")
  val SCALE =  ("scale-factor",   "scale factor for the noise during data generation")
  val SMOOTHING =     ("nb-lambda",   "the smoothing parameter lambda for Naive Bayes")
  val PATH = ("path", "data directory path")

  intOptions = intOptions ++ Seq(NUM_FEATURES)
  doubleOptions = doubleOptions ++ Seq(THRESHOLD, SCALE, SMOOTHING)
  longOptions = longOptions ++ Seq(NUM_EXAMPLES)
  stringOptions = stringOptions ++ Seq(PATH)
  val options = intOptions ++ stringOptions ++ booleanOptions ++ longOptions ++ doubleOptions
  addOptionsToParser()

  var rdd: RDD[Vector] = _
  var testRdd: RDD[Vector] = _

  def validate(model: NaiveBayesModel, rdd: RDD[Vector]): Double = {
    val numPoints = rdd.cache().count()
    val error = 100
    math.sqrt(error/numPoints)
  }

  override def createInputData(seed: Long) = {
	val path: String = stringOptionValue(PATH)

    //RUBEN
    println("LOADING DATA FROM DISK...")
    //println(bsc.spark.Loader.hdfsURI("/etc/hadoop/conf"))
    val dataLines = sc.textFile(path+"/*")
    val data = dataLines.map(s => Vectors.dense(s.split(' ').map(_.toDouble)))
    println("DATA LOADED.")
    //RUBEN

    val split = data.randomSplit(Array(0.8, 0.2), seed)

    rdd = split(0).cache()
    testRdd = split(1)

    // Materialize rdd
    println("Num Examples: " + rdd.count())
  }

  override def run(): JValue = {
    var start = System.currentTimeMillis()
    //val model = runTest(rdd)
    val trainingTime = (System.currentTimeMillis() - start).toDouble / 1000.0

    start = System.currentTimeMillis()
    //val trainingMetric = validate(model, rdd)
    val trainingMetric = 0
    val testTime = (System.currentTimeMillis() - start).toDouble / 1000.0

    //val testMetric = validate(model, testRdd)
    val testMetric = 0
    Map("trainingTime" -> trainingTime, "testTime" -> testTime)
  }
}


/*
   KMeansGenToDisk and ClusteringTestsGenToDisk
*/
// Clustering
class KMeansGenToDisk(sc: SparkContext) extends ClusteringTestsGenToDisk(sc) {
  override def runTest(rdd: RDD[Vector]): KMeansModel = {
    return null
  }

}

abstract class ClusteringTestsGenToDisk(sc: SparkContext) extends PerfTest {

  def runTest(rdd: RDD[Vector]): KMeansModel

  val NUM_POINTS =    ("num-points",   "number of points for clustering tests")
  val NUM_COLUMNS =   ("num-columns",   "number of columns for each point for clustering tests")
  val NUM_CENTERS =   ("num-centers",   "number of centers for clustering tests")
  val NUM_ITERATIONS =      ("num-iterations",   "number of iterations for the algorithm")
  val PATH =      ("path",   "data directory path")

  intOptions = intOptions ++ Seq(NUM_CENTERS, NUM_COLUMNS, NUM_ITERATIONS)
  longOptions = longOptions ++ Seq(NUM_POINTS)
  stringOptions = stringOptions ++ Seq(PATH)
  val options = intOptions ++ stringOptions  ++ booleanOptions ++ longOptions ++ doubleOptions
  addOptionsToParser()

  var rdd: RDD[Vector] = _
  var testRdd: RDD[Vector] = _

  def validate(model: KMeansModel, rdd: RDD[Vector]): Double = {
    val numPoints = rdd.cache().count()

    val error = model.computeCost(rdd)

    math.sqrt(error/numPoints)
  }

  override def createInputData(seed: Long) = {
    val numPartitions: Int = intOptionValue(NUM_PARTITIONS)

    val numPoints: Long = longOptionValue(NUM_POINTS)
    val numColumns: Int = intOptionValue(NUM_COLUMNS)
    val numCenters: Int = intOptionValue(NUM_CENTERS)
    val path: String = stringOptionValue(PATH)

    val data = DataGenerator.generateKMeansVectors(sc, math.ceil(numPoints*1.25).toLong, numColumns,
      numCenters, numPartitions, seed)

    //RUBEN
    println("SAVING DATA TO DISK...")
    val dataStrings = data.map(v => v.toArray.mkString(" "))
    dataStrings.saveAsTextFile(path)
    println("DATA SAVED.")
    //RUBEN
  }

  override def run(): JValue = {
    var start = System.currentTimeMillis()
    //val model = runTest(rdd)
    val trainingTime = (System.currentTimeMillis() - start).toDouble / 1000.0

    start = System.currentTimeMillis()
    //val trainingMetric = validate(model, rdd)
    val trainingMetric = 0
    val testTime = (System.currentTimeMillis() - start).toDouble / 1000.0

    //val testMetric = validate(model, testRdd)
    val testMetric = 0
    Map("trainingTime" -> trainingTime, "testTime" -> testTime)
  }
}

/*
   KMeansTestFromDisk and ClusteringTestsFromDisk
*/

// Clustering
class KMeansTestFromDisk(sc: SparkContext) extends ClusteringTestsFromDisk(sc) {
  override def runTest(rdd: RDD[Vector]): KMeansModel = {
    val numIterations: Int = intOptionValue(NUM_ITERATIONS)
    val k: Int = intOptionValue(NUM_CENTERS)
    KMeans.train(rdd, k, numIterations)
  }
}

abstract class ClusteringTestsFromDisk(sc: SparkContext) extends PerfTest {

  def runTest(rdd: RDD[Vector]): KMeansModel

  val NUM_POINTS =    ("num-points",   "number of points for clustering tests")
  val NUM_COLUMNS =   ("num-columns",   "number of columns for each point for clustering tests")
  val NUM_CENTERS =   ("num-centers",   "number of centers for clustering tests")
  val NUM_ITERATIONS =      ("num-iterations",   "number of iterations for the algorithm")
  val PATH =      ("path",   "data directory path")


  intOptions = intOptions ++ Seq(NUM_CENTERS, NUM_COLUMNS, NUM_ITERATIONS)
  longOptions = longOptions ++ Seq(NUM_POINTS)
  stringOptions = stringOptions ++ Seq(PATH)
  val options = intOptions ++ stringOptions  ++ booleanOptions ++ longOptions ++ doubleOptions
  addOptionsToParser()

  var rdd: RDD[Vector] = _
  var testRdd: RDD[Vector] = _

  def validate(model: KMeansModel, rdd: RDD[Vector]): Double = {
    val numPoints = rdd.cache().count()

    val error = model.computeCost(rdd)

    math.sqrt(error/numPoints)
  }

  override def createInputData(seed: Long) = {
	val path: String = stringOptionValue(PATH)

    //RUBEN
    println("LOADING DATA FROM DISK...")
    //println(bsc.spark.Loader.hdfsURI("/etc/hadoop/conf"))
    val dataLines = sc.textFile(path+"/*")
    val data = dataLines.map(s => Vectors.dense(s.split(' ').map(_.toDouble)))
    println("DATA LOADED.")
    //RUBEN

    val split = data.randomSplit(Array(0.8, 0.2), seed)

    rdd = split(0).cache()
    testRdd = split(1)

    // Materialize rdd
    println("Num Examples: " + rdd.count())


  }


  override def run(): JValue = {
    var start = System.currentTimeMillis()
    val model = runTest(rdd)
    val trainingTime = (System.currentTimeMillis() - start).toDouble / 1000.0

    start = System.currentTimeMillis()
    val trainingMetric = validate(model, rdd)
    val testTime = (System.currentTimeMillis() - start).toDouble / 1000.0

    val testMetric = validate(model, testRdd)
    Map("trainingTime" -> trainingTime, "testTime" -> testTime,
      "trainingMetric" -> trainingMetric, "testMetric" -> testMetric)
  }
}
