package com.github.libsml.math.linalg

/**
 * Created by huangyu on 15/8/18.
 */
object BLASTest {

  def euclideanNormTest(): Unit = {
    println(BLAS.euclideanNorm(Vector(Array[Double]())))
    println(BLAS.euclideanNorm(Vector(Array[Double](4))))
    println(BLAS.euclideanNorm(Vector(Array[Double](2,2))))
    println(BLAS.euclideanNorm(Vector(Array[Double](3,3,3,-3))))
    val mv=Vector()
    mv(2)=3
    println(BLAS.euclideanNorm(mv))
    mv(2) = -3
    println(BLAS.euclideanNorm(mv))

    mv(4) = -3
    println(BLAS.euclideanNorm(mv))
    mv(5) = -3
    mv(6) = -3
    println(BLAS.euclideanNorm(mv))
  }

  def main(args: Array[String]) {

    euclideanNormTest()
  }
}
