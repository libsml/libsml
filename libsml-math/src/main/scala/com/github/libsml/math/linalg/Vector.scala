package com.github.libsml.math.linalg

import java.io.PrintWriter
import java.util

import com.github.libsml.math.util.{HashFunctions, PrimeFinder}

import scala.util.Random


/**
 * Created by huangyu on 15/7/18.
 *
 * Represents a numeric vector, whose index type is Int and value type is Double.
 */
sealed trait Vector extends Serializable {

  def noZeroSize: Int

  def size: Int

  def update(i: Int, v: Double): Unit

  def update(i: Int, f: Double => Double): Unit


  /**
   * Gets the value of the ith element.
   * @param i index
   */
  def apply(i: Int): Double

  def foreachNoZero(f: (Int, Double) => Unit): Unit

  override def equals(other: Any): Boolean = {
    other match {
      case v2: Vector => {
        if (this.noZeroSize != v2.noZeroSize) {
          return false
        }
        this.foreachNoZero { case (index, value) =>
          try {
            if (v2(index) != value) return false
          } catch {
            case e: Throwable => return false;
          }
        }

        return true
      }
      case _ => false
    }
  }

  override def hashCode(): Int = {
    var result: Int = noZeroSize + 31
    this.foreachNoZero { case (index, value) =>
      result = 31 * result + index
      // refer to {@link java.util.Arrays.equals} for hash algorithm
      val bits = java.lang.Double.doubleToLongBits(value)
      result = 31 * result + (bits ^ (bits >>> 32)).toInt
    }
    result
  }

  override def toString(): String = {
    val buf: StringBuilder = new StringBuilder()
    var count = 0
    buf.append('[')
    foreachNoZero((i, v) => {
      if (count != 0) {
        buf.append(',')
      }
      buf.append(i + "->" + v)
      count += 1
    })
    buf.append(']')
    buf.toString()
  }
}

class DenseVector(val values: Array[Double]) extends Vector {

  require(values != null, "DenseVector exception:values is null.")

  override val size: Int = values.length

  override def noZeroSize: Int = {
    values.count(_ != 0)
  }

  //  override def toString: String = values.mkString("[", ",", "]")


  override def equals(other: Any): Boolean = {
    other match {
      case v2: DenseVector => return util.Arrays.equals(values, v2.values)
    }
    super.equals(other)
  }


  override def hashCode(): Int = super.hashCode()

  override def foreachNoZero(f: (Int, Double) => Unit) = {
    var i = 0
    val localValuesSize = values.size
    val localValues = values

    while (i < localValuesSize) {
      if (localValues(i) != 0) {
        f(i, localValues(i))
      }
      i += 1
    }
  }

  override def update(i: Int, v: Double): Unit = {
    if (i < 0 || i >= size) throw new IndexOutOfBoundsException(i + " not in [0," + size + ")")
    values(i) = v
  }

  override def update(i: Int, f: (Double) => Double): Unit = {
    if (i < 0 || i >= size) throw new IndexOutOfBoundsException(i + " not in [0," + size + ")")
    values(i) = f(values(i))
  }

  override def apply(i: Int): Double = {
    if (i < 0 || i >= size) throw new IndexOutOfBoundsException(i + " not in [0," + size + ")")
    values(i)
  }


}

class SparseVector(var indices: Array[Int], var values: Array[Double])
  extends Vector {


  require(indices != null, "SparkVector exception:indices is null.")
  require(values != null, "SparkVector exception:values is null.")
  require(indices.length == values.length, "SparkVector exception:indices length=%d should equal values length=%d"
    .format(indices.length, values.length))

  override val size: Int = Int.MaxValue
  var used = indices.length

  override def noZeroSize: Int = values.count(_ != 0)


  private[libsml] var lastReturnedPos = -1

  /**
   * Returns the offset into index and data for the requested vector
   * index.  If the requested index is not found, the  value is
   * negative and can be converted into an insertion point with ~rv.
   */
  protected final def findOffset(i: Int): Int = {

    if (i < 0 || i >= size)
      throw new IndexOutOfBoundsException("Index " + i + " out of bounds [0," + size + ")")
    if (used == 0) {
      // empty list do nothing
      -1
    } else {
      if (i > indices(used - 1)) {
        ~used
      } else {
        var begin = 0

        var end = used - 1
        // Simple optimization: position i can't be after offset i.
        if (end > i)
          end = i

        var found = false
        var mid = (begin + end) >> 1

        // another optimization: cache the last found
        // position and restart binary search from lastReturnedPos
        // This is thread safe because writes of ints
        // are atomic on the JVM.
        // We actually don't want volatile here because
        // we want a poor-man's threadlocal...
        // be sure to make it a local though, so it doesn't
        // change from under us.
        val l = lastReturnedPos
        if (l >= 0 && l < end) {
          mid = l
        }

        // unroll the loop once. We're going to
        // check mid and mid+1, because if we're
        // iterating in order this will give us O(1) access
        val mi = indices(mid)
        if (mi == i) {
          found = true
        } else if (mi > i) {
          end = mid - 1
        } else {
          // mi < i
          begin = mid + 1
        }

        // a final stupid check:
        // we're frequently iterating
        // over all elements, so we should check if the next
        // pos is the right one, just to see
        if (!found && mid < end) {
          val mi = indices(mid + 1)
          if (mi == i) {
            mid = mid + 1
            found = true
          } else if (mi > i) {
            end = mid
          } else {
            // mi < i
            begin = mid + 2
          }
        }
        if (!found)
          mid = (end + begin) >> 1

        while (!found && begin <= end) {
          //          println("begin:"+begin+" end:"+end+" mid:"+mid)
          //          println("i:"+i+" ind:"+indices(mid))
          if (i < indices(mid)) {
            end = mid - 1
            mid = (begin + end) >> 1
          } else if (i > indices(mid)) {
            begin = mid + 1
            mid = (begin + end) >> 1
          } else {
            found = true
          }
        }
        val result = if (found || mid < 0)
          mid
        else if (i <= indices(mid))
          ~mid
        else
          ~(mid + 1)
        lastReturnedPos = result
        result
      }

    }
  }

  override def apply(i: Int): Double = {
    if (i < 0) throw new IndexOutOfBoundsException(i + " not in [0," + size + ")")
    val offset = findOffset(i)
    if (offset >= 0) values(offset) else 0
  }

  /**
   * Sets the given value at the given index if the value is not
   * equal to the current default.  The data and
   * index arrays will be grown to support the insertion if
   * necessary.  The growth schedule doubles the amount
   * of allocated memory at each allocation request up until
   * the sparse array contains 1024 values, at which point
   * the growth is additive: an additional n * 1024 spaces will
   * be allocated for n in 1,2,4,8,16.  The largest amount of
   * space added to this vector will be an additional 16*1024*(sizeof(Elem)+4),
   * which is 196608 bytes at a time for a SparseVector[Double],
   * although more space is needed temporarily while moving to the
   * new arrays.
   */
  override def update(i: Int, v: Double): Unit = {
    if (i < 0) throw new IndexOutOfBoundsException(i + " not in [0," + size + ")")
    val offset = findOffset(i)
    updateWithOffset(i, offset, v)
  }

  override def update(i: Int, f: (Double) => Double): Unit = {
    if (i < 0) throw new IndexOutOfBoundsException(i + " not in [0," + size + ")")
    val offset = findOffset(i)

    updateWithOffset(i, offset, if (offset >= 0) f(values(offset)) else f(0))
  }

  private def updateWithOffset(i: Int, offset: Int, v: Double): Unit = {

    if (offset >= 0) {
      // found at offset
      values(offset) = v
    } else if (v != 0) {

      val insertPos = ~offset
      used += 1
      if (used > values.length) {
        grow(insertPos)
      } else if (used - insertPos > 1) {
        // need to make room for new element mid-array
        System.arraycopy(indices, insertPos, indices, insertPos + 1, used - insertPos - 1)
        System.arraycopy(values, insertPos, values, insertPos + 1, used - insertPos - 1)

      }

      // assign new value
      indices(insertPos) = i
      values(insertPos) = v
    }
  }

  override def foreachNoZero(f: (Int, Double) => Unit): Unit = {
    var i = 0
    val localValuesSize = values.size
    val localIndices = indices
    val localValues = values

    while (i < localValuesSize) {
      if (localValues(i) != 0) {
        f(localIndices(i), localValues(i))
      }
      i += 1
    }
  }

  //  override def toString: String =
  //    "(%s,%s,%s)".format(size, indices.mkString("[", ",", "]"), values.mkString("[", ",", "]"))


  override def equals(other: Any): Boolean = {
    other match {
      case v2: SparseVector =>
        if (v2.used != this.used)
          return false
        else
          Vector.equals(indices, v2.indices, 0, 0, used) && Vector.equals(values, v2.values, 0, 0, used)
    }
    super.equals(other)
  }


  override def hashCode(): Int = super.hashCode()

  private def grow(insertPos: Int): Unit = {

    var newLength = {
      if (values.length == 0) {
        4
      }
      else if (values.length < 0x0400) {
        values.length * 2
      }
      else if (values.length < 0x0800) {
        values.length + 0x0400
      }
      else if (values.length < 0x1000) {
        values.length + 0x0800
      }
      else if (values.length < 0x2000) {
        values.length + 0x1000
      }
      else if (values.length < 0x4000) {
        values.length + 0x2000
      }
      else {
        values.length + 0x4000
      }
    }
    if (newLength > size) {
      newLength = size
    }

    // allocate new arrays
    val newIndex = util.Arrays.copyOf(indices, newLength)
    val newData = util.Arrays.copyOf(values, newLength)

    // copy existing data into new arrays
    System.arraycopy(indices, insertPos, newIndex, insertPos + 1, used - insertPos - 1)
    System.arraycopy(values, insertPos, newData, insertPos + 1, used - insertPos - 1)

    // update pointers
    indices = newIndex
    values = newData
  }


}


class MapVector(initCapacity: Int,
                private[this] var minLoadFactor: Double,
                private[this] var maxLoadFactor: Double) extends Vector {
  checkArguments(initCapacity, minLoadFactor, maxLoadFactor)

  //for test
  var capacity = PrimeFinder.nextPrime(initCapacity)

  //  if (capacity == 0) {
  //    capacity = 1
  //  }

  private[libsml] var table: Array[Int] = Array.fill(capacity)(-1)
  private[libsml] var values: Array[Double] = Array.fill(capacity)(0)
  private[this] var distinct = 0
  private[this] var freeEntries = table.length

  maxLoadFactor = if (PrimeFinder.LARGEST_PRIME == capacity) 1.0 else maxLoadFactor

  override val size = Int.MaxValue

  private var lowWaterMark = 0
  private var highWaterMark = chooseHighWaterMark(capacity, this.maxLoadFactor)


  def clear(): Unit = {
    util.Arrays.fill(table, -1)
    util.Arrays.fill(values, 0)
    freeEntries = table.length
    distinct = 0
  }

  /**
   * Trims the capacity of the receiver to be the receiver's current size. Releases any superfluous internal memory. An
   * application can use this operation to minimize the storage of the receiver.
   */
  def trimToSize() {
    val newCapacity: Int = PrimeFinder.nextPrime((1 + 1.2 * size).toInt)
    if (table.length > newCapacity) {
      rehash(newCapacity)
    }
  }


  def keys(): Array[Int] = {
    val keys = new Array[Int](noZeroSize)
    var index = 0
    var i = 0
    while (i < table.length) {
      if (table(i) != 0) {
        keys(index) = table(i)
        index += 1
      }
      i += 1
    }
    keys
  }

  override def noZeroSize: Int = distinct

  override def update(i: Int, v: Double): Unit = {
    if (i < 0) throw new IndexOutOfBoundsException(i + " not in [0," + size + ")")
    val pos = indexOfInsertion(i)
    updateWithInsertion(i, pos, v)
  }

  override def update(i: Int, f: (Double) => Double): Unit = {
    if (i < 0) throw new IndexOutOfBoundsException(i + " not in [0," + size + ")")
    val pos = indexOfInsertion(i)
    updateWithInsertion(i, pos, if (pos < 0) f(values(-pos - 1)) else f(0))
  }

  private[this] def updateWithInsertion(i: Int, pos: Int, v: Double): Unit = {

    var j = pos
    if (j < 0) {
      //already contained
      j = -j - 1
      if (v == 0 && values(j) != 0) {
        distinct -= 1
      }
      values(j) = v
      if (this.distinct < this.lowWaterMark) {
        val newCapacity: Int = chooseShrinkCapacity(this.distinct, this.minLoadFactor, this.maxLoadFactor)
        rehash(newCapacity)
      }
      return
    }

    if (v == 0) {
      return
    }

    if (this.distinct > this.highWaterMark) {
      val newCapacity: Int = chooseGrowCapacity(this.distinct + 1, this.minLoadFactor, this.maxLoadFactor)
      rehash(newCapacity)
      updateWithInsertion(i, indexOfInsertion(i), v)
      return
    }

    if (table(j) == -1) {
      freeEntries -= 1
    }
    table(j) = i
    values(j) = v
    if (v != 0) {
      distinct += 1
    }

    if (this.freeEntries < 1) {
      val newCapacity: Int = chooseGrowCapacity(this.distinct + 1, this.minLoadFactor, this.maxLoadFactor)
      rehash(newCapacity)
    }
  }

  /**
   * Gets the value of the ith element.
   * @param i index
   */
  override def apply(i: Int): Double = {
    if (i < 0) throw new IndexOutOfBoundsException(i + " not in [0," + size + ")")
    val j: Int = indexOfKey(i)
    if (j < 0) {
      0
    } else {
      values(j)
    }
  }

  override def foreachNoZero(f: (Int, Double) => Unit): Unit = {
    var j = 0
    while (j < table.length) {
      if (values(j) != 0) {
        f(table(j), values(j))
      }
      j += 1
    }
  }

  /**
   * @param key the key to be added to the receiver.
   * @return the index where the key would need to be inserted, if it is not already contained. Returns -index-1 if the
   *         key is already contained at slot index. Therefore, if the returned index < 0, then it is already contained
   *         at slot -index-1. If the returned index >= 0, then it is NOT already contained and should be inserted at
   *         slot index.
   */
  private def indexOfInsertion(key: Int): Int = {
    val length = table.length
    val hash = HashFunctions.hash(key) & 0x7FFFFFFF
    var i: Int = hash % length
    var decrement: Int = hash % (length - 2)
    //int decrement = (hash / length) % length;
    if (decrement == 0) {
      decrement = 1
    }


    // stop if we find a removed or free slot, or if we find the key itself
    // do NOT skip over removed slots (yes, open addressing is like that...)
    while (values(i) != 0 && table(i) != key) {
      i -= decrement
      if (i < 0) {
        i += length
      }

    }


    if (values(i) == 0 && table(i) != -1) {
      val j = i
      while (table(i) != -1 && table(i) != key) {
        i -= decrement
        if (i < 0) {
          i += length
        }
      }
      if (table(i) == -1) {
        i = j
      }

    }

    if (values(i) != 0) {
      -i - 1
    } else {
      i
    }

  }

  /**
   * @param key the key to be searched in the receiver.
   * @return the index where the key is contained in the receiver, returns -1 if the key was not found.
   */
  private def indexOfKey(key: Int): Int = {
    val length = table.length
    val hash = HashFunctions.hash(key) & 0x7FFFFFFF
    var i: Int = hash % length
    var decrement: Int = hash % (length - 2)
    //int decrement = (hash / length) % length;
    if (decrement == 0) {
      decrement = 1
    }



    while (table(i) != -1 && table(i) != key) {


      i -= decrement
      if (i < 0) {
        i += length
      }
    }
    if (table(i) == -1) {
      -1
    } else {
      i
    }


  }

  private def rehash(newCapacity: Int): Unit = {
    val oldCapacity: Int = table.length
    //if (oldCapacity == newCapacity) return;
    val oldTable: Array[Int] = table
    val oldValues: Array[Double] = values

    table = Array.fill(newCapacity)(-1)
    values = new Array[Double](newCapacity)

    this.lowWaterMark = chooseLowWaterMark(newCapacity, this.minLoadFactor)
    this.highWaterMark = chooseHighWaterMark(newCapacity, this.maxLoadFactor)

    this.freeEntries = newCapacity - this.distinct


    //    this.freeEntries = newCapacity - this.distinct

    var i = oldCapacity - 1
    while (i >= 0) {
      if (oldValues(i) != 0) {
        val element: Int = oldTable(i)
        val index: Int = indexOfInsertion(element)
        this.table(index) = element
        this.values(index) = oldValues(i)
      }
      i -= 1
    }
  }


  /**
   * Returns new high water mark threshold based on current capacity and maxLoadFactor.
   *
   * @return int the new threshold.
   */
  private def chooseHighWaterMark(capacity: Int, maxLoad: Double): Int = {
    return Math.min(capacity - 2, (capacity * maxLoad).toInt)
  }

  /**
   * Returns new low water mark threshold based on current capacity and minLoadFactor.
   *
   * @return int the new threshold.
   */
  private def chooseLowWaterMark(capacity: Int, minLoad: Double): Int = {
    return (capacity * minLoad).toInt
  }

  /**
   * Chooses a new prime table capacity optimized for shrinking that (approximately) satisfies the invariant <tt>c *
   * minLoadFactor <= size <= c * maxLoadFactor</tt> and has at least one FREE slot for the given size.
   */
  private def chooseShrinkCapacity(size: Int, minLoad: Double, maxLoad: Double): Int = {
    return PrimeFinder.nextPrime(Math.max(size + 1, ((4 * size / (minLoad + 3 * maxLoad))).toInt))
  }

  /**
   * Chooses a new prime table capacity optimized for growing that (approximately) satisfies the invariant <tt>c *
   * minLoadFactor <= size <= c * maxLoadFactor</tt> and has at least one FREE slot for the given size.
   */
  private def chooseGrowCapacity(size: Int, minLoad: Double, maxLoad: Double): Int = {
    return PrimeFinder.nextPrime(Math.max(size + 1, ((4 * size / (3 * minLoad + maxLoad))).toInt))
  }

  private def checkArguments(capacity: Int, minLoadFactor: Double, maxLoadFactor: Double): Unit = {
    if (capacity < 0) {
      throw new IllegalArgumentException("Initial Capacity must not be less than zero: " + capacity)
    }
    if (minLoadFactor < 0.0 || minLoadFactor >= 1.0) {
      throw new IllegalArgumentException("Illegal minLoadFactor: " + minLoadFactor)
    }
    if (maxLoadFactor <= 0.0 || maxLoadFactor >= 1.0) {
      throw new IllegalArgumentException("Illegal maxLoadFactor: " + maxLoadFactor)
    }
    if (minLoadFactor >= maxLoadFactor) {
      throw new IllegalArgumentException("Illegal minLoadFactor: " + minLoadFactor + " and maxLoadFactor: " + maxLoadFactor)
    }
  }

}

object Vector {

  val TYPE: Class[Vector] = classOf[Vector]

  val DEFAULT_CAPACITY: Int = 277
  val DEFAULT_MIN_LOAD_FACTOR: Double = 0.2
  val DEFAULT_MAX_LOAD_FACTOR: Double = 0.75

  def apply(indices: Array[Int], values: Array[Double]): SparseVector
  = new SparseVector(indices, values)

  def apply(values: Array[Double]): DenseVector
  = new DenseVector(values)

  def apply(initialCapacity: Int, minLoadFactor: Double, maxLoadFactor: Double): MapVector = {
    new MapVector(initialCapacity, minLoadFactor, maxLoadFactor)
  }

  def apply(initialCapacity: Int): MapVector = {
    apply(initialCapacity, DEFAULT_MIN_LOAD_FACTOR, DEFAULT_MAX_LOAD_FACTOR)
  }

  def apply(): MapVector = {
    apply(DEFAULT_CAPACITY)
  }

  def equals(a: Array[Int], a2: Array[Int], s: Int, s2: Int, lenght: Int): Boolean = {
    if (a eq a2) return true
    if (a == null || a2 == null) return false
    var i = s
    var i2 = s2
    while (i < lenght + s && i2 < lenght + s2) {
      if (a(i) != a2(i)) return false
      i += 1
      i2 += 1
    }
    return true
  }

  def equals(a: Array[Double], a2: Array[Double], s: Int, s2: Int, lenght: Int): Boolean = {
    if (a eq a2) return true
    if (a == null || a2 == null) return false
    var i = s
    var i2 = s2
    while (i < lenght + s && i2 < lenght + s2) {
      if (java.lang.Double.doubleToLongBits(a(i)) != java.lang.Double.doubleToLongBits(a2(i)))
        return false
      i += 1
      i2 += 1
    }
    return true
  }
}


object Vectors {


  def main(args: Array[String]): Unit = {
    val p = new PrintWriter("tmp")
    //    val v1 = Vector(20000000)
    val v1: SparseVector = Vector(Array(), Array())
    for (i <- 0 to 300000) {
      val k = Math.abs(Random.nextInt(300000))

      val v = Random.nextInt(300)
      println(i + ":" + k + ":" + v)
      v1(k) = v
      println("l:" + v1.indices.length)
      //      println(v1)
      //      println("active:" + v1.table.length + "|nozero:" + v1.noZeroSize)
      //      p.println(v1)
    }

    //    for (i <- 0 to 300) {
    //      val k = MLMath.abs(Random.nextInt(300))
    //
    //      p.println(i + ":" + k + ":" + 0)
    //      v1(k) = 0
    //      p.println("active:" + v1.table.length + "|nozero:" + v1.noZeroSize)
    //      p.println(v1)
    //    }
    //
    //    p.println(v1(111))
    //    p.println(v1(112))
    //    p.println(v1(113))
    //    p.println(v1.count)
    //    p.println(v1.count.toDouble/605)


    p.close()
  }
}