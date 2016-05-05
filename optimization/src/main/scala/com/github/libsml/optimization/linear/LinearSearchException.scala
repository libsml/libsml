package com.github.libsml.optimization.linear

import com.github.libsml.commons.LibsmlException

/**
 * Created by huangyu on 15/9/13.
 */
class LinearSearchException(message: String, cause: Throwable) extends LibsmlException(message, cause) {

  def this(message: String) = this(message, null)
}
