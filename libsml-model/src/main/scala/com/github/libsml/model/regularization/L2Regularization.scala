package com.github.libsml.model.regularization

import com.github.libsml.math.function.Function
import com.github.libsml.math.linalg._

/**
 * Created by huangyu on 15/7/26.
 */
class L2Regularization(val lambda: Double) extends Function {

  require(lambda > 0, "L2Regularization exception:lambda=%f must >0".format(lambda))

  override val isDerivable: Boolean = true

  override def subGradient(w: Vector, f: Double, g: Vector, sg: Vector): Double = f

  //g+w,w dot w
  override def gradient(w: Vector, g: Vector, setZero: Boolean = true): Double = {
    if (setZero) {
      BLAS.zero(g)
    }
    BLAS.axpy(lambda, w, g)
    BLAS.dot(w, w) / 2
  }

  /**
   * Hessian  * d
   * @param w current value
   * @param d
   * @param hv Hessian  * d
   */
  override def hessianVector(w: Vector, d: Vector, hv: Vector, isUpdateHessian: Boolean, setZero: Boolean = true): Unit = {
    if (setZero) {
      BLAS.zero(hv)
    }
    BLAS.axpy(lambda, d, hv)
  }

  override val isSecondDerivable: Boolean = true

  override def invertHessianVector(w: Vector, d: Vector, hv: Vector, isUpdateHessian: Boolean, setZero: Boolean): Unit = {
    if (setZero) {
      BLAS.zero(hv)
    }
    BLAS.axpy(1 / lambda, d, hv)
  }
}
