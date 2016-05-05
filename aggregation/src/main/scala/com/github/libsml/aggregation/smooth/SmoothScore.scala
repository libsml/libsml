package com.github.libsml.aggregation.smooth

import com.github.libsml.commons.util.Logging
import com.github.libsml.math.linalg.{BLAS, Vector}
import com.github.libsml.model.dirichlet.DirichletMultinomial
import com.github.libsml.optimization.OptimizerUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}


/**
 * Created by huangyu on 15/8/23.
 */
object SmoothScore extends Logging {

  def smoothScore(data: RDD[String], outputPath: String,
                  optimizerClassName: String = "com.github.libsml.optimization.lbfgs.LBFGS",
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
      if (BLAS.sum(clicks) == 0 || BLAS.sum(unClicks) == 0) {
        Seq.empty[String]
      } else {
        val function = new DirichletMultinomial(Array(clicks, unClicks))
        val optimizer = OptimizerUtils.instantiateOptimizer(optimizerClassName, function.prior(), function)
        val alphaBeta = optimizer.optimize()._1
        pairs._2.filter(ps => {
          (ps._1 + alphaBeta(0)) / (ps._2 + ps._1 + alphaBeta(1) + alphaBeta(0)) > alphaBeta(0) / (alphaBeta(0) + alphaBeta(1))
        }).map(ps => {
          ps._3 + "\t" + (ps._1 + alphaBeta(0)) / (ps._2 + ps._1 + alphaBeta(1) + alphaBeta(0))
        })
      }
    }).saveAsTextFile(outputPath)
  }

  def main(args: Array[String]) {
    val argument = new BayesianSmoothArguments(args)
    val conf = new SparkConf().setAppName("Smooth score")
    val sc = new SparkContext(conf)
    smoothScore(sc.textFile(argument.input), argument.output,
      argument.optimizerClass, argument.keyIndex,
      argument.clickIndex, argument.impressionIndex, argument.reduceNum)
    sc.stop()
  }

}
