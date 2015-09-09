package com.github.libsml.commons.util

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import com.github.libsml.commons.LibsmlException


/**
 * Created by huangyu on 15/9/7.
 */
object Utils extends Logging {

  def currentTime(): String = {
    val dateFormat: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    dateFormat.format(new Date)
  }

  def className(fullClassName: String): String = {
    fullClassName.substring(fullClassName.lastIndexOf('.') + 1)
  }

  def mkdirs(path: String, overWrite: Boolean) = {
    val f = new File(path)
    if (overWrite) {
      if (f.exists()) {
        f.delete()
      }
    }
    if (f.exists()) {
      logError("Path:" + path + " exists!")
      throw new LibsmlException("Path:" + path + " exists!")
    }
    f.mkdirs()
  }


}
