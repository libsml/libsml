package com.github.libsml.feature.engineering.smooth

import com.github.libsml.math.linalg.{BLAS, Vector}
import com.github.libsml.math.function.Function
import com.github.libsml.math.util.Gamma._
import com.github.libsml.math.util.VectorUtils

/**
 * Created by huangyu on 15/8/23.
 */
class BayesianSmoothFunction(val clicks: Vector, val unClicks: Vector) extends Function[Vector] {

  val impressions: Vector = VectorUtils.newVectorAs(clicks)
  BLAS.copy(unClicks, impressions)
  BLAS.axpy(1, clicks, impressions)
  checkArguments()

  //  private[this] var diGammaClick: Array[Double] = _
  //  private[this] var diGammaImpression: Array[Double] = _

  override def isDerivable: Boolean = true

  override def subGradient(w: Vector, f: Double, g: Vector, sg: Vector): Double = f

  override def gradient(w: Vector, g: Vector, setZero: Boolean): (Vector,Double) = {
    val alpha = w(0)
    val beta = w(1)
    val alphaBeta = alpha + beta
    var f: Double = 0
    var i = 0
    val logGammaAlphaBeta = logGamma(alphaBeta)
    val logGammaAlpha = logGamma(alpha)
    val logGammaBeta = logGamma(beta)

    clicks.foreachNoZero((i, v) => {
      f += logGamma(v + alpha) - logGammaAlpha
    })

    unClicks.foreachNoZero((i, v) => {
      f += logGamma(v + beta) - logGammaBeta
    })

    impressions.foreachNoZero((i, v) => {
      f += logGamma(v + alphaBeta) - logGammaAlphaBeta
    })

    var dAlpha: Double = 0
    var dBeta: Double = 0
    val diGammaAlphaBeta = diGamma(alphaBeta)
    val diGammaAlpha = diGamma(alpha)
    val diGammaBeta = diGamma(beta)

    clicks.foreachNoZero((i, v) => {
      dAlpha += diGamma(v + alpha) - diGammaAlpha
    })
    unClicks.foreachNoZero((i, v) => {
      dBeta += diGamma(v + beta) - diGammaBeta
    })
    impressions.foreachNoZero((i, v) => {
      val tmp = diGamma(v + alphaBeta) - diGammaAlphaBeta
      dAlpha -= tmp
      dBeta -= tmp
    })

    g(0) = -dAlpha
    g(1) = -dBeta
    (g,-f)
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

    require(clicks != null && unClicks != null, "Bayesian smooth function exception!")
    clicks.foreachNoZero((i, v) => {
      require(v >= 0, "Bayesian smooth function exception!")
    })

    unClicks.foreachNoZero((i, v) => {
      require(v >= 0, "Bayesian smooth function exception!")
    })
  }

  override def invertHessianVector(w: Vector, d: Vector, hv: Vector, isUpdateHessian: Boolean, setZero: Boolean): Unit = {
    if (setZero) {
      BLAS.zero(hv)
    }

    var b: Double = 0
    var qAlpha: Double = 0.0
    var qBeta: Double = 0.0
    var z: Double = 0
    val alphaBeta = w(0) + w(1)

    var i = 0

    var tmp = triGamma(w(0))
    clicks.foreachNoZero((i, v) => {
      qAlpha += triGamma(v + w(0)) - tmp
    })


    tmp = triGamma(w(1))
    unClicks.foreachNoZero((i, v) => {
      qBeta += triGamma(v + w(1)) - tmp
    })

    tmp = triGamma(alphaBeta)
    impressions.foreachNoZero((i, v) => {
      z += tmp - triGamma(v + alphaBeta)
    })

    b = (d(0) / qAlpha + d(1) / qBeta) / (1 / z + 1 / qAlpha + 1 / qBeta)

    d.foreachNoZero((k, v) => {
      hv(k) = _ + (d(k) - b) / (if (k == 0) qAlpha else qBeta)
    })

    hv(0) = -hv(0)
    hv(1) = -hv(1)
  }

  override def isInBound(w: Vector): Boolean = {
    w(0) > 0 && w(1) > 0
  }
}
