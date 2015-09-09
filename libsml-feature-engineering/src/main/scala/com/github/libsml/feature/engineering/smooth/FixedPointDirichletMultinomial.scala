package com.github.libsml.feature.engineering.smooth

import com.github.libsml.math.function.Function
import com.github.libsml.math.linalg.{BLAS, Vector}
import com.github.libsml.math.util.Gamma._
import com.github.libsml.model.dirichlet.DirichletMultinomial
import com.github.libsml.optimization.{Optimizer, OptimizerResult}
import com.github.libsml.commons.util.MapWrapper._

/**
 * Created by huangyu on 15/8/23.
 */
class FixedPointDirichletMultinomial(_function: Function[Vector]) extends Optimizer[Vector] {


  def this(_function: Function[Vector], map: Map[String, String]) {
    this(_function)
    this.epsion = map.getDouble("fixpoint.epsion", epsion)
    this.maxIteration = map.getInt("fixpoint.maxIteration", maxIteration)
  }

  var function: DirichletMultinomial = _function.asInstanceOf[DirichletMultinomial]
  var _weight: Vector = _

  var data: Array[Vector] = _
  var sums: Vector = _

  var K: Int = _
  var N: Int = _


  private[this] var epsion: Double = 1E-10
  private[this] var maxIteration: Int = 2000

  private[this] var iter = 0
  private[this] var isStop = false

  init()


  private[this] def init() {
    iter = 0
    isStop = false
    data = function.data
    sums = function.sums
    N = function.N
    K = function.K
    //        _weight = Vector(Array.fill(K)(1.0))
    _weight = function.prior()

  }

  override def prior(weight: Vector): FixedPointDirichletMultinomial.this.type = {

    require(weight.size >= 2, "Beyesian smooth exception")
    this._weight = weight
    iter = 0
    isStop = false
    this
  }

  override def setFunction(function: Function[Vector]): FixedPointDirichletMultinomial.this.type = {
    this.function = function.asInstanceOf[DirichletMultinomial]
    init()
    this
  }

  override def weight: Vector = _weight

  override def isConvergence(): Boolean =
    iter > maxIteration || isStop

  //TODO:compute the function value
  override def f: Double = -1

  override def nextIteration(): OptimizerResult[Vector] = {


    //    println("Iter:" + iter + ",time:" + System.currentTimeMillis() / 1000)

    var dSum: Double = 0
    val wSum = BLAS.sum(weight)
    val diGammaSum = diGamma(wSum)
    sums.foreachNoZero((i, v) => {
      dSum += diGamma(wSum + v) - diGammaSum
    })

    isStop = true
    var isWStop = false
    var k = 0
    while (k < K) {
      var tmp: Double = 0
      val _diGamma = diGamma(weight(k))
      data(k).foreachNoZero((i, v) => {
        tmp += diGamma(weight(k) + v) - _diGamma
      })
      val update = tmp / dSum
      if (Math.abs(update - 1) > epsion) {
        isStop = false
      }
      _weight(k) *= update
      if (weight(k) <= epsion) {
        isWStop = true
      }
      k += 1
    }

    //    println("w:"+weight)
    if (isWStop) {
      isStop = true
    }

    iter += 1
    new OptimizerResult(weight)
  }
}
