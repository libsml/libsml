package com.github.libsml.optimization.linear

import com.github.libsml.math.function.Function

/**
 * Created by huangyu on 15/8/26.
 */
class LinearSearchOwlqn(val param: LinerSearchParameter) extends LinearSearch {

  override def search(function: Function[Double], initStep: Double): (Int, Double, Double) = {
    require(function.isInstanceOf[LinearSearchOwlqn], "Line search:owlqn line search function error.")

    def subGrandient(stp: Double): (Double, Double) = {
      function.subGradient(stp, 0., 0., 0.)
    }

    val (dginit, finit) = subGrandient(0)

    /* Make sure that s points to a descent direction. */
    if (dginit > 0) {
      throw new LinearSearchException("Line search:INCREASEGRADIENT")
    }

    val dgtest = param.ftol * dginit
    var count: Int = 0
    var width: Double = 0
    var dg: Double = 0
    var step = initStep
    var fnew = finit
    val dec: Double = 0.5f
    val inc: Double = 2.1f

    var outOfBound: Boolean = false

    while (true) {

      while (!function.isInBound(step)) {
        outOfBound = true
        step *= dec
      }

      //      val dgf = function.subGradient(step, 0., 0., 0.)
      val dgf = subGrandient(step)
      dg = dgf._1
      fnew = dgf._2
      count += 1

      if (outOfBound) {
        while (fnew > finit + step * dgtest) {
          step *= dec
          //          val dgf = function.subGradient(step, 0., 0., 0.)
          val dgf = subGrandient(step)
          dg = dgf._1
          fnew = dgf._2
          count += 1
        }
        return (count, fnew, step)
      }

      if (fnew > finit + step * dgtest) {
        width = dec
      } else {
        return (count, fnew, step)
      }
      if (step < param.minStep) {
        throw new LinearSearchException("LBFGSERR_MINIMUMSTEP")
      }
      if (step > param.maxStep) {
        throw new LinearSearchException("LBFGSERR_MAXIMUMSTEP")
      }
      if (param.maxLinesearch <= count) {
        throw new LinearSearchException("LBFGSERR_MAXIMUMLINESEARCH")
      }
      step *= width
      //      println("step:"+step)
    }
    (count, fnew, step)

  }
}
