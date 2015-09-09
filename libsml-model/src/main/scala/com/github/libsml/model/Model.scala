package com.github.libsml.model

/**
 * Created by huangyu on 15/8/14.
 */
trait Model[P, W] extends Serializable {

  def update(w: W): this.type

  def score(testPoint: P, k: Double): Double

  def value(testPoint: P): Double

  def save(path: String): Unit

}
