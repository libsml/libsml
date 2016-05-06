package com.github.libsml.model.recommendation

import com.github.libsml.math.linalg.Vector

/**
 * Created by yellowhuang on 2016/5/6.
 */
trait Similarity extends Serializable {

  def norm(vector: Vector): Double

  def aggregate(score1: Double, score2: Double): Double = score1 + score2

  def similarity(score: Double, norm1: Double, norm2: Double): Double

  def join(score1: Double, score2: Double): Double = score1 * score2

}
