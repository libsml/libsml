package com.github.libsml.model.regularization

import com.github.libsml.math.function.Function
import com.github.libsml.math.linalg._

/**
 * Created by yellowhuang on 2016/5/3.
 */
class L1Regularization(val lambda: Double) extends Function[Vector] {

  require(lambda > 0, "L2Regularization exception:lambda=%f must >0".format(lambda))
  //  require(start >= 0, "L2Regularization exception:start=%d must >=0".format(start))
  //  require(end >= 0 || end == -1, "L2Regularization exception:end=%d must >=0 or ==-1".format(end))

  override val isDerivable: Boolean = false

  override def isSecondDerivable: Boolean = false

  override def isInBound(w: Vector): Boolean = true

  override def gradient(w: Vector, g: Vector, setZero: Boolean): (Vector, Double) = {
    if (setZero) {
      BLAS.zero(g)
    }
    (g, 0)

  }

  override def subGradient(w: Vector, f: Double, g: Vector, sg: Vector): (Vector, Double) = {
    BLAS.zero(sg)
    w.foreachNoZero((k, v) => sg(k) = if (v < 0) g(k) - lambda else g(k) + lambda)
    g.foreachNoZero((k, v) => {
      if (w(k) == 0) {
        if (v < -lambda) sg(k) = v + lambda
        else if (v > lambda) sg(k) = v - lambda

      }
    }
    )
    (sg, f + BLAS.l1Norm(w))

  }

  /**
   * Hessian  * d
   * @param w current value
   * @param d
   * @param hv Hessian  * d
   */
  override def hessianVector(w: Vector, d: Vector, hv: Vector, isUpdateHessian: Boolean, setZero: Boolean): Unit = ???

  override def invertHessianVector(w: Vector, d: Vector, hv: Vector, isUpdateHessian: Boolean, setZero: Boolean): Unit = ???
}
