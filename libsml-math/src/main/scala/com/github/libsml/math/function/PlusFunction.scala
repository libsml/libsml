package com.github.libsml.math.function

import com.github.libsml.math.linalg
import com.github.libsml.math.linalg.Vector
import com.github.libsml.math.linalg.BLAS

/**
 * Created by huangyu on 15/7/25.
 */
class PlusFunction(private val first: Option[Function[Vector]], private var second: Option[Function[Vector]] = None) extends Function[Vector] {


  require(first.map(_.isDerivable).getOrElse(true) ||
    second.map(_.isDerivable).getOrElse(true),
    "Plus function exception!")

  override def isDerivable: Boolean = {
    first.map(_.isDerivable).getOrElse(true) &&
      second.map(_.isDerivable).getOrElse(true)
  }

  override def subGradient(w: linalg.Vector, f: Double, g: linalg.Vector, sg: linalg.Vector): Double = {
    first.filter(!_.isDerivable).map(_.subGradient(w, f, g, sg)).getOrElse(0.0) +
      second.filter(!_.isDerivable).map(_.subGradient(w, f, g, sg)).getOrElse(0.0)
  }

  override def gradient(w: linalg.Vector, g: linalg.Vector, setZero: Boolean = true): (Vector, Double) = {
    if (setZero) {
      BLAS.zero(g)
    }
    (g, first.map(_.gradient(w, g, false)._2).getOrElse(0.0) + second.map(_.gradient(w, g, false)._2).getOrElse(0.0))
  }

  /**
   * Hessian  * d
   * @param w current value
   * @param d
   * @param hv Hessian  * d
   */
  override def hessianVector(w: linalg.Vector, d: linalg.Vector, hv: linalg.Vector, isUpdateHessian: Boolean, setZero: Boolean = true): Unit = {
    //    require(isSecondDerivable,"Plus function exception!")
    if (setZero) {
      BLAS.zero(hv)
    }
    first.foreach(_.hessianVector(w, d, hv, isUpdateHessian, false))
    second.foreach(_.hessianVector(w, d, hv, isUpdateHessian, false))
  }

  override def isSecondDerivable: Boolean = {
    {
      first.map(_.isSecondDerivable).getOrElse(true) &&
        second.map(_.isSecondDerivable).getOrElse(true)
    }
  }

  def +(function: Function[Vector]): PlusFunction = {

    require(this.isDerivable || function.isDerivable, "Plus function exception!")

    second match {
      case None => {
        second = Some(function)
        this
      }
      case _ => new PlusFunction(Some(this), Some(function))
    }
  }

  override def invertHessianVector(w: linalg.Vector, d: linalg.Vector,
                                   hv: linalg.Vector, isUpdateHessian: Boolean, setZero: Boolean): Unit = {

    if (setZero) {
      BLAS.zero(hv)
    }
    first.foreach(_.invertHessianVector(w, d, hv, isUpdateHessian, false))
    second.foreach(_.invertHessianVector(w, d, hv, isUpdateHessian, false))
  }

  override def isInBound(w: linalg.Vector): Boolean = {
    first.map(_.isInBound(w)).getOrElse(true) && second.map(_.isInBound(w)).getOrElse(true)
  }
}
