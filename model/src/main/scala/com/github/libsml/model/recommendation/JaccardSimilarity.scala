package com.github.libsml.model.recommendation

import com.github.libsml.math.linalg.Vector

/**
 * Created by yellowhuang on 2016/5/6.
 */
class JaccardSimilarity extends Similarity {

  override def norm(vector: Vector): Double = vector.noZeroSize

  override def similarity(score: Double, norm1: Double, norm2: Double): Double = score / (norm1 + norm2 - score)

}
