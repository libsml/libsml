package com.github.libsml.math.function

import com.github.libsml.math.linalg._

/**
 * Created by huangyu on 15/7/25.
 */
trait Function {

  def isDerivable: Boolean

  def isSecondDerivable: Boolean

  def gradient(w: Vector, g: Vector, setZero: Boolean = true): Double

  def subGradient(w: Vector, f: Double, g: Vector, sg: Vector): Double


  /**
   * Hessian  * d
   * @param w current value
   * @param d
   * @param hv Hessian  * d
   */
  def hessianVector(w: Vector, d: Vector, hv: Vector, isUpdateHessian: Boolean, setZero: Boolean = true): Unit

  def invertHessianVector(w: Vector, d: Vector, hv: Vector, isUpdateHessian: Boolean, setZero: Boolean = true): Unit

}

object Function {
  implicit def toPlusFunction(function: Function) = {
    new PlusFunction(Some(function))
  }
}
