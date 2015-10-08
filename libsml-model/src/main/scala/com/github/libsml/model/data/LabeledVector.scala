package com.github.libsml.model.data

import com.github.libsml.math.linalg.Vector

/**
 * Created by huangyu on 15/7/26.
 *
 * Class that represents the features and labels of a data point.
 *
 * @param label Label for this data point.
 * @param features List of features for this data point.
 *
 */
case class LabeledVector(val label: Double, val features: Vector) {

  override def toString: String = {
    "(%s,%s)".format(label, features)
  }
}
