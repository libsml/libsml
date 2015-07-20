package com.github.libsml.math.util

/**
 * Created by huangyu on 15/7/20.
 *
 * Provides various hash functions.
 */
object HashFunctions {


  /**
   * Returns a hashcode for the specified value.
   *
   * @return a hash code value for the specified value.
   */
  def hash(value: Char): Int = {
    return value
  }

  /**
   * Returns a hashcode for the specified value.
   *
   * @return a hash code value for the specified value.
   */
  def hash(value: Double): Int = {
    val bits: Long = java.lang.Double.doubleToLongBits(value)
    return (bits ^ (bits >>> 32)).toInt
  }

  /**
   * Returns a hashcode for the specified value.
   *
   * @return a hash code value for the specified value.
   */
  def hash(value: Float): Int = {
    return java.lang.Float.floatToIntBits(value * 663608941.737f)
  }

  /**
   * Returns a hashcode for the specified value.
   * The hashcode computation is similar to the last step
   * of MurMurHash3.
   *
   * @return a hash code value for the specified value.
   */
  def hash(value: Int): Int = {
    var h: Int = value
    h ^= h >>> 16
    h *= 0x85ebca6b
    h ^= h >>> 13
    h *= 0xc2b2ae35
    h ^= h >>> 16
    return h
  }

  /**
   * Returns a hashcode for the specified value.
   *
   * @return a hash code value for the specified value.
   */
  def hash(value: Long): Int = {
    return (value ^ (value >> 32)).toInt
  }

  /**
   * Returns a hashcode for the specified object.
   *
   * @return a hash code value for the specified object.
   */
  def hash(`object`: AnyRef): Int = {
    return if (`object` == null) 0 else `object`.hashCode
  }

  /**
   * Returns a hashcode for the specified value.
   *
   * @return a hash code value for the specified value.
   */
  def hash(value: Short): Int = {
    return value
  }

  /**
   * Returns a hashcode for the specified value.
   *
   * @return a hash code value for the specified value.
   */
  def hash(value: Boolean): Int = {
    return if (value) 1231 else 1237
  }
}
