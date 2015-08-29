package com.github.libsml.feature.engineering.smooth

import java.io.PrintWriter

import com.github.libsml.commons.util.Logging
import com.github.libsml.model.dirichlet.DirichletMultinomial
import com.github.libsml.optimization.OptimizerUtils
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.rdd.RDD

import com.github.libsml.math.linalg.Vector


/**
 * Created by huangyu on 15/8/23.
 */
object BayesianSmooth extends Logging {

  def smooth(data: RDD[String], outputPath: String,
             optimizerClassName: String = "com.github.libsml.feature.engineering.smooth.FixedPointDirichletMultinomial",
             keyIndex: Int = 0, clickIndex: Int = 1, impressionIndex: Int = 2, reduceNum: Int = 100): Unit = {

    val p = new PrintWriter(outputPath)
    data.map(line => {
      val ss = line.split("\\s+")
      (ss(keyIndex), (ss(clickIndex).toDouble, ss(impressionIndex).toDouble - ss(clickIndex).toDouble))
    }).groupByKey(reduceNum).map(pairs => {
      val key = pairs._1
      val clicks = Vector()
      val unClicks = Vector()
      var index = 0
      pairs._2.foreach(pair => {
        clicks(index) = pair._1
        unClicks(index) = pair._2
        index += 1
      })
      //      val function = new BayesianSmoothFunction(clicks, unClicks)
      val function = new DirichletMultinomial(Array(clicks, unClicks))
      val optimizer = OptimizerUtils.instantiateOptimizer(optimizerClassName, function)
      var iter = 0
      logInfo("start")
      for (r <- optimizer) {
        logInfo("iter:" + iter + "," + "key:" + key + ",alpha:" + r.w(0) + ",beta:" + r.w(1))
        iter += 1
      }
      logInfo("end")
      val alphaBeta = optimizer.optimize()._1
      key + "\t" + alphaBeta(0) + "\t" + alphaBeta(1)
    }).collect().foreach(p.println _)
    p.close()
  }

  def main(args: Array[String]) {
    val argument = new BayesianSmoothArguments(args)
    val conf = new SparkConf().setAppName("Bayesian Smooth")
    val sc = new SparkContext(conf)
    smooth(sc.textFile(argument.input), argument.output,
      argument.optimizerClass, argument.keyIndex,
      argument.clickIndex, argument.impressionIndex, argument.reduceNum)
    sc.stop()
  }

}
