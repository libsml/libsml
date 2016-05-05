package com.github.libsml.optimization.linear

import com.github.libsml.math.function.Function
import com.github.libsml.math.linalg.BLAS._
import com.github.libsml.math.linalg.{BLAS, Vector}
import com.github.libsml.math.util.VectorUtils

/**
 * Created by huangyu on 15/8/31.
 */
class LinearSearchFunction(val w: Vector, val f: Double, val g: Vector, val direct: Vector,
                           val function: Function[Vector], temp: Option[Vector]) extends Function[Double] {

  private[this] var oldStep: Double = 0
  val w0: Vector = {
    val returnW = temp.getOrElse(VectorUtils.newVectorAs(w))
    BLAS.copy(w, returnW)
    returnW
  }

  private[this] def updateIfNessesary(step: Double): Unit = {
    if (step != oldStep) {
      oldStep = step
    }
    copy(w0, w)
    axpy(oldStep, direct, w)
  }

  override def isInBound(step: Double): Boolean = {
    updateIfNessesary(step)
    function.isInBound(w)
  }

  override def gradient(step: Double, _g: Double, setZero: Boolean): (Double, Double) = {
    if (step == 0) {
      (dot(g, direct), f)
    } else {
      updateIfNessesary(step)
      val fun = function.gradient(w, g)._2
      (dot(g, direct), fun)
    }
  }

  override def subGradient(w: Double, f: Double, g: Double, sg: Double): (Double, Double) = ???

  override def isDerivable: Boolean = function.isDerivable

  override def isSecondDerivable: Boolean = function.isSecondDerivable
}
