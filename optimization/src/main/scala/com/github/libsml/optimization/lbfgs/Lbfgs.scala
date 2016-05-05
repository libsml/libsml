package com.github.libsml.optimization.lbfgs

import com.github.libsml.commons.util.MapWrapper._
import collection.mutable
import com.github.libsml.math.function.Function
import com.github.libsml.math.linalg.BLAS._
import com.github.libsml.math.linalg.Vector
import com.github.libsml.math.util.VectorUtils
import com.github.libsml.optimization.linear.{LinearSearchFunctionOwlqn, LinearSearch, LinearSearchException, LinearSearchFunction}
import com.github.libsml.optimization.{Optimizer, OptimizerResult}

/**
 * Created by huangyu on 15/9/5.
 */
class LBFGS(var _weight: Vector, map: Map[String, String], var function: Function[Vector])
  extends Optimizer[Vector] {

  def this(_weight: Vector, function: Function[Vector]) = {
    this(_weight, Map[String, String](), function)
  }

  var parameter: LBFGSParameter = initParameter(map)
  private[this] val linearSearch: LinearSearch = LinearSearch(map,function)
  private[this] val directSearch: DirectSearch = DirectSearch(map, parameter.m)

  private[this] var iter = 0
  private[this] var linearSearchIter = 0
  private[this] var isStop = false
  private[this] var fun: Double = 0.0

  private[this] var xnorm: Double = 0.0
  private[this] var gnorm: Double = 0.0
  private[this] var step: Double = 0.0
  private[this] var rate: Double = 0.0
  private[this] var pf: Array[Double] = _
  private[this] var xp: Vector = _
  private[this] var g: Vector = _
  private[this] var sg: Vector = _
  private[this] var gp: Vector = _
  var d: Vector = _

  allocate()


  override def prior(weight: Vector): LBFGS.this.type = {
    this._weight = weight
    iter = 0
    isStop = false
    directSearch.clear()
    this
  }

  override def setFunction(function: Function[Vector]): LBFGS.this.type = {
    this.function = function
    iter = 0
    isStop = false
    directSearch.clear()
    this
  }

  override def weight: Vector = _weight

  override def f: Double = fun

  override def nextIteration(): OptimizerResult[Vector] = {
    var msg = ""
    //    zero(d)
    if (iter == 0) {
      zero(d)
      //      zero(g)
      //      zero(sg)
      //      zero(gp)
      //      zero(xp)

      fun = function.gradient(weight, g)._2
      if (!function.isDerivable) {
        //L1 norm
        fun = function.subGradient(weight, fun, g, sg)._2
        axpy(-1.0, sg, d)
        gnorm = euclideanNorm(sg)
      } else {
        axpy(-1.0, g, d)
        gnorm = euclideanNorm(g)
      }
      xnorm = euclideanNorm(weight)
      if (parameter.past > 0) {
        pf(0) = fun
      }
      if (xnorm < 1.0) {
        xnorm = 1.0
      }


      if (gnorm / xnorm <= parameter.epsilon) {
        isStop = true
        msg = "Initial Convergence"
      }
      step = 1.0 / euclideanNorm(d)

    } else {
      copy(weight, xp)
      copy(g, gp)
      try {
        if (function.isDerivable) {
          //          println(s"fun1:${fun}")/\
          val tmp = linearSearch.search(new LinearSearchFunction(weight, fun, g, d, function, Some(xp)), step)
          fun = tmp._2
          linearSearchIter = tmp._1
          msg += s"\nlinearSearchIter:${linearSearchIter}"
          //          println(s"fun2:${fun}")
        } else {
          val tmp = linearSearch.search(new LinearSearchFunctionOwlqn(weight, fun, g, sg, d, function, Some(xp)), step)
          fun = tmp._2
          linearSearchIter = tmp._1
          msg += s"\nlinearSearchIter:${linearSearchIter}"
        }
      } catch {
        case e: LinearSearchException =>
          msg = e.getMessage
          isStop = true
          return new OptimizerResult[Vector](xp, Some(g), Some(fun), Some(msg))
      }

      /* Compute x and g norms. */
      xnorm = euclideanNorm(weight)
      gnorm = if (function.isDerivable) euclideanNorm(g) else euclideanNorm(sg)

      /*
			 * Convergence test. The criterion is given by the following
			 * formula: |g(x)| / \max(1, |x|) < \epsilon
			 */
      if (xnorm < 1.0) xnorm = 1.0f
      if (gnorm / xnorm <= parameter.epsilon) {
        msg = "Convergence"
        isStop = true
        new OptimizerResult[Vector](weight, Some(sg), Some(fun), Some(msg))
      }
      //      println(s"xnorm:${xnorm},gnorm:${gnorm},eps:${parameter.epsilon},gnorm/xnorm:${gnorm / xnorm}")

      /*
			 * Test for stopping criterion. The criterion is given by the
			 * following formula: (f(past_x) - f(x)) / f(x) < \delta
			 */
      if (parameter.past > 0) {
        if (parameter.past <= iter) {
          rate = (pf(iter % parameter.past) - fun) / fun
          //          println(rate+":"+parameter.delta)
          if (rate < parameter.delta) {
            isStop = true

            msg = "Past stopping criterion"
            return new OptimizerResult[Vector](weight, Some(sg), Some(fun), Some(msg))
          }
        }
        pf(iter % parameter.past) = fun
      }

      directSearch.direct(d, weight, xp, g, gp, Some(sg))


      if (!function.isDerivable) {
        val zeroSet = new mutable.HashSet[Int]()
        d.foreachNoZero((k, v) => if (v * sg(k) >= 0) zeroSet += k)
        zeroSet.foreach(k => d(k) = 0)
        //        var i = parameter.l1Start
        //        while (i < parameter.l1End) {
        //          if (d(i) * sg(i) >= 0) {
        //            d(i) = 0
        //          }
        //          i += 1
        //        }
      }

      step = 1.0

    }
    iter += 1
    new OptimizerResult[Vector](weight, Some(sg), Some(fun), Some(msg))
  }

  override def isConvergence(): Boolean = isStop ||
    (parameter.maxIterations != 0 && parameter.maxIterations <= iter)


  private[this] def initParameter(map: Map[String, String]): LBFGSParameter = {
    val para = new LBFGSParameter()
    para.maxIterations = map.getInt("lbfgs.maxIterations", para.maxIterations)
    para.epsilon = map.getDouble("lbfgs.epsilon", para.epsilon)
    para.delta = map.getDouble("lbfgs.delta", para.delta)
    para.m = map.getInt("lbfgs.m", para.m)
    para.past = map.getInt("lbfgs.past", para.past)
    //    para.l1Start = map.getInt("l1.start", para.l1Start)
    //    para.l1End = map.getInt("l1.end", para.l1End)
    para
  }

  private[this] def checkParameter(): Unit = {
    /* Check the input parameters for errors. */
    val param = parameter
    if (param.epsilon < 0) {
      throw new IllegalStateException("LBFGSERR_INVALID_EPSILON")
    }
    if (param.past < 0) {
      throw new IllegalStateException("LBFGSERR_INVALID_TESTPERIOD")
    }
    if (param.delta < 0) {
      throw new IllegalStateException("LBFGSERR_INVALID_DELTA")
    }
  }

  private[this] def allocate(): Unit = {
    /*
       Allocate an array for storing previous values of the objective
		 * function.
		 */
    if (0 < parameter.past) {
      pf = new Array[Double](parameter.past)
    }
    g = VectorUtils.newVectorAs(weight)
    sg = if (function.isDerivable) g else VectorUtils.newVectorAs(weight)
    d = VectorUtils.newVectorAs(weight)
    gp = VectorUtils.newVectorAs(weight)
    xp = VectorUtils.newVectorAs(weight)


  }


}