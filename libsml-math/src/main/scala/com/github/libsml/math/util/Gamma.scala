package com.github.libsml.math.util

import breeze.numerics

/**
 * Created by huangyu on 15/8/23.
 */
object Gamma {

  def digamma(_x: Double): Double = {
    numerics.digamma(_x)
    //    var x = _x
    //    var r: Double = 0.0
    //    while (x <= 5) {
    //      r -= 1 / x
    //      x += 1
    //    }
    //    val f: Double = 1.0 / (x * x)
    //    val t: Double = f * (-1.0 / 12.0 + f * (1.0 / 120.0 + f * (-1.0 / 252.0 + f * (1.0 / 240.0 + f * (-1.0 / 132.0 + f * (691.0 / 32760.0 + f * (-1.0 / 12.0 + f * 3617.0 / 8160.0)))))))
    //    return r + Math.log(x) - 0.5 / x + t
  }

  def logGamma(x: Double): Double = {

    numerics.lgamma(x)

    //    val tmp = (x - 0.5) * Math.log(x + 0.5) - (x + 4.5)
    //    val ser = 1.0 + 76.18009173 / (x + 0) - 86.50532033 / (x + 1) + 24.01409822 / (x + 2) - 1.231739516 / (x + 3) +
    //      0.00120858003 / (x + 4) - 0.00000536382 / (x + 5)
    //    tmp + Math.log(ser * Math.sqrt(2 * Math.PI))
  }

  def gamma(x: Double) = Math.exp(logGamma(x))

  def triGamma(x: Double) = numerics.digamma(x)
  
}
