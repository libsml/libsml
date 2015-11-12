package com.github.libsml.feature.engineering.smooth


import com.github.libsml.commons.LibsmlException
import com.github.libsml.commons.util.Logging
import com.github.libsml.math.linalg.Vector
import com.github.libsml.model.dirichlet.DirichletMultinomial
import com.github.libsml.optimization.{Optimizer, OptimizerUtils}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}


/**
 * Created by huangyu on 15/8/23.
 */
object BayesianSmooth extends Logging {

  def smooth(data: RDD[String], outputPath: String,
             optimizerClassName: String = "com.github.libsml.optimization.lbfgs.LBFGS",
             keyIndex: Int = 0, clickIndex: Int = 1, impressionIndex: Int = 2,
             reduceNum: Int = 100, map: Map[String, String] = Map()): Unit = {

    data.map(line => {
      val ss = line.split("\t")
      try {
        (ss(keyIndex), (ss(clickIndex).toDouble, ss(impressionIndex).toDouble - ss(clickIndex).toDouble))
      } catch {
        case e: Throwable =>
          logError(s"Smooth exception:${line}")
          throw new LibsmlException(line, e)
      }
    }).groupByKey(reduceNum).mapPartitions(its => {
      var optimizer: Optimizer[Vector] = null
      its.map(
        pairs => {
          val key = pairs._1
          val clicks = Vector()
          val unClicks = Vector()
          var index = 0
          pairs._2.foreach(pair => {
            clicks(index) = pair._1
            unClicks(index) = pair._2
            index += 1
          })

          val function = new DirichletMultinomial(Array(clicks, unClicks))
          //          val optimizer = OptimizerUtils.instantiateOptimizer(optimizerClassName, function.prior(), function)
          if (optimizer == null) {
            optimizer = OptimizerUtils.instantiateOptimizer(optimizerClassName, function.prior(), map, function)
          } else {
            optimizer.prior(function.prior())
            optimizer.setFunction(function)
          }
          try {
            //            val alphaBeta = optimizer.optimize()._1
            var isStop = false
            var alphaBeta: Vector = Vector(Array(0.0, 0.0))
            for (t <- optimizer if !isStop) {
              alphaBeta = t.w
              isStop = t.f.map(_ < 10E-10).getOrElse(isStop)
              isStop = if (alphaBeta(0) < 10E-10) true else isStop
              isStop = if (alphaBeta(1) < 10E-10) true else isStop
              if (isStop) {
                logWarning(s"Function value or weight is too small,function value:${t.f},weight:${alphaBeta},key:${key}")
              }
            }
            key + "\t" + alphaBeta(0) + "\t" + alphaBeta(1)
          } catch {
            case e: Throwable =>
              reappear(key, optimizer, function)
              throw new LibsmlException("Bayesian smooth exception.")
          }

        }).filter(_.trim != "")
    }).saveAsTextFile(outputPath)
  }

  def main(args: Array[String]) {
    val argument = new BayesianSmoothArguments(args)
    val conf = new SparkConf().setAppName("Bayesian Smooth")
    val sc = new SparkContext(conf)
    smooth(sc.textFile(argument.input), argument.output,
      argument.optimizerClass, argument.keyIndex,
      argument.clickIndex, argument.impressionIndex, argument.reduceNum, argument.confMap.toMap)
    sc.stop()
  }

  def reappear(key: String, optimizer: Optimizer[Vector], function: DirichletMultinomial): Unit = {
    try {
      logError(s"key:${key}")
      logError(s"weight:${optimizer.weight}")

      optimizer.prior(function.prior())
      optimizer.setFunction(function)

      for (it <- optimizer) {
        logError(s"${it}")
      }
    } catch {
      case e: Throwable =>
        logError(e.getMessage)
    }
  }


}
