package com.github.libsml.model.evaluation

import com.github.libsml.model.Model

/**
 * Created by huangyu on 15/9/6.
 */
trait Evaluator[P, W] {

  def evaluator(model: Model[P, W])

}
