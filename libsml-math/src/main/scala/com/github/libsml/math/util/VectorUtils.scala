package com.github.libsml.math.util

import com.github.libsml.math.linalg._

/**
 * Created by huangyu on 15/8/18.
 */
object VectorUtils {

  def newVectorAs(vector: Vector): Vector = {
    vector match {
      case mv: MapVector =>
        Vector()
      case dv: DenseVector =>
        Vector(new Array[Double](dv.values.length))
      case sv: SparseVector =>
        Vector(new Array[Int](sv.indices.length), new Array[Double](sv.values.length))
      case _ =>
        throw new IllegalArgumentException("Unknow vector:" + vector.getClass + "!")
    }
  }


}
