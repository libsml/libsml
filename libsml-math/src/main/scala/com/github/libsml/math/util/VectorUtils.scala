package com.github.libsml.math.util

import com.github.libsml.math.linalg.VectorType.VectorType
import com.github.libsml.math.linalg._

import scala.io.Source

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

  def newVector(vectorType: VectorType, denseLength: => Int = 0): Vector = {
    vectorType match {
      case VectorType.DENSE =>
        require(denseLength >= 0, "Create dense vector but length=%d is less than 0.".format(denseLength))
        Vector(new Array[Double](denseLength))
      case VectorType.SPARSE =>
        Vector(new Array[Int](0), new Array[Double](0))
      case VectorType.MAP =>
        Vector()
    }
  }

  //index:value
  def load(path: String, vector: Vector): Vector = {
    Source.fromFile(path, "utf-8").getLines().foreach(l => {
      val ss = l.split(":")
      vector(ss(0).trim.toInt) = ss(1).trim.toDouble
    })
    vector
  }

}
