package com.github.libsml.model.data

import com.github.libsml.math.linalg.Vector
import org.apache.avro.Schema

/**
 * Created by huangyu on 15/7/26.
 *
 * Class that represents the features and labels of a data point with a weight.
 *
 * @param label Label for this data point.
 * @param features List of features for this data point.
 * @param weight Weight of the data point.
 *
 */
case class WeightedLabeledVector(val label: Double, val features: Vector, val weight: Double = 1.0) {

  override def toString: String = {
    "(%s,%s,%s)".format(label, features, weight)
  }
}
