package com.github.libsml.optimization.linear

import com.github.libsml.math.function.Function
import com.github.libsml.math.linalg.BLAS._
import com.github.libsml.math.linalg.{BLAS, Vector}
import com.github.libsml.math.util.VectorUtils
import collection.mutable

/**
 * Created by huangyu on 15/8/31.
 */
class LinearSearchFunctionOwlqn(val w: Vector, val f: Double, val g: Vector, val sg: Vector, val direct: Vector,
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
    owlqnProject(w, w0, sg)
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

  override def subGradient(step: Double, _f: Double, _g: Double, _sg: Double): (Double, Double) = {
    if (step == 0) {
      (dot(sg, direct), f)
    } else {
      updateIfNessesary(step)
      val fun_ = function.gradient(w, g)._2
      val sf = function.subGradient(w, fun_, g, sg)._2
      (dot(sg, direct), sf)
    }
  }

  override def isDerivable: Boolean = function.isDerivable

  override def isSecondDerivable: Boolean = function.isSecondDerivable

  def owlqnProject(d: Vector, x: Vector, sg: Vector): Unit = {

    val zeroSet = new mutable.HashSet[Int]()
    d.foreachNoZero((k, v) => {
      val sign = if (x(k) == 0.) -sg(k) else x(k)
      if (v * sign <= 0) zeroSet += k
    })
    zeroSet.foreach(k => d(k) = 0)
    zeroSet.clear()
  }

}
