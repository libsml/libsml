package com.github.libsml.optimization.lbfgs

import com.github.libsml.commons.LibsmlException
import com.github.libsml.math.linalg.{BLAS, Vector}
import com.github.libsml.math.util.VectorUtils

/**
 * Created by huangyu on 15/9/14.
 */
class SingleDirectSearch(val m: Int) extends DirectSearch {

  require(m > 0, s"Single direct search exception:m=${m} should > 0")

  private[this] var k: Int = 1
  private[this] var end: Int = 0
  private[this] var lm: Array[IterationData] = null

  //d should be inited.
  override def direct(d: Vector, x: Vector, xp: Vector, g: Vector, gp: Vector, sg: Option[Vector] = None): Vector = {

    if (lm == null) {
      lm = Array.fill(m)(new IterationData(0.0, VectorUtils.newVectorAs(x), VectorUtils.newVectorAs(x), 0.0))
    }
    sg.foreach(BLAS.ncopy(_, d))

    var it = lm(end)
    BLAS.copy(x, it.s)
    BLAS.axpy(-1, xp, it.s)
    BLAS.copy(g, it.y)
    BLAS.axpy(-1, gp, it.y)

    it.ys = BLAS.dot(it.y, it.s)
    val yy: Double = BLAS.dot(it.y, it.y)

    val bound = if (m <= k) m else k
    k += 1
    end = (end + 1) % m

    var j = end
    var i = 0
    while (i < bound) {
      j = (j + m - 1) % m
      i += 1
      it = lm(j)
      it.alpha = BLAS.dot(it.s, d)
      it.alpha /= it.ys
      BLAS.axpy(-it.alpha, it.y, d)
    }
    BLAS.scal(it.ys / yy, d)

    i = 0
    while (i < bound) {
      it = lm(j)
      var beta = BLAS.dot(it.y, d)
      beta /= it.ys
      BLAS.axpy(it.alpha - beta, it.s, d)
      j = (j + 1) % m
      i += 1
    }
    d
  }

  override def clear(): Unit = {
    k = 1
    end = 0
//    lm = null
  }
}

case class IterationData(var alpha: Double, s: Vector, y: Vector, var ys: Double)

