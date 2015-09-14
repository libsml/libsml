package com.github.libsml.optimization.linear


import com.github.libsml.commons.LibsmlException
import com.github.libsml.math.function.Function
import com.github.libsml.commons.util.MapWrapper._

/**
 * Created by huangyu on 15/8/26.
 */
trait LinearSearch {

  def search(function: Function[Double], initStep: Double): (Int, Double, Double)
}

object LinearSearch {
  def apply(map: Map[String, String]): LinearSearch = {
    val linearType = map.getOrElse("linearSearch.type", "strongWolf")
    val para = new LinerSearchParameter()
    para.ftol = map.getDouble("linearSearch.ftol", para.ftol)
    para.gtol = map.getDouble("linearSearch.gtol", para.gtol)
    para.maxLinesearch = map.getInt("linearSearch.maxLinesearch", para.maxLinesearch)
    para.maxStep = map.getDouble("linearSearch.maxStep", para.maxStep)
    para.minStep = map.getDouble("linearSearch.minStep", para.minStep)
    para.wolfe = map.getDouble("linearSearch.wolfe", para.wolfe)
    para.xtol = map.getDouble("linearSearch.xtol", para.xtol)

    if (para.minStep < 0) {
      throw new LibsmlException("Linear search parameter check exception.")
    }
    if (para.maxStep < 0) {
      throw new LibsmlException("Linear search parameter check exception.")
    }
    if (para.ftol < 0) {
      throw new LibsmlException("Linear search parameter check exception.")
    }

    if (para.gtol < 0) {
      throw new LibsmlException("Linear search parameter check exception.")
    }
    if (para.xtol < 0) {
      throw new LibsmlException("Linear search parameter check exception.")
    }
    if (para.maxLinesearch <= 0) {
      throw new LibsmlException("Linear search parameter check exception.")
    }

    linearType.trim match {
      case "armijo" =>
        new LinearSearchArmijo(para)
      case "wolf" =>
        new LinearSearchWolf(para)
      case "strongWolf" =>
        new LinearSearchStrongWolf(para)
      case _ =>
        throw new LibsmlException(s"Unsupported linear search type:${linearType},linear type is:armijo,wolf,strongWolf.")
    }
  }
}