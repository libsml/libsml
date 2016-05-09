package com.github.libsml.model.function

import com.github.libsml.math.function.Function
import com.github.libsml.math.linalg.Vector
import com.github.libsml.model.data.WeightedLabeledVector
import org.apache.spark.rdd.RDD

/**
 * Created by yellowhuang on 2016/5/9.
 */
abstract class FunctionWithRDDData extends Function[Vector] {

  def gradientOneData(data: WeightedLabeledVector, w: Vector, g: Vector): Double

  def subGradientOneData(data: WeightedLabeledVector, w: Vector, f: Double, g: Vector, sg: Vector): Double

  def getData(): RDD[WeightedLabeledVector]

}
