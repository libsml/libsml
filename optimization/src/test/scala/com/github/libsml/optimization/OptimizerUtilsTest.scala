package com.github.libsml.optimization

import com.github.libsml.math.linalg.Vector
import com.github.libsml.math.function.Function

/**
 * Created by huangyu on 15/8/23.
 */
object OptimizerUtilsTest {

  def main(args: Array[String]) {
    OptimizerUtils.instantiateOptimizer("com.github.libsml.feature.engineering.smooth.FixedPointDirichletMultinomial", null.asInstanceOf[Function[Vector]])
  }
}
