package com.github.libsml.optimization

import com.github.libsml.math.function.Function
import com.github.libsml.math.linalg.Vector

/**
 * Created by huangyu on 15/8/14.
 */
trait Optimizer extends Iterable[OptimizerResult] {


  override def iterator(): Iterator[OptimizerResult] = {
    new Iterator[OptimizerResult] {
      override def hasNext: Boolean = !isConvergence()

      override def next(): OptimizerResult = nextIteration()
    }
  }

  def optimize(): (Vector, Double) = {
    val it = iterator()
    while (it.hasNext) {
      it.next()
    }
    (weight, f)
  }

  def prior(weight: Vector): this.type

  def setFunction(function: Function): this.type

  def weight: Vector

  def f: Double

  def isConvergence(): Boolean

  def nextIteration(): OptimizerResult

}

case class OptimizerResult(val w: Vector,
                           val g: Option[Vector] = None,
                           val f: Option[Double] = None,
                           val msg: Option[String] = None) {

}