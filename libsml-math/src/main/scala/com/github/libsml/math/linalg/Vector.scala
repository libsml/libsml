package com.github.libsml.math.linalg

import java.util

import com.github.libsml.math.util.{HashFunctions, PrimeFinder}


/**
 * Created by huangyu on 15/7/18.
 *
 * Represents a numeric vector, whose index type is Int and value type is Double.
 */
trait Vector extends Serializable {

  def activeSize: Int

  def size: Int

  def update(i: Int, v: Double): Unit

  /**
   * Gets the value of the ith element.
   * @param i index
   */
  def apply(i: Int): Double

  def foreachActive(f: (Int, Double) => Unit): Unit

  override def equals(other: Any): Boolean = {
    other match {
      case v2: Vector => {
        this.foreachActive { case (index, value) =>
          // ignore explict 0 for comparison between sparse and dense
          try {
            if (v2(index) != value) return false
          } catch {
            case e: Throwable => return false;
          }
        }

        v2.foreachActive { case (index, value) =>
          // ignore explict 0 for comparison between sparse and dense
          try {
            if (this(index) != value) return false
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
    var result: Int = size + 31
    this.foreachActive { case (index, value) =>
      // ignore explict 0 for comparison between sparse and dense
      if (value != 0) {
        result = 31 * result + index
        // refer to {@link java.util.Arrays.equals} for hash algorithm
        val bits = java.lang.Double.doubleToLongBits(value)
        result = 31 * result + (bits ^ (bits >>> 32)).toInt
      }
    }
    result
  }
}

class DenseVector(val values: Array[Double]) extends Vector {

  require(values != null, "DenseVector exception:values is null.")

  override val size: Int = values.length

  override val activeSize: Int = {
    values.length
  }

  override def toString: String = values.mkString("[", ",", "]")


  override def equals(other: Any): Boolean = {
    other match {
      case v2: DenseVector => return util.Arrays.equals(values, v2.values)
    }
    super.equals(other)
  }


  override def hashCode(): Int = super.hashCode()

  override def foreachActive(f: (Int, Double) => Unit) = {
    var i = 0
    val localValuesSize = values.size
    val localValues = values

    while (i < localValuesSize) {
      f(i, localValues(i))
      i += 1
    }
  }

  override def update(i: Int, v: Double): Unit = {
    if (i < 0 || i >= size) throw new IndexOutOfBoundsException(i + " not in [0," + size + ")")
    values(i) = v
  }

  override def apply(i: Int): Double = {
    if (i < 0 || i >= size) throw new IndexOutOfBoundsException(i + " not in [0," + size + ")")
    values(i)
  }
}

class SparseVector(var indices: Array[Int], var values: Array[Double], override val size: Int = Int.MaxValue)
  extends Vector {

  require(indices != null, "SparkVector exception:indices is null.")
  require(values != null, "SparkVector exception:values is null.")
  require(indices.length == values.length, "SparkVector exception:indices length=%d should equal values length=%d"
    .format(indices.length, values.length))
  require(indices.length <= size, "SparkVector exception:indices and values length=%d should less than size=%d"
    .format(indices.length, size))

  var used = indices.length

  override def activeSize: Int = used


  private var lastReturnedPos = -1

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
          if (i < indices(mid)) {
            end = mid - 1
            mid = (begin + mid) >> 2
          } else if (i > indices(mid)) {
            begin = mid + 1
            mid = (begin + mid) >> 2
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
    val offset = findOffset(i)
    if (offset >= 0) {
      // found at offset
      values(offset) = v
    } else if (v != 0) {

      val insertPos = ~offset
      used += 1
      if (used > values.length) {

        // need to grow array
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

  override def foreachActive(f: (Int, Double) => Unit): Unit = {
    var i = 0
    val localValuesSize = values.size
    val localIndices = indices
    val localValues = values

    while (i < localValuesSize) {
      f(localIndices(i), localValues(i))
      i += 1
    }
  }

  override def toString: String =
    "(%s,%s,%s)".format(size, indices.mkString("[", ",", "]"), values.mkString("[", ",", "]"))


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
}


class MapVector(private[this] var capacity: Int,
                private[this] var minLoadFactor: Double,
                private[this] var maxLoadFactor: Double) extends Vector {
  checkArguments()

  capacity = PrimeFinder.nextPrime(capacity)

  //  if (capacity == 0) {
  //    capacity = 1
  //  }

  private[libsml] var table: Array[Int] = Array.fill(capacity)(-1)
  private[libsml] var values: Array[Double] = Array.fill(capacity)(0)

  maxLoadFactor = if (PrimeFinder.LARGEST_PRIME == capacity) 1.0 else maxLoadFactor
  override var size = 0
  private var lowWaterMark = 0
  private var highWaterMark = chooseHighWaterMark(capacity, this.maxLoadFactor)


  override def activeSize: Int = ???

  override def update(i: Int, v: Double): Unit = {
    var j = indexOfInsertion(i)
    if (j < 0) {
      j = -j - 1
      values(j) = v
      return
    }

  }

  /**
   * Gets the value of the ith element.
   * @param i index
   */
  override def apply(i: Int): Double = {
    val j: Int = indexOfKey(i)
    if (j < 0) {
      0
    } else {
      values(j)
    }
  }

  override def foreachActive(f: (Int, Double) => Unit): Unit = ???

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
    while (values(i) == 0 && table(i) != key) {
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

    table = new Array[Int](newCapacity)
    values = new Array[Double](newCapacity)

    this.lowWaterMark = chooseLowWaterMark(newCapacity, this.minLoadFactor)
    this.highWaterMark = chooseHighWaterMark(newCapacity, this.maxLoadFactor)


    //    this.freeEntries = newCapacity - this.distinct

    var i = oldCapacity - 1
    while (i >= 0) {
      if (values(i) != 0) {
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


  private def checkArguments(): Unit = {
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

  private def isFree(i: Int): Boolean = table(i) == -1

  private def isFull(i: Int): Boolean = table(i) != -1 && values(i) != 0

  private def isRemove(i: Int): Boolean = table(i) != -1 && values(i) == 0

}

object Vector {

  def apply(indices: Array[Int], values: Array[Double], size: Int = Int.MaxValue): SparseVector
  = new SparseVector(indices, values, size)


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
    //    val v1: Vector = new DenseVector(Array(1, 3, 4, 5))
    //    val v2: Vector = new DenseVector(Array(1, 3, 4, 5))
    //    println(v1 == v2)
    //    println(v1.hashCode())
    //    println(v2.hashCode())
    //    v1(1) = 2.3
    //    println(v1)
    //    println(v1 == v2)

    val v1 = Vector(Array(), Array())
    val v2 = Vector(Array(), Array())
    //    println(v1)
    //    println(v1.activeSize)
    //    v1(1) = 2.2
    //    println(v1)
    //    println(v1.activeSize)
    //    v1(2) = 2.55
    //    println(v1)
    //    println(v1.activeSize)

    println(v1 == v2)
    println(v1.hashCode())
    println(v2.hashCode())
    v1(3) = 1.2
    v2(3) = 1.2
    println(v1 == v2)
    println(v1.hashCode())
    println(v2.hashCode())
    v2(4) = 1.2
    println(v1 == v2)
    println(v1.hashCode())
    println(v2.hashCode())

  }
}