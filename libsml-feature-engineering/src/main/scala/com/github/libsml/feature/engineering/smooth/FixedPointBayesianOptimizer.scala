package com.github.libsml.feature.engineering.smooth

import com.github.libsml.math.util.Gamma._
import com.github.libsml.math.function.Function
import com.github.libsml.math.linalg.Vector
import com.github.libsml.optimization.{OptimizerResult, Optimizer}

/**
 * Created by huangyu on 15/8/23.
 */
class FixedPointBayesianOptimizer extends Optimizer {

  def this(function: Function) = {
    this()
    setFunction(function)
  }

  var _weight: Vector = Vector(Array(1.0, 1.0))
  var function: BayesianSmoothFunction = _
  var clicks: Array[Double] = _
  var impressions: Array[Double] = _

  private[this] var iter = 0
  private[this] var updateAlpha: Double = 0
  private[this] var updateBeta: Double = 0


  private[this] def init() {
    iter = 0
    updateAlpha = 0
    updateBeta = 0
  }

  override def prior(weight: Vector): FixedPointBayesianOptimizer.this.type = {

    require(weight.size >= 2, "Beyesian smooth exception")
    require(weight(0) >= 1E-10, "Beyesian smooth exception")
    require(weight(1) >= 1E-10, "Beyesian smooth exception")

    this._weight = weight
    init()
    this
  }

  override def setFunction(function: Function): FixedPointBayesianOptimizer.this.type = {
    this.function = function.asInstanceOf[BayesianSmoothFunction]
    this.clicks = this.function.clicks
    this.impressions = this.function.impressions
    require(clicks != null && impressions != null && clicks.length == impressions.length, "Bayesian smooth exception.")
    var i: Int = 0
    while (i < clicks.length) {
      require(clicks(i) >= 0 && impressions(i) >= 0 && clicks(i) <= impressions(i),
        "Bayesian smooth exception:click=" + clicks(i) + ",impression=" + impressions(i))
      i += 1
    }
    init()
    this
  }

  override def weight: Vector = _weight

  override def isConvergence(): Boolean =
    iter > 1000 ||
      (Math.abs(updateAlpha - 1) <= 1E-10 && Math.abs(updateBeta - 1) <= 1E-10) ||
      weight(0) <= 1E-10 ||
      weight(1) <= 1E-10

  //TODO:
  override def f: Double = -1

  override def nextIteration(): OptimizerResult = {
    var tmp1: Double = 0
    var tmp2: Double = 0

    var i = 0
    while (i < clicks.length) {
      tmp1 += digamma(weight(0) + clicks(i)) - digamma(weight(0))
      tmp2 += digamma(weight(0) + weight(1) + impressions(i)) - digamma(weight(0) + weight(1))
      i += 1
    }
    updateAlpha = tmp1 / tmp2
    weight(0) *= updateAlpha

    tmp1 = 0
    tmp2 = 0
    i = 0
    while (i < clicks.length) {
      tmp1 += digamma(weight(1) + impressions(i) - clicks(i)) - digamma(weight(1))
      tmp2 += digamma(weight(0) + weight(1) + impressions(i)) - digamma(weight(0) + weight(1))
      i += 1
    }

    updateBeta = tmp1 / tmp2
    weight(1) *= updateBeta

    iter += 1
    new OptimizerResult(weight)
  }
}
