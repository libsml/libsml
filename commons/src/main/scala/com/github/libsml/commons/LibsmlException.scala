package com.github.libsml.commons

/**
 * Created by huangyu on 15/9/8.
 */
class LibsmlException(message: String, cause: Throwable) extends Exception(message, cause) {

  def this(message: String) = this(message, null)
}
