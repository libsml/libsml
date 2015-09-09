package com.github.libsml.driver.optimization

import java.io.FileInputStream
import java.util.Properties

import OptimizationParameter._
import OptimizationMode._
import com.github.libsml.commons.util.MapWrapper._

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable
import scala.collection.JavaConverters._

/**
 * Created by huangyu on 15/9/5.
 */
class OptimizationParameter(args: Array[String]) {

  //check:!=null;argument:--input,-i
  var input: String = null
  //check:!=null;argument:--output,-o
  var output: String = null
  //argument:--testInput,-ti
  var testInput: String = null
  //argument:--testOut,-to
  var testOutput: String = null
  //check:!=null;argument:--functionClass,-f
  var functionClass: String = null
  //argument:--optimizerClass,-op;default:tron
  var optimizerClass: String = "tron"
  //check:>=0;argument:--l1,-l1
  var l1Regularizer: Double = 0.0
  //check:>=0;argument:--l2,l2
  var l2Regularizer: Double = 0.0
  //check:>=0,0 means regression;argument:--classNumber,-c
  var classNum: Int = 2
  //check:>0;argument:--featureNumber,-e
  var featureNum: Int = -1
  //check:>0;argument:--mapNumber,-m
  var mapNum: Int = 100
  //check:>0;argument:--reduceNumber,-r
  var reduceNum: Int = 100
  //check:>0;argument:--testMapNumber,-tm
  var testMapNum: Int = Math.max(1, (mapNum * 0.2).toInt)
  //check:>0;argument:--testReduceNumber,-tr
  var testReduceNum: Int = Math.max(1, (reduceNum * 0.2).toInt)
  //argument:--mode
  var mode: Mode = SPARK
  //check:!=null;argument:--modelClass,-mc
  var modelClass: String = null
  //argument:--metricsMethod,-mm
  var metricsMethods: ArrayBuffer[String] = new ArrayBuffer[String]()

  //argument:--saveFrequency,-sf
  var saveFrequency: Int = 0

  //argument:--confFile,-cf
  var confPath: String = null
  //argument:(--conf key=value)*
  var confMap: mutable.Map[String, String] = new mutable.HashMap[String, String]()


  parse(args.toList)
  loadConfFile()
  parseConfMap(confMap.toMap)


  private[this] def loadConfFile(): Unit = {
    if (confPath != null) {
      val prop = new Properties()
      prop.load(new FileInputStream(confPath))
      for (key <- prop.stringPropertyNames().asScala) {
        confMap.getOrElseUpdate(key, prop.getProperty(key))
      }
    }
  }

  private[this] def parseConfMap(map: Map[String, String]) = {

    if (input == null) {
      input = map("input")
    } else {
      confMap("input") = input
    }

    if (output == null) {
      output = map("output")
    } else {
      confMap("output") = output
    }

    if (testInput == null) {
      testInput = map("testInput")
    } else {
      confMap("testInput") = testInput
    }

    if (testOutput == null) {
      testOutput = map("testOutput")
    } else {
      confMap("testOutput") = testOutput
    }

    if (functionClass == null) {
      functionClass = map("functionClass")
    } else {
      confMap("functionClass") = functionClass
    }


    if (optimizerClass == null) {
      optimizerClass = map("optimizerClass")
    } else {
      confMap("optimizerClass") = optimizerClass
    }

    if (l1Regularizer == 0) {
      l1Regularizer = map.getDouble("l1Regularizer", 0.0)
    } else {
      confMap("l1Regularizer") = l1Regularizer.toString
    }

    if (output == null) {
      output = map("output")
    } else {
      confMap("output") = output
    }

    if (output == null) {
      output = map("output")
    } else {
      confMap("output") = output
    }

    if (output == null) {
      output = map("output")
    } else {
      confMap("output") = output
    }

    if (output == null) {
      output = map("output")
    } else {
      confMap("output") = output
    }


  }


  private[this] def parse(args: List[String]): Unit = args match {
    case ("--input" | "-i") :: value :: tail =>
      input = value
      parse(tail)

    case ("--output" | "-o") :: value :: tail =>
      output = value
      parse(tail)

    case ("--testInput" | "-ti") :: value :: tail =>
      testInput = value
      parse(tail)

    case ("--testInput" | "-to") :: value :: tail =>
      testOutput = value
      parse(tail)

    case ("--functionClass" | "-f") :: value :: tail =>
      functionClass = value
      parse(tail)

    case ("--optimizerClass" | "-op") :: value :: tail =>
      functionClass = value
      parse(tail)

    case ("--l1" | "-l1") :: value :: tail =>
      l1Regularizer = value.toDouble
      parse(tail)

    case ("--l2" | "-l2") :: value :: tail =>
      l2Regularizer = value.toDouble
      parse(tail)

    case ("--classNumber" | "-c") :: value :: tail =>
      classNum = value.toInt
      parse(tail)

    case ("--featureNumber" | "-e") :: value :: tail =>
      featureNum = value.toInt
      parse(tail)

    case ("--mapNumber" | "-m") :: value :: tail =>
      mapNum = value.toInt
      parse(tail)

    case ("--reduceNumber" | "-r") :: value :: tail =>
      reduceNum = value.toInt
      parse(tail)

    case ("--testMapNumber" | "-tm") :: value :: tail =>
      mapNum = value.toInt
      parse(tail)

    case ("--testReduceNumber" | "-tr") :: value :: tail =>
      reduceNum = value.toInt
      parse(tail)

    case ("--mode") :: value :: tail =>
      mode = {
        value.trim match {
          case "spark" =>
            SPARK
          case "local" =>
            LOCAL
          case _ =>
            throw new UnsupportedOperationException("Don't support mode:" + value)
        }
      }
      parse(tail)

    case ("--modelClass" | "-mc") :: value :: tail =>
      modelClass = value
      parse(tail)

    case ("--metricsMethod" | "-mm") :: value :: tail =>
      metricsMethods += value
      parse(tail)

    case ("--saveFrequency" | "-sf") :: value :: tail =>
      saveFrequency += value.toInt
      parse(tail)

    case ("--confFile" | "-cf") :: value :: tail =>
      confPath += value
      parse(tail)

    case ("--conf") :: value :: tail =>
      val ss = value.split("=")
      confMap(ss(0)) = ss(1)
      parse(tail)
    case Nil => {}

    case _ =>
      printUsageAndExit(1)
  }


}

object OptimizationParameter {

  /**
   * Print usage and exit JVM with the given exit code.
   */
  private def printUsageAndExit(exitCode: Int) {
    System.err.println(
      "Usage: Master [options]\n" +
        "\n" +
        "Options:\n" +
        "  --confFile CONFIGURATION FILE  configuration file\n")
    // scalastyle:on println
    System.exit(exitCode)
  }
}
