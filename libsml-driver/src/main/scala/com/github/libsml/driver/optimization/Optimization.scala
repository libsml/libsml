package com.github.libsml.driver.optimization


import java.io.PrintStream

import com.github.libsml.commons.util.{Utils, Logging}
import com.github.libsml.math.util.VectorUtils
import com.github.libsml.model.data.{DataFormat, WeightedLabeledVector, DataUtils}
import com.github.libsml.model.evaluation.{BinaryDefaultEvaluator, Evaluator}
import com.github.libsml.model.{ModelUtils, Model}
import com.github.libsml.optimization.{OptimizerUtils, Optimizer}
import com.github.libsml.math.linalg.Vector
import com.github.libsml.math.function.Function
import com.github.libsml.driver.optimization.OptimizationMode._
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

import scala.reflect.ClassTag


/**
 * Created by huangyu on 15/9/7.
 */
object Optimization extends Logging {

  def main(args: Array[String]) {
    if (args.length != 1) {
      System.err.println("Usage:mainClass <confFile>")
      System.exit(1)
    }
    val conf = new OptimizationConf(args(0))
    optimize(conf)
  }

  def optimize(conf: OptimizationConf): Unit = {


    Utils.mkdirs(conf.output, conf.overWrite)

    val sc: Option[SparkContext] = {
      conf.mode match {
        case SPARK =>
          Some(new SparkContext(new SparkConf().setAppName("Optimization_"
            + Utils.className(conf.functionClass) + "_" + Utils.className(conf.optimizerClass))))
        case LOCAL =>
          None
      }
    }

    val logStream = {
      conf.logFile match {
        case Some(path) =>
          new PrintStream(path)
        case None =>
          System.out
      }
    }

    val prior: Vector = VectorUtils.newVector(conf.vectorType, conf.featureNum + (if (conf.bias > 0) 1 else 0))

    conf.prior.foreach(DataUtils.readAvro2Vector(_, prior))

    val trainData = getData(sc, conf.input, conf.mapNum, conf)

    val function: Function[Vector] = {
      trainData match {
        case rdd: RDD[_] =>
          getFunction[RDD[WeightedLabeledVector]](trainData, conf)
        case array: Array[WeightedLabeledVector] =>
          getFunction[Array[WeightedLabeledVector]](trainData, conf)

      }
    }

    val model: Model[Vector, Vector] = ModelUtils.instantiateModel(conf.modelClass, prior, conf.setting)

    val optimizer: Optimizer[Vector] = OptimizerUtils.instantiateOptimizer(conf.optimizerClass,
      prior, conf.setting, function)
    val evaluator: Option[Evaluator[Vector, Vector]] = getEvaluator(sc, logStream, conf)


    val algorithm = new DefaultOptimizationAlgorithm(optimizer, model,
      evaluator, conf.iterationSaveOut, conf.saveFrequency, conf.evaluateFrequency, logStream)


    val lastModel: Model[Vector, Vector] = algorithm.optimize()
    lastModel.save(conf.output + "/" + Utils.className(conf.modelClass))

    sc.foreach(_.stop())
    logStream.close()
  }

  def getData(sc: Option[SparkContext], path: String, numPartitions: Int, conf: OptimizationConf): AnyRef = {


    if (sc.isDefined) {
      conf.dataFormat match {
        case DataFormat.AVRO =>
          //          println(s"mp:${numPartitions}")
          DataUtils.loadAvroData2RDD(sc.get, conf.bias, conf.featureNum, path, numPartitions).coalesce(numPartitions).cache()
        case DataFormat.LIBSVM =>
          DataUtils.loadSVMData2RDD(sc.get, conf.bias, conf.featureNum, path, numPartitions).coalesce(numPartitions).cache()
      }
    } else {
      conf.dataFormat match {
        case DataFormat.AVRO =>
          DataUtils.loadAvroData(conf.bias, conf.featureNum, path)
        case DataFormat.LIBSVM =>
          DataUtils.loadSVMData(conf.bias, conf.featureNum, path)
      }
    }
  }

  def getEvaluator(sc: Option[SparkContext], logStream: PrintStream, conf: OptimizationConf): Option[Evaluator[Vector, Vector]] = {
    conf.testInput match {
      case Some(input) =>
        val data = getData(sc, input, conf.testMapNum, conf)
        val arrayOrRdd = data match {
          case rdd: RDD[WeightedLabeledVector] =>
            Left(rdd)
          case array: Array[WeightedLabeledVector] =>
            Right(array)
        }
        conf.classNum match {
          case 2 => Some(new BinaryDefaultEvaluator(arrayOrRdd, conf.metricsMethods,
            conf.binsNum, Some(logStream)))
          case _ => None
        }
      case None =>
        None
    }
  }


  //(data,map),(data),(map),()
  def getFunction[T](data: AnyRef, conf: OptimizationConf)(implicit ctg: ClassTag[T]): Function[Vector] = {

    var function: Function[Vector] = null
    val cls = Class.forName(conf.functionClass)
    //    println(data.getClass)
    try {
      function = cls.getConstructor(ctg.runtimeClass, classOf[Map[String, String]]).newInstance(data, conf.setting).
        asInstanceOf[Function[Vector]]
    } catch {
      case _: NoSuchMethodException =>
        try {
          function = cls.getConstructor(data.getClass).newInstance(data).
            asInstanceOf[Function[Vector]]
        } catch {
          case _: NoSuchMethodException =>
            try {
              function = cls.getConstructor(classOf[Map[String, String]]).newInstance(conf.setting).
                asInstanceOf[Function[Vector]]
            } catch {
              case _: NoSuchMethodException =>
                function = cls.getConstructor().newInstance().
                  asInstanceOf[Function[Vector]]

            }
        }
    }
    function

  }


}
