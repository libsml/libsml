package com.github.libsml.feature.engineering.smooth

import java.util

import com.github.libsml.math.function.Function
import com.github.libsml.math.linalg.{BLAS, Vector}
import com.github.libsml.model.dirichlet.DirichletMultinomial
import com.github.libsml.optimization.{Optimizer, OptimizerResult}


/**
 * Created by huangyu on 15/8/23.
 */
class MedianDirichletMultinomial(_function: Function[Vector]) extends Optimizer[Vector] {


  def this(_function: Function[Vector], map: Map[String, String]) {
    this(_function)
  }

  var function: DirichletMultinomial = _function.asInstanceOf[DirichletMultinomial]
  var _weight: Vector = _

  var data: Array[Vector] = _
  var sums: Vector = _

  var K: Int = _
  var N: Int = _

  private[this] var isStop = false

  init()


  private[this] def init() {
    isStop = false
    data = function.data
    sums = function.sums
    N = function.N
    K = function.K
    //        _weight = Vector(Array.fill(K)(1.0))
    _weight = function.prior()

  }

  override def prior(weight: Vector): MedianDirichletMultinomial.this.type = {

    require(weight.size >= 2, "Median smooth exception")
    this._weight = weight
    isStop = false
    this
  }

  override def setFunction(function: Function[Vector]): MedianDirichletMultinomial.this.type = {
    this.function = function.asInstanceOf[DirichletMultinomial]
    init()
    this
  }

  override def weight: Vector = _weight

  override def isConvergence(): Boolean = isStop

  //TODO:compute the function value
  override def f: Double = -1

  override def nextIteration(): OptimizerResult[Vector] = {

    val kSum = data.map(BLAS.sum(_))
    val sumSum = kSum.sum

    val imps = new Array[Double](N)
    sums.foreachNoZero((i, v) => {
      imps(i) = v
    })
    util.Arrays.sort(imps)
    val m = median(imps)

    isStop = true

    var i = 0
    while (i < kSum.length) {
      weight(i) = if (m == 0) 0 else kSum(i) / sumSum * m
      i += 1
    }
    new OptimizerResult(weight)
  }

  private[this] def median(a: Array[Double]): Double = {
    a.length % 2 match {
      case 0 =>
        (a(a.length / 2) + a(a.length / 2 - 1)) / 2
      case 1 =>
        a(a.length / 2)
    }
  }

}
