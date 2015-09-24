package com.github.libsml.commons.util

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import com.github.libsml.commons.LibsmlException

import scala.util.control.NonFatal


/**
 * Created by huangyu on 15/9/7.
 */
object Utils extends Logging {

  def diffDays(startDay: String, endDay: String): Int = {
    val dateFormat: SimpleDateFormat = new SimpleDateFormat("yyyyMMdd")
    val date1 = dateFormat.parse(startDay)
    val date2 = dateFormat.parse(endDay)
    val diff: Long = (date2.getTime() - date1.getTime()) / 1000 / 60 / 60 / 24
    diff.toInt
  }

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

  def tryLogNonFatalError(block: => Unit) {
    try {
      block
    } catch {
      case NonFatal(t) =>
        logError(s"Uncaught exception in thread ${Thread.currentThread().getName}", t)
    }
  }

  /**
   * Execute a block of code, then a finally block, but if exceptions happen in
   * the finally block, do not suppress the original exception.
   *
   * This is primarily an issue with `finally { out.close() }` blocks, where
   * close needs to be called to clean up `out`, but if an exception happened
   * in `out.write`, it's likely `out` may be corrupted and `out.close` will
   * fail as well. This would then suppress the original/likely more meaningful
   * exception from the original `out.write` call.
   */
  def tryWithSafeFinally[T](block: => T)(finallyBlock: => Unit): T = {
    // It would be nice to find a method on Try that did this
    var originalThrowable: Throwable = null
    try {
      block
    } catch {
      case t: Throwable =>
        // Purposefully not using NonFatal, because even fatal exceptions
        // we don't want to have our finallyBlock suppress
        originalThrowable = t
        throw originalThrowable
    } finally {
      try {
        finallyBlock
      } catch {
        case t: Throwable =>
          if (originalThrowable != null) {
            originalThrowable.addSuppressed(t)
            logWarning(s"Suppressing exception in finally: " + t.getMessage, t)
            throw originalThrowable
          } else {
            throw t
          }
      }
    }
  }

  def main(args: Array[String]) = {
    println(diffDays("20150101","20150202"))
  }

}
