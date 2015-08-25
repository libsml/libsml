package com.github.libsml.feature.engineer.smooth

import com.github.libsml.feature.engineering.smooth.{FixedPointBayesianOptimizer, BayesianSmoothFunction}
import com.github.libsml.math.linalg.Vector
import com.github.libsml.optimization.Optimizer

import scala.io.Source

/**
 * Created by huangyu on 15/8/23.
 */
object BayesianSmoothTest {


  def singleBayesianSmooth() = {
    val n: Int = 1843036
    val clicks: Array[Double] = new Array[Double](n)
    val impressions: Array[Double] = new Array[Double](n)
    var index: Int = 0
    Source.fromFile("data/bayesian", "utf-8").getLines().foreach(line => {
      val ss = line.split("\\s+")
      clicks(index) = ss(1).toDouble
      impressions(index) = ss(2).toDouble
      index += 1
    })
    val fun: BayesianSmoothFunction = new BayesianSmoothFunction(clicks, impressions)
    val optimizer: Optimizer = new FixedPointBayesianOptimizer()
    optimizer.setFunction(fun)

    for (r <- optimizer) {
      println(r.w)
    }
  }

  def main(args: Array[String]) {

    singleBayesianSmooth()
  }
}
