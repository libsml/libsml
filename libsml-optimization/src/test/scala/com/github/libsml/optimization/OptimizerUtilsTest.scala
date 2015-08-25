package com.github.libsml.optimization

import com.github.libsml.math.linalg.Vector

/**
 * Created by huangyu on 15/8/23.
 */
object OptimizerUtilsTest {

  def main(args: Array[String]) {
    OptimizerUtils.instantiateOptimizer("Tron", Vector(),Map[String,String]())
  }
}
