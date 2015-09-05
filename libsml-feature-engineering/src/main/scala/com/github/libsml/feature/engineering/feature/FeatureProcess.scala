package com.github.libsml.feature.engineering.feature

import com.github.libsml.math.linalg.Vector
import com.github.libsml.math.util.VectorUtils
import org.apache.avro.generic.{GenericRecord, GenericData}
import org.apache.avro.mapred.{AvroJob, AvroOutputFormat, AvroWrapper}
import org.apache.hadoop.io.NullWritable
import com.github.libsml.model.data.avro.Scheme
import org.apache.hadoop.mapred.JobConf
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.collection.mutable

/**
 * Created by huangyu on 15/8/31.
 */
object FeatureProcess {

  //-i input,-o output
  def save2Avro(rdd: RDD[DataPoint], path: String): Unit = {
    val rddTmp = rdd.map(dp => {
      val datum: GenericRecord = new GenericData.Record(Scheme.WEIGHTED_LABEL_SCHEME)
      datum.put("y", dp.label)
      //      if (dp.weight != 1.0) {
      datum.put("weight", dp.weight)
      //      }
      val features = new ArrayBuffer[GenericRecord]()

      var base = 0
      var i = 0
      while (i < dp.groups.length) {

        dp.groups(i).features.foreachNoZero((index, value) => {
          val entry = new GenericData.Record(Scheme.ENTRY)
          entry.put("index", index + base)
          entry.put("value", value)
          features += entry
        })

        base += dp.groups(i).length
        i += 1
      }
      datum.put("Features", features.toArray)
      (new AvroWrapper[GenericRecord](datum), NullWritable.get())
    })

    val jobConf = new JobConf(rdd.sparkContext.hadoopConfiguration)
    AvroJob.setOutputSchema(jobConf, Scheme.WEIGHTED_LABEL_SCHEME)

    rddTmp.saveAsHadoopFile(path, classOf[AvroWrapper[GenericRecord]], classOf[NullWritable],
      classOf[AvroOutputFormat[GenericRecord]], jobConf)
  }

  def combine(rdd: RDD[DataPoint], index1: Int, index2: Int, isPutBack: Boolean = false): RDD[DataPoint] = {
    rdd.map(dp => {
      val group = dp.groups(index1) * dp.groups(index2)
      if (!isPutBack) {
        dp.groups.remove(Math.min(index1, index2))
        dp.groups.remove(Math.max(index1, index2) - 1)
      }
      dp.groups += group
      dp
    })
  }

  def groupByKey(rdd: RDD[String], index: Int, numPartitions: Int): RDD[String] = {
    rdd.map(line => {
      val ss = line.split("\\s+|,")
      (ss(index), concat(ss, index, ":"))
    }).reduceByKey(_ + "|" + _, numPartitions).map(kv => kv._1 + "\t" + kv._2)
  }

  def filter(rdd: RDD[String], indices: Array[Int]): RDD[String] = {

    def contains(index: Int): Boolean = {
      for (i <- indices) {
        if (index == i) return true
      }
      false
    }
    rdd.map(line => {
      val ss = line.split("\\s+|,")
      var isAdd = false
      val sb = new StringBuilder()
      var i = 0
      while (i < ss.length) {
        if (!contains(i)) {
          if (isAdd) {
            sb.append('\t')
          }
          sb.append(ss(i))
          isAdd = true
        }
        i += 1
      }
      sb.toString()
    })
  }


  def map(rdd: RDD[String], indices: Array[Int], functions: Array[Option[String => FeatureGroup]],
          labelIndex: Int, weightIndex: Int = -1): RDD[DataPoint] = {
    rdd.map(line => {
      val ss = line.split("\\s+|,")
      val dp = new DataPoint(ss(labelIndex).toDouble, if (weightIndex > 0) ss(weightIndex).toDouble else 1.0,
        new ArrayBuffer[FeatureGroup]())
      var i = 0
      while (i < indices.length) {
        functions(i).map(dp.groups += _(ss(indices(i))))
        i += 1
      }
      dp
    })
  }

  //map_filter -i path -m path
  def map(rdd: RDD[String], map: Map[String, String]): RDD[String] = {
    rdd.map(line => {
      val ss = line.split("\\s+|,")
      var i = 0
      while (i < ss.length) {
        map.get(ss(i)).foreach(ss(i) = _)
        i += 1
      }
      concat(ss)
    })
  }


  /**
   *
   * @param main
   * @param numReduce
   * @param features
   * @return
   * mid,...fid1,...fid2,...,mf1,...
   * fid1,ff11,ff12,...
   * fid2,ff21,ff22,...
   * ...
   * => mid,...fid1,...fid2,...,mf1,...,ff1,ff2,...,ff21,ff22,...
   */
  def join(main: RDD[String], numReduce: Int, features: JoinMessage*): RDD[String] = {

    var mainRdd = main

    for (feature <- features) {
      val mainMap = mainRdd.map(line => {
        val ss = line.split("\\s+|,")
        (ss(feature.mainKey), line)
      })
      val featureMap = feature.rdd.map(line => {
        val ss = line.split("\\s+|,")
        (ss(feature.featureKey), concat(ss, feature.featureKey))
      })

      mainRdd = mainMap.join(featureMap, numReduce).map(kv => {
        kv._2._1 + "\t" + kv._2._2
      })
    }
    mainRdd
  }


  //map_fg -i path -index 2,3,... -f map_path_5000,map_path_5000,...
  def mapMain(sc: SparkContext, args: Array[String],
              rdds: Option[Array[RDD[_]]] = None): RDD[DataPoint] = {
    val arg = new MapArguments(args)
    val rdd = getRDD(sc, arg.input, arg.numMap, rdds).asInstanceOf[RDD[String]]
    val funs: Array[Option[String => FeatureGroup]] = arg.functions.toArray.map(d => Some(getFuction(d)))
    map(rdd, arg.indices.toArray, funs, arg.labelIndex, arg.weightIndex)
  }


  //filter -i path -index 2,3,...
  def filterMain(sc: SparkContext, args: Array[String],
                 rdds: Option[Array[RDD[_]]] = None): RDD[String] = {
    val arg = new FilterArguments(args)
    val rdd = getRDD(sc, arg.input, arg.numMap, rdds).asInstanceOf[RDD[String]]
    val frdd = filter(rdd, arg.indices.toArray)
    if (arg.output != null) {
      frdd.saveAsTextFile(arg.output)
    }
    frdd
  }

  //combine -i1 2 -i2 2 -p
  def avroMain(sc: SparkContext, args: Array[String],
               rdds: Option[Array[RDD[_]]] = None): RDD[DataPoint] = {
    val arg = new AvroArguments(args)
    val rdd = getRDD(sc, arg.input, -1, rdds).asInstanceOf[RDD[DataPoint]]
    save2Avro(rdd, arg.output)
    rdd
  }

  //combine -i1 2 -i2 2 -p
  def combineMain(sc: SparkContext, args: Array[String],
                  rdds: Option[Array[RDD[_]]] = None): RDD[DataPoint] = {
    val arg = new CombineArguments(args)
    val rdd = getRDD(sc, arg.input, arg.numMap, rdds).asInstanceOf[RDD[DataPoint]]
    combine(rdd, arg.index1, arg.index2, arg.isPutBack)
  }

  def joinMain(sc: SparkContext, args: Array[String],
               rdds: Option[Array[RDD[_]]] = None): RDD[String] = {
    val arg = new JoinArguments(args)
    val rdd = join(
      getRDD(sc, arg.mainPath, arg.numMap, rdds).asInstanceOf[RDD[String]],
      arg.numReduce,
      arg.joinMessage.map(jm => {
        new JoinMessage(getRDD(sc, jm._1, arg.numMap, rdds).asInstanceOf[RDD[String]], jm._2, jm._3)
      }).toArray: _*)
    if (arg.output != null) {
      rdd.saveAsTextFile(arg.output)
    }
    rdd
  }

  def groupMain(sc: SparkContext, args: Array[String], rdds: Option[Array[RDD[_]]] = None): RDD[String] = {
    val arg = new GroupArguments(args)
    val rdd = groupByKey(getRDD(sc, arg.input, arg.numMap, rdds).asInstanceOf[RDD[String]],
      arg.index, arg.numReduce)
    if (arg.output != null) {
      rdd.saveAsTextFile(arg.output)
    }
    rdd
  }


  private[this] def concat(ss: Array[String], key: Int = -1, sep: String = "\t"): String = {
    val sb = new StringBuilder
    var i = 0
    while (i < ss.length) {

      if (i != 0 && i != key) {
        if (key == 0) {
          if (i > 1) {
            sb.append(sep)
          }
        } else {
          sb.append(sep)
        }
      }
      if (i != key) {
        sb.append(ss(i))
      }
      i += 1
    }
    sb.toString()
  }


  private[this] def getRDD(sc: SparkContext, path: String,
                           numMap: Int, rdds: Option[Array[RDD[_]]] = None): RDD[_] = {
    val RDD_N_REGEX = """rdd\[([0-9]+)\]""".r
    path match {
      case RDD_N_REGEX(index) =>
        rdds.map(_(index.toInt)).getOrElse(sc.textFile(path, numMap))
      case _ =>
        sc.textFile(path, numMap)
    }
  }

  private[this] def getMap(path: String): Map[String, Int] = {
    val map: mutable.Map[String, Int] = mutable.Map()
    Source.fromFile(path, "utf-8").getLines().foreach(line => {
      val ss = line.split("=")
      map(ss(0).trim) = ss(1).trim.toInt
    })
    map.toMap
  }


  private[this] def getFuction(describe: String): String => FeatureGroup = {
    val FUN_N_REGEX = """map_([0-9]+)""".r
    val RDD_N_PATH_REGEX = """map_([0-9]+)_(.+)""".r
    describe match {
      case FUN_N_REGEX(len) =>
        Functions.map(len.toInt)
      case RDD_N_PATH_REGEX(len, path) =>
        Functions.map(len.toInt, getMap(path))
      case _ =>
        throw new UnsupportedOperationException("Function " + describe)
    }
  }

  /**
   * Print usage and exit JVM with the given exit code.
   */
  private[this] def printUsageAndExit(exitCode: Int) {
    System.err.println(
      "Usage: Feature Process [join|filter|map|group|combine|avro]"
    )
    System.exit(exitCode)
  }

  def main(args: Array[String]): Unit = {


    if (args == null || args.length == 0) {
      printUsageAndExit(1)
    }

    lazy val sc = new SparkContext(new SparkConf().setAppName("Feature Process"))

    val rdds: ArrayBuffer[RDD[_]] = new ArrayBuffer[RDD[_]]()

    def parse(args: List[String]): Unit = {
      args match {
        case ("join") :: value :: tail =>
          rdds += joinMain(sc, value.split("\\s+"), Some(rdds.toArray))
          parse(tail)
        case ("group") :: value :: tail =>
          rdds += groupMain(sc, value.split("\\s+"), Some(rdds.toArray))
          parse(tail)
        case ("map") :: value :: tail =>
          rdds += mapMain(sc, value.split("\\s+"), Some(rdds.toArray))
          parse(tail)
        case ("combine") :: value :: tail =>
          rdds += combineMain(sc, value.split("\\s+"), Some(rdds.toArray))
          parse(tail)
        case ("filter") :: value :: tail =>
          rdds += filterMain(sc, value.split("\\s+"), Some(rdds.toArray))
          parse(tail)
        case ("avro") :: value :: tail =>
          rdds += avroMain(sc, value.split("\\s+"), Some(rdds.toArray))
          parse(tail)
        case ("help") :: value :: tail =>
          value match {
            case "join" =>
              JoinArguments.printUsageAndExit(1)
            case "group" =>
              GroupArguments.printUsageAndExit(1)
            case "map" =>
              MapArguments.printUsageAndExit(1)
            case "combine" =>
              CombineArguments.printUsageAndExit(1)
            case "filter" =>
              FilterArguments.printUsageAndExit(1)
            case "avro" =>
              AvroArguments.printUsageAndExit(1)
            case _ =>
              printUsageAndExit(1)
          }
        case ("help") :: tail =>
          printUsageAndExit(0)
        case Nil => {}
        case _ =>
          printUsageAndExit(0)

      }
    }

    parse(args.toList)
    //    rdds.foreach(_.take(10).foreach(println _))

    sc.stop()
  }


}

case class JoinMessage(val rdd: RDD[String], val featureKey: Int, val mainKey: Int)

case class FeatureGroup(val features: Vector, val length: Int) {
  def *(other: FeatureGroup): FeatureGroup = {
    val fs = VectorUtils.newVectorAs(features)
    features.foreachNoZero((i1, v1) => {
      other.features.foreachNoZero((i2, v2) => {
        fs(i1 * length + i2) = v1 * v2
      })
    })
    FeatureGroup(fs, length * other.length)
  }
}

case class DataPoint(val label: Double, val weight: Double, var groups: ArrayBuffer[FeatureGroup])