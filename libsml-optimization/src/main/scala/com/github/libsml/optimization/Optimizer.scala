package com.github.libsml.optimization

import com.github.libsml.math.function.Function
import com.github.libsml.math.linalg.Vector

import scala.reflect.ClassTag

/**
 * Created by huangyu on 15/8/14.
 */
abstract class Optimizer[W] extends Iterable[OptimizerResult[W]] {


  override def iterator(): Iterator[OptimizerResult[W]] = {
    new Iterator[OptimizerResult[W]] {
      override def hasNext: Boolean = !isConvergence()

      override def next(): OptimizerResult[W] = nextIteration()
    }
  }

  def optimize(): (W, Double) = {
    val it = iterator()
    while (it.hasNext) {
      it.next()
    }
    (weight, f)
  }

  def prior(weight: W): this.type

  def setFunction(function: Function[W]): this.type

  def weight: W

  def f: Double

  def isConvergence(): Boolean

  def nextIteration(): OptimizerResult[W]

}

case class OptimizerResult[W](val w: W,
                                        val g: Option[W] = None,
                                        val f: Option[Double] = None,
                                        val msg: Option[String] = None) {
}