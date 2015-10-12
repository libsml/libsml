package com.github.libsml.math.linalg


import com.github.fommil.netlib.{BLAS => NetlibBLAS, F2jBLAS}
import com.github.fommil.netlib.BLAS.{getInstance => NativeBLAS}
import java.util.Arrays

/**
 * Created by yellowhuang on 2015/7/17.
 */
object BLAS extends Serializable {

  @transient private var _f2jBLAS: NetlibBLAS = _
  @transient private var _nativeBLAS: NetlibBLAS = _

  // For level-1 routines, we use Java implementation.
  private def f2jBLAS: NetlibBLAS = {
    if (_f2jBLAS == null) {
      _f2jBLAS = new F2jBLAS
    }
    _f2jBLAS
  }

  // For level-3 routines, we use the native BLAS.
  private def nativeBLAS: NetlibBLAS = {
    if (_nativeBLAS == null) {
      _nativeBLAS = NativeBLAS
    }
    _nativeBLAS
  }

  def sum(x: Vector): Double = {
    var sum: Double = 0
    x.foreachNoZero((_, v) => sum += v)
    sum
  }

  def dot(x: Vector, y: Vector): Double = {
    (x, y) match {
      case (dx: DenseVector, dy: DenseVector) =>
        dot(dx, dy)
      case (sx: SparseVector, sy: SparseVector) =>
        dot(sx, sy)
      case (mx: MapVector, my: MapVector) => {
        val rmx = if (mx.noZeroSize < my.noZeroSize) mx else my
        val rmy = if (mx.noZeroSize >= my.noZeroSize) mx else my
        defaultDot(rmx, rmy)
      }
      case (sx: SparseVector, dy: DenseVector) =>
        dot(sx, dy)
      case (dx: DenseVector, sy: SparseVector) =>
        dot(sy, dx)
      case (dx: DenseVector, my: MapVector) =>
        defaultDot(my, dx)
      case (mx: MapVector, sy: SparseVector) =>
        defaultDot(sy, mx)
      case _ =>
        defaultDot(x, y)

    }
  }


  /**
   * dot(x,y)
   */
  private def defaultDot(x: Vector, y: Vector): Double = {
    var dot: Double = 0
    x.foreachNoZero(dot += y(_) * _)
    dot
  }

  /**
   * dot(x, y)
   */
  private def dot(x: DenseVector, y: DenseVector): Double = {
    val n = x.size
    f2jBLAS.ddot(n, x.values, 1, y.values, 1)
  }

  /**
   * dot(x, y)
   */
  private def dot(x: SparseVector, y: DenseVector): Double = {
    val xValues = x.values
    val xIndices = x.indices
    val yValues = y.values
    val nnz = xIndices.size

    var sum = 0.0
    var k = 0
    while (k < nnz) {
      sum += xValues(k) * yValues(xIndices(k))
      k += 1
    }
    sum
  }

  /**
   * dot(x, y)
   */
  private def dot(x: SparseVector, y: SparseVector): Double = {
    val xValues = x.values
    val xIndices = x.indices
    val yValues = y.values
    val yIndices = y.indices
    val nnzx = xIndices.size
    val nnzy = yIndices.size

    var kx = 0
    var ky = 0
    var sum = 0.0
    // y catching x
    while (kx < nnzx && ky < nnzy) {
      val ix = xIndices(kx)
      while (ky < nnzy && yIndices(ky) < ix) {
        ky += 1
      }
      if (ky < nnzy && yIndices(ky) == ix) {
        sum += xValues(kx) * yValues(ky)
        ky += 1
      }
      kx += 1
    }
    sum
  }


  def axpy(a: Double, x: Vector, y: Vector): Unit = {

    (x, y) match {
      case (dx: DenseVector, dy: DenseVector) =>
        axpy(a, dx, dy)
      case _ =>
        defaultAxpy(a, x, y)
    }
  }

  /**
   * y += a * x
   */
  private def defaultAxpy(a: Double, x: Vector, y: Vector): Unit = {
    if (a != 1) {
      x.foreachNoZero((i, v) => y(i) = _ + v * a)
    }
    else {
      x.foreachNoZero((i, v) => y(i) = _ + v)
    }
  }

  /**
   * y += a * x
   */
  private def axpy(a: Double, x: DenseVector, y: DenseVector): Unit = {
    val n = x.size
    f2jBLAS.daxpy(n, a, x.values, 1, y.values, 1)
  }


  def zero(x: Vector): Unit = {
    x match {
      case mx: MapVector =>
        mx.clear()
      case sx: SparseVector =>
        Arrays.fill(sx.values, 0)
      case dx: DenseVector =>
        Arrays.fill(dx.values, 0)
      case _ =>
        throw new IllegalArgumentException("Vector zero exception")
      //        x.foreachNoZero((i, v) => x(i) = 0)
    }
  }

  private def defaultCopy(x: Vector, y: Vector): Unit = {
    zero(y)
    x.foreachNoZero(y(_) = _)
  }

  /**
   * y = x
   */
  def copy(x: Vector, y: Vector): Unit = {
    (x, y) match {
      case (dx: DenseVector, dy: DenseVector) =>
        require(dx.size == dy.size, "Vector copy exception!")
        Array.copy(dx.values, 0, dy.values, 0, dx.values.length)
      case (sx: SparseVector, sy: SparseVector) =>
        if (sx.used > sy.used) {

          // allocate new arrays
          sy.indices = new Array[Int](sx.used)
          sy.values = new Array[Double](sx.used)
          sy.used = sx.used
        }

        Array.copy(sx.indices, 0, sy.indices, 0, sx.indices.length)
        Array.copy(sx.values, 0, sy.values, 0, sx.values.length)
        sy.lastReturnedPos = sx.lastReturnedPos

        if (sx.used < sy.used) {
          Arrays.fill(sy.indices, sx.used, sy.used, 0)
          Arrays.fill(sy.values, sx.used, sy.used, 0.0)
        }

      case _ =>
        defaultCopy(x, y)
    }
  }

  def ncopy(x: Vector, y: Vector): Unit = {
    zero(y)
    axpy(-1, x, y)
  }

  def euclideanNorm(vector: Vector): Double = {

    val noZeroSize = vector.noZeroSize

    if (noZeroSize < 1) {
      0
    }
    else if (noZeroSize == 1) {
      var re = 0.0
      vector.foreachNoZero((_, v) => re = Math.abs(v))
      re
    }
    else {

      // this algorithm is (often) more accurate than just summing up the squares and taking the square-root afterwards
      var scale: Double = 0 // scaling factor that is factored out
      var sum: Double = 1 // basic sum of squares from which scale has been factored out

      vector.foreachNoZero((_, v) => {
        val abs: Double = Math.abs(v)

        // try to get the best scaling factor
        if (scale < abs) {
          val t: Double = scale / abs
          sum = 1 + sum * (t * t)
          scale = abs
        }
        else {
          val t: Double = abs / scale
          sum += t * t
        }
      })
      scale * Math.sqrt(sum)

    }
  }

  /**
   * x = a * x
   */
  def scal(a: Double, x: Vector): Unit = {
    if (a == 0) {
      zero(x)
    } else if (a != 1) {
      x match {
        case sx: SparseVector =>
          f2jBLAS.dscal(sx.values.size, a, sx.values, 1)
        case dx: DenseVector =>
          f2jBLAS.dscal(dx.values.size, a, dx.values, 1)
        case mx: MapVector =>
          f2jBLAS.dscal(mx.values.size, a, mx.values, 1)
        case _ =>
          throw new IllegalArgumentException("Scale exception!")
      }
    }
  }

}
