package com.github.libsml.model.dirichlet

import com.github.libsml.math.function.Function
import com.github.libsml.math.linalg
import com.github.libsml.math.linalg.Vector
import com.github.libsml.math.linalg.BLAS
import com.github.libsml.math.util.Gamma._
import com.github.libsml.math.util.VectorUtils

/**
 * Created by huangyu on 15/8/25.
 */
class DirichletMultinomial(val data: Array[Vector]) extends Function {

  checkArguments()
  var _1q: Vector = _
  var z: Double = _
  val K = data.length
  val sums: Vector = VectorUtils.newVectorAs(data(0))

  var k = 0
  while (k < K) {
    data(k).foreachNoZero(sums(_) += _)
    k += 1
  }
  val N = sums.noZeroSize

  override def isDerivable: Boolean = true

  override def subGradient(w: linalg.Vector, f: Double, g: linalg.Vector, sg: linalg.Vector): Double = {
    f
  }

  override def gradient(w: linalg.Vector, g: linalg.Vector, setZero: Boolean): Double = {
    if (setZero) {
      BLAS.zero(g)
    }

    //TODO:w > 0

    var f: Double = 0.0

    val sumW = BLAS.sum(w)
    val lgGammaSum = logGamma(sumW)
    val diGammaSum = diGamma(sumW)
    var k = 0
    while (k < K) {
      val lgGamma = logGamma(w(k))
      data(k).foreachNoZero((i, v) => {
        f += logGamma(v + w(k)) - lgGamma
      })
      k += 1
    }
    sums.foreachNoZero((_, v) => {
      f += lgGammaSum - logGamma(v + sumW)
    })

    k = 0
    while (k < K) {
      val _diGamma = diGamma(w(k))
      data(k).foreachNoZero((_, v) => {
        g(k) = _ + diGamma(v + w(k)) - _diGamma
      })
      sums.foreachNoZero((_, v) => {
        g(k) = _ + diGammaSum - diGamma(v + sumW)
      })
      g(k) = -_
      k += 1
    }

    //    println("g:" + g)

    -f
  }

  /**
   * Hessian  * d
   * @param w current value
   * @param d
   * @param hv Hessian  * d
   */
  override def hessianVector(w: linalg.Vector, d: linalg.Vector, hv: linalg.Vector,
                             isUpdateHessian: Boolean, setZero: Boolean): Unit = {

    if (setZero) {
      BLAS.zero(hv)
    }

    //TODO:w > 0

    if (isUpdateHessian) {
      updateQZ(w)
    }

    val b = z * BLAS.sum(d)
    k = 0
    while (k < K) {
      hv(k) = _ + -(1 / _1q(k) * d(k) + b)
      //      println("qk:" + 1 / _1q(k))
      //      println("hv" + k + ":" + hv(k))
      //      println("dk" + k + ":" + d(k))
      //      println("b" + k + ":" + b)

      k += 1
    }

  }

  override def isSecondDerivable: Boolean = true

  override def invertHessianVector(w: linalg.Vector, d: linalg.Vector, hv: linalg.Vector,
                                   isUpdateHessian: Boolean, setZero: Boolean): Unit = {

    if (setZero) {
      BLAS.zero(hv)
    }

    //TODO:w > 0

    if (isUpdateHessian) {
      updateQZ(w)
    }



    val b = BLAS.dot(d, _1q) / (1 / z + BLAS.sum(_1q))

    k = 0
    while (k < K) {
      hv(k) = _ + -((d(k) - b) * _1q(k))
      k += 1
    }

  }

  private[this] def checkArguments(): Unit = {
    require(data != null && data.length >= 2, "Bayesian smooth function exception!")
    data.foreach(_.foreachNoZero((_, d) => require(d >= 0, "Bayesian smooth function exception!")))
  }

  private[this] def updateQZ(w: Vector): Unit = {
    val sumW = BLAS.sum(w)
    val triGammaSum = triGamma(sumW)
    _1q = VectorUtils.newVectorAs(w)

    var k = 0
    while (k < K) {
      val _triGamma = triGamma(w(k))
      data(k).foreachNoZero((_, v) => {
        _1q(k) += triGamma(v + w(k)) - _triGamma
      })
      _1q(k) = 1 / _1q(k)
      k += 1
    }

    var z: Double = 0.0
    sums.foreachNoZero((_, v) => {
      z += triGammaSum - triGamma(v + sumW)
    })
  }

  override def isInBound(w: Vector): Boolean = {
    var flag = true
    var k = 0
    while (k < K) {
      if (w(k) <= 0) flag = false
      k += 1
    }
    flag
  }
}
