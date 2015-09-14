package com.github.libsml.feature.engineering.smooth

import java.io.PrintWriter

import com.github.libsml.commons.util.Logging
import com.github.libsml.math.linalg.Vector
import com.github.libsml.model.dirichlet.DirichletMultinomial
import com.github.libsml.optimization.OptimizerUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}


/**
 * Created by huangyu on 15/8/23.
 */
object SmoothScore extends Logging {

  def smoothScore(data: RDD[String], outputPath: String,
                  optimizerClassName: String = "com.github.libsml.feature.engineering.smooth.FixedPointDirichletMultinomial",
                  keyIndex: Int = 0, clickIndex: Int = 1, impressionIndex: Int = 2, reduceNum: Int = 100): Unit = {

    data.map(line => {
      val ss = line.split("\\s+")
      (ss(keyIndex), (ss(clickIndex).toDouble, ss(impressionIndex).toDouble - ss(clickIndex).toDouble, line))
    }).groupByKey(reduceNum).flatMap(pairs => {
      val clicks = Vector()
      val unClicks = Vector()
      var index = 0
      pairs._2.foreach(pair => {
        clicks(index) = pair._1
        unClicks(index) = pair._2
        index += 1
      })
      val function = new DirichletMultinomial(Array(clicks, unClicks))
      val optimizer = OptimizerUtils.instantiateOptimizer(optimizerClassName, function.prior(), function)
      val alphaBeta = optimizer.optimize()._1
      pairs._2.filter(pairs => {
        (pairs._1 + alphaBeta(0)) / (pairs._2 + pairs._1 + alphaBeta(1) + alphaBeta(0)) > alphaBeta(0) / (alphaBeta(0) + alphaBeta(1))
      }).map(pairs => {
        pairs._3 + "\t" + (pairs._1 + alphaBeta(0)) / (pairs._2 + pairs._1 + alphaBeta(1) + alphaBeta(0))
      })
    }).saveAsTextFile(outputPath)
  }

  def main(args: Array[String]) {
    val argument = new BayesianSmoothArguments(args)
    val conf = new SparkConf().setAppName("Bayesian Smooth")
    val sc = new SparkContext(conf)
    smoothScore(sc.textFile(argument.input), argument.output,
      argument.optimizerClass, argument.keyIndex,
      argument.clickIndex, argument.impressionIndex, argument.reduceNum)
    sc.stop()
  }

}
