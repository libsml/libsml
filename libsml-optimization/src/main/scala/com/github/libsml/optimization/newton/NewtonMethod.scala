package com.github.libsml.optimization.newton

import com.github.libsml.math.function.Function
import com.github.libsml.math.linalg.{BLAS, Vector}
import com.github.libsml.math.util.VectorUtils
import com.github.libsml.optimization.line.{LineSearchParameter, LinearSearchWolf, LineSearch}
import com.github.libsml.optimization.{OptimizerResult, Optimizer}

/**
 * Created by huangyu on 15/8/25.
 */
class NewtonMethod extends Optimizer {

  var function: Function = _
  var _weight: Vector = Vector()
  var iter: Int = 0
  var isStop: Boolean = false
  var gnorm1: Double = 0
  var _f: Double = Double.MaxValue
  var wp: Vector = _

  var lineSearch: LineSearch = new LinearSearchWolf(new LineSearchParameter())

  var g: Vector = null
  var d: Vector = null
  var hv: Vector = null

  var para: Parameter = new Parameter()

  def this(function: Function, _weight: Vector) {
    this()
    this.function = function
    this._weight = _weight
    init(_weight)
  }

  def this(function: Function, _weight: Vector, para: Parameter) {
    this(function, _weight)
    this.para = para
  }


  override def prior(_weight: Vector): NewtonMethod.this.type = {
    this._weight = _weight
    init(_weight)
    this
  }

  override def setFunction(function: Function): NewtonMethod.this.type = {
    this.function = function
    this.iter = 0
    this._f = Double.MaxValue
    this.isStop = false
    this
  }

  override def weight: Vector = {
    _weight
  }

  override def isConvergence(): Boolean = (para.maxIterations != 0 && iter > para.maxIterations) || isStop

  override def f: Double = _f

  override def nextIteration(): OptimizerResult = {

    if (iter == 0) {
      _f = function.gradient(weight, g, true)
    }
    function.invertHessianVector(weight, g, hv, true, true)
    BLAS.scal(-1, hv)
    //    println("hv:" + hv)
    //    println(weight)
    BLAS.copy(weight, wp)
    _f = lineSearch.search(weight, _f, g, hv, 1.0, function, wp)._2
    //    println(weight)

    //    BLAS.axpy(-1, hv, _weight)

    var msg: String = ""

    if (_f < -1.0e+32) {
      msg += "WARNING: f < -1.0e+32\n"
      isStop = true
    }

    if (iter == 0) {
      gnorm1 = BLAS.euclideanNorm(g)
    } else {
      val gnorm = BLAS.euclideanNorm(g)

      if (gnorm <= para.epsilon * gnorm1) {
        isStop = true
      }
    }

    iter += 1

    new OptimizerResult(weight, Some(g), Some(f), Some(msg))
  }


  private[this] def init(weight: Vector): Unit = {
    iter = 0
    _f = Double.MaxValue
    isStop = false
    if (g == null) {
      g = VectorUtils.newVectorAs(weight)
      d = VectorUtils.newVectorAs(weight)
      hv = VectorUtils.newVectorAs(weight)
      wp = VectorUtils.newVectorAs(weight)
    }
  }
}
