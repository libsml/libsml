package com.github.libsml.feature.engineering.smooth

import com.github.libsml.math.linalg.Vector
import com.github.libsml.math.function.Function
import com.github.libsml.math.util.Gamma._

/**
 * Created by huangyu on 15/8/23.
 */
class BayesianSmoothFunction(val clicks: Array[Double], val impressions: Array[Double]) extends Function {

  checkArguments()

  override def isDerivable: Boolean = true

  override def subGradient(w: Vector, f: Double, g: Vector, sg: Vector): Double = f

  override def gradient(w: Vector, g: Vector, setZero: Boolean): Double = {
    val alpha = w(0)
    val beta = w(1)
    val alphaBeta = alpha + beta
    var f: Double = 0
    var i = 0

    var tmp = 0
    while (i < clicks.length) {
      tmp += logGamma(alphaBeta) + logGamma(clicks(i) + alpha) + logGamma(impressions(i) - clicks(i) + beta) -
        logGamma(alpha) - logGamma(beta) - logGamma(impressions(i) + alphaBeta)
      f += tmp
      i += 1
    }

    var dAlpha = 0
    var dBeta = 0
    i = 0
    while (i < clicks.length) {
      dAlpha += digamma(alphaBeta) + digamma(clicks(i) + alpha) - digamma(alpha) - digamma(impressions(i) + alphaBeta)
      dBeta += digamma(alphaBeta) + digamma(impressions(i) - clicks(i) + beta) - digamma(beta) - digamma(impressions(i) + alphaBeta)
    }
    g(0) = -dAlpha
    g(1) = -dBeta
    -f
  }

  /**
   * Hessian  * d
   * @param w current value
   * @param d
   * @param hv Hessian  * d
   */
  override def hessianVector(w: Vector, d: Vector, hv: Vector, isUpdateHessian: Boolean, setZero: Boolean): Unit = ???

  override def isSecondDerivable: Boolean = true

  private[this] def checkArguments(): Unit = {
    require(clicks != null && impressions != null && clicks.length == impressions.length, "Bayesian smooth function exception!")
    var i = 0
    while (i < clicks.length) {
      require(clicks(i) >= 0 && impressions(i) >= 0 && clicks(i) <= impressions(i), "Bayesian smooth function exception!")
      i += 1
    }
  }

  override def invertHessianVector(w: Vector, d: Vector, hv: Vector, isUpdateHessian: Boolean, setZero: Boolean): Unit = {

  }
}
