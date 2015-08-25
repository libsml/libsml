package com.github.libsml.model

/**
 * Created by huangyu on 15/8/14.
 */
trait Model[P, W] {

  def update(w: W): this.type

  def probability(testPoint: P, w: W): Double

}
