package com.github.libsml.aggregation.optimization

import java.io.{FileInputStream, InputStream}
import java.util.Properties

import com.github.libsml.commons.LibsmlException
import com.github.libsml.commons.util.MapWrapper._
import com.github.libsml.commons.util.{Logging, Utils}
import com.github.libsml.aggregation.optimization.OptimizationMode._
import com.github.libsml.math.linalg.VectorType
import com.github.libsml.model.data.DataFormat

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Created by huangyu on 15/9/6.
 */
class OptimizationConf() extends Logging {


  private[this] var shortFullMap: Map[String, String] = loadConfFile(
    this.getClass.getClassLoader.getResourceAsStream("short-full-default.properties"))

  def this(confPath: String) = {
    this()
    setting = loadConfFile(confPath)
    shortFullConf.foreach(f => shortFullMap = loadConfFile(f))
  }

  var setting: Map[String, String] = Map[String, String]()


  def input = setting("input")


  def output = setting("output")

  def shortFullConf = setting.get("shortFullConf")

  def testInput: Option[String] = setting.get("testInput")

  //  def testOutput: Option[String] = setting.get("testOutput")

  def logFile: Option[String] = setting.get("logFile")

  def functionClass = fullClassName(setting("functionClass"))

  def optimizerClass = fullClassName(setting.getOrElse("optimizerClass", "tron"))

  def l1Regularizer: Int = setting.getInt("l1Regularizer", 0)

  def binsNum: Int = setting.getInt("binsNumber", 0)

  def bias: Double = setting.getDouble("bias", 0)

  def l2Regularizer: Int = setting.getInt("l2Regularizer", 0)

  def classNum: Int = setting.getInt("classNumber", 2)

  def featureNum: Int = setting.getInt("featureNumber", -1)

  def mapNum: Int = setting.getInt("mapNumber", 100)

  def reduceNum: Int = setting.getInt("reduceNumber", 100)

  def testMapNum: Int = setting.getInt("testMapNum", Math.max(1, (mapNum * 0.2).toInt))

  def testReduceNum: Int = setting.getInt("testReduceNum", Math.max(1, (reduceNum * 0.2).toInt))

  def modelClass = fullClassName(setting("modelClass"))

  def prior = setting.get("prior")

  def dataFormat = {
    val dataFormat = setting.getOrElse("dataFormat", "avro").trim.toLowerCase
    dataFormat match {
      case "avro" =>
        DataFormat.AVRO
      case "libsvm" =>
        DataFormat.LIBSVM
      case _ =>
        throw new LibsmlException(s"Unsupported data format:${dataFormat}, the data format is:avro,libsvm.")
    }
  }


  //values:dense,sparse,map;default:map
  def vectorType = {
    val vt = setting.getOrElse("vectorType", "map").trim.toLowerCase()
    vt match {
      case "dense" =>
        VectorType.DENSE
      case "sparse" =>
        VectorType.SPARSE
      case "map" =>
        VectorType.MAP
      case _ =>
        throw new LibsmlException(s"Unsupported vector type:${vt}, the vector type is:dense,spark,map;defualt:map.")
    }
  }

  def metricsMethods: Array[String] = {
    val arrayBuffer: ArrayBuffer[String] = new ArrayBuffer[String]()

    setting.get("metricsMethods").foreach(ms => {
      ms.split(",").foreach(arrayBuffer += _)
    })

    arrayBuffer.toArray
  }

  def saveFrequency: Int = setting.getInt("saveFrequency", 1)

  def overWrite: Boolean = setting.getBoolean("overWrite", true)

  def evaluateFrequency: Int = setting.getInt("evaluateFrequency", 1)

  def iterationSaveOut: Option[String] = {
    if (saveFrequency > 0) {
      Utils.mkdirs(output + "/model", true)
      Some(output + "/model")
    } else {
      None
    }
  }

  def mode: Mode = {
    setting.getOrElse("mode", "spark").trim match {
      case "spark" =>
        SPARK
      case "local" =>
        LOCAL
      case _ =>
        throw new LibsmlException("Don't support mode:" + setting.getOrElse("mode", "spark"))
    }
  }


  private[this] def loadConfFile(confPath: String): Map[String, String] = {
    if (confPath == null)
      Map.empty[String, String]
    else {
      try {
        loadConfFile(new FileInputStream(confPath))
      } catch {
        case e: Throwable =>
          logError("Load configuration exception!", e)
          Map.empty[String, String]

      }
    }
  }

  private[this] def loadConfFile(confInputStream: InputStream): Map[String, String] = {
    val setting: mutable.Map[String, String] = new mutable.HashMap[String, String]()
    val prop = new Properties()
    prop.load(confInputStream)
    for (key <- prop.stringPropertyNames().asScala) {
      setting.getOrElseUpdate(key, prop.getProperty(key))
    }
    setting.toMap
  }

  private[this] def fullClassName(className: String): String = {
    shortFullMap.getOrElse(className, className)
  }


}
