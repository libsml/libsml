package com.github.libsml.optimization.line


import com.github.libsml.math.linalg.Vector
import com.github.libsml.math.function.Function

/**
 * Created by huangyu on 15/8/26.
 */
trait LineSearch {

  def search(w: Vector, f: Double, g: Vector, s: Vector, stp: Double,
             function: Function, wp: Vector): (Int, Double, Double)
}
