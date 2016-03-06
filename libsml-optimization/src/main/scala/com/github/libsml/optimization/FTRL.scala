package com.github.libsml.optimization

import com.github.libsml.math.linalg.{BLAS, Vector}

/**
 * Created by huangyu on 16/3/5.
 */
class FTRL(val alpha: Double, val beta: Double, val lambda1: Double, val lambda2: Double) {


  private[this] val w: Vector = Vector()

  private[this] val z: Vector = Vector()

  private[this] val n: Vector = Vector()

  private[this] var P: Int = 0
  private[this] var N: Int = 0

  def update2(x: Vector, _y: Double, dw: Double): this.type = {
    def sgn(d: Double) = if (d > 0) 1 else if (d < 0) -1 else 0

    val y: Int = if (_y == 1) 1 else 0
    //    val yy: Int = if (_y == 1) 1 else -1

    val yz: Double = BLAS.dot(x, w)
    val p: Double = 1 / (1 + Math.exp(-yz))
    val tmp = (p - y) * dw

    val oldN = if (P + N == 0) 0 else P * N / (P + N)
    if (_y == 1) P += 1 else N += 1
    val newN = (P * N) / (P + N)


    x.foreachNoZero((k, v) => {
      val g = tmp * v
      //      val n = if (P + N == 0) 0 else P * N / (P + N)
      val thi = (Math.sqrt(newN.toDouble) - Math.sqrt(oldN.toDouble)) / alpha
      z(k) = _ + (g - thi * w(k))
      //      n(k) = _ + g * g

      w(k) = (if (z(k) <= lambda1 && z(k) >= -lambda1) 0 else -(z(k) - sgn(z(k)) * lambda1) / ((beta + Math.sqrt(newN)) / alpha + lambda2))
    })
    this
  }

  def update(x: Vector, _y: Double, dw: Double): this.type = {


    def sgn(d: Double) = if (d > 0) 1 else if (d < 0) -1 else 0

    val y: Int = if (_y == 1) 1 else -1

    val yz: Double = BLAS.dot(x, w) * y
    val p: Double = 1 / (1 + Math.exp(-yz))
    val tmp = y * (p - 1) * dw

    x.foreachNoZero((k, v) => {
      val g = tmp * v
      val thi = (Math.sqrt(n(k) + g * g) - Math.sqrt(n(k))) / alpha
      z(k) = _ + (g - thi * w(k))
      n(k) = _ + g * g

      w(k) = (if (z(k) <= lambda1 && z(k) >= -lambda1) 0 else -(z(k) - sgn(z(k)) * lambda1) / ((beta + Math.sqrt(n(k))) / alpha + lambda2))
    })

    this
  }

  def weight(): Vector = w

  def getZ(): Vector = z

  def getN(): Vector = n


}
