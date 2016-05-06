package com.github.libsml.math.function

import com.github.libsml.math.linalg._


/**
 * Created by huangyu on 15/7/25.
 */
abstract class Function[W] {

  def isInBound(w: W): Boolean

  def isDerivable: Boolean

  def isSecondDerivable: Boolean

  def gradient(w: W, g: W, setZero: Boolean = true): (W, Double)

  def subGradient(w: W, f: Double, g: W, sg: W):(W,Double) = {
    (g,f)
  }


  /**
   * Hessian  * d
   * @param w current value
   * @param d
   * @param hv Hessian  * d
   */
  def hessianVector(w: W, d: W, hv: W, isUpdateHessian: Boolean, setZero: Boolean = true): Unit = {
    throw new UnsupportedOperationException("Function exception!")
  }

  def invertHessianVector(w: W, d: W, hv: W, isUpdateHessian: Boolean, setZero: Boolean = true): Unit = {
    throw new UnsupportedOperationException("Function exception!")
  }

}

object Function {
  implicit def toPlusFunction(function: Function[Vector]) = {
    new PlusFunction(Some(function))
  }
}
