package com.github.libsml.optimization

import com.github.libsml.math.linalg.{BLAS, Vector}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

/**
 * Created by huangyu on 16/3/5.
 */
class FTRL(val alpha: Double, val beta: Double,
           val lambda1: Double, val lambda2: Double,
           val _featureMinCount: Int, val dataSubSampling: Double,
           val zMin: Double, val nMin: Double, clearRate: Double) {


  private[this] val w: Vector = Vector()
  private[this] val z: Vector = Vector()
  private[this] val n: Vector = Vector()
  private[this] val rdn = new Random(System.currentTimeMillis())
  private[this] var P: Int = 0
  private[this] var N: Int = 0

  private[this] val RANDOM_PRECISION: Int = 1000
  private[this] val featureMinCount = RANDOM_PRECISION / _featureMinCount
  private[this] val dataMinCount = RANDOM_PRECISION * dataSubSampling


  private def isInclude(minCount: Double): Boolean = {
    rdn.nextInt(RANDOM_PRECISION) < minCount
  }


  def update2(x: Vector, _y: Double, dw: Double): this.type = {
    def sgn(d: Double) = if (d > 0) 1 else if (d < 0) -1 else 0

    val y: Int = if (_y == 1) 2 else 0
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

  def clear(): Unit = {
    val ids = new ArrayBuffer[Int]()
    z.foreachNoZero((k, v) => {
      if (Math.abs(v) <= zMin) {
        ids += k
      }
    })

    ids.foreach(k => {
      z(k) = 0
      n(k) = 0
      w(k) = 0
    })

    val ids2 = new ArrayBuffer[Int]()
    n.foreachNoZero((k, v) => {
      if (Math.abs(v) <= nMin) {
        ids2 += k
      }
    })

    ids2.foreach(k => {
      n(k) = 0
    })

  }

  def update(x: Vector, _y: Double, _dw: Double): this.type = {


    if (rdn.nextInt(RANDOM_PRECISION) < RANDOM_PRECISION * clearRate) clear()

    def sgn(d: Double) = if (d > 0) 1 else if (d < 0) -1 else 0

    val y: Int = if (_y == 1) 1 else -1


    if (y == -1 && !isInclude(dataMinCount)) return this

    val dw = if (y == -1) _dw / dataSubSampling else _dw

    val yz: Double = BLAS.dot(x, w) * y
    val p: Double = 1 / (1 + Math.exp(-yz))
    val tmp = y * (p - 1) * dw

    x.foreachNoZero((k, v) => {

      if (z(k) != 0 || y != -1 || isInclude(featureMinCount)) {
        val g = tmp * v
        val thi = (Math.sqrt(n(k) + g * g) - Math.sqrt(n(k))) / alpha
        z(k) = _ + (g - thi * w(k))
        n(k) = _ + g * g
        w(k) = (if (z(k) <= lambda1 && z(k) >= -lambda1) 0 else -(z(k) - sgn(z(k)) * lambda1) / ((beta + Math.sqrt(n(k))) / alpha + lambda2))
      }
    })

    this
  }

  def weight(): Vector = w

  def getZ(): Vector = z

  def getN(): Vector = n


}

object FTRL {
  def main(args: Array[String]): Unit = {
    val rdn = new Random()
    for (i <- 1 until 10000) {
      println(rdn.nextInt(10))
    }
  }
}