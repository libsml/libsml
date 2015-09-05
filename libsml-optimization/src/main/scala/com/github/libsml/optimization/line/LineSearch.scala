package com.github.libsml.optimization.line


import com.github.libsml.math.linalg.Vector
import com.github.libsml.math.function.Function

/**
 * Created by huangyu on 15/8/26.
 */
trait LineSearch {

  def search(function: Function[Double],initStep:Double): (Int, Double, Double)
}
