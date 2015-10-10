package com.github.libsml.optimization.rdd

import com.github.libsml.math.linalg.Vector
import org.apache.spark.rdd.RDD

/**
 * Created by yellowhuang on 2015/10/10.
 */
class VectorRDDFunctions(self: RDD[Vector]) {

  def add(numReduce: Int, vector: Vector): Unit = {
    self.toLocalIterator
  }

}
