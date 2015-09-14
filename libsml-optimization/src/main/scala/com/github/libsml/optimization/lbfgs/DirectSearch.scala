package com.github.libsml.optimization.lbfgs

import com.github.libsml.commons.LibsmlException
import com.github.libsml.math.linalg.Vector

/**
 * Created by huangyu on 15/9/14.
 */
trait DirectSearch {

  def direct(d: Vector, x: Vector, xp: Vector, g: Vector, gp: Vector, sg: Option[Vector] = None): Vector
}


object DirectSearch {

  def apply(map: Map[String, String], m: Int): DirectSearch = {
    val t = map.getOrElse("directSearch.type", "single")
    t match {
      case "single" =>
        new SingleDirectSearch(m)
      case "spark" =>
      //TODO:spark direct search
        null
      case _ =>
        throw new LibsmlException(s"Unsupported direct search type:${t}")
    }
  }
}