package com.github.libsml.optimization.liblinear

import java.util.Random

import com.github.libsml.math.linalg.{DenseVector, Vector}
import org.apache.spark.rdd.RDD

/**
 * Created by huangyu on 16/4/30.
 */
object NewGLMNET {

  val DEFAULT_RANDOM_SEED = 0L
  val random = new Random(DEFAULT_RANDOM_SEED)
  val nu = 1e-12
  val max_iter = 1000
  val max_newton_iter = 100
  val max_num_linesearch = 20
  val sigma = 0.01

  def solve(inverseData: RDD[(Array[Vector], Array[Byte], Array[Double], Array[Int])], l1: Double, eps: Double): Vector = {

    null

  }

  def swap(array: Array[Int], idxA: Int, idxB: Int) {
    val temp = array(idxA)
    array(idxA) = array(idxB)
    array(idxB) = temp
  }

  private def solveCD(ix: Array[Vector], y: Array[Byte], dataWeight: Array[Double], l1: Double, eps: Double,inner_eps:Double, _w: Vector,
                      xTd: Array[Double], exp_wTx: Array[Double],
                      fd: Array[Int], D: Array[Double], tau: Array[Double], xjneg_sum: Array[Double], Gmax_old: Double, _Gnorm1_init: Double): Unit = {

    var Gmax_new = 0.
    var Gnorm1_new = 0.
    var QP_Gmax_new = 0.
    var QP_Gnorm1_new = 0.
    val w_size = ix.length
    var active_size = w_size
    val l = y.length
    var z=0.



    val index = new Array[Int](w_size)
    val Hdiag = new Array[Double](w_size)
    val Grad = new Array[Double](w_size)
    val w = new Array[Double](w_size)
    val wpd = new Array[Double](w_size)


    for (i <- 0 until w_size) {
      index(i) = i
      w(i) = _w(fd(i))
      wpd(i) = w(i)
    }

    var s = 0
    while (s < active_size) {
      var isContinue = false
      val j = index(s)
      Hdiag(j) = nu
      Grad(j) = 0

      var tmp = 0
      ix(j).foreachNoZero((ind, v) => {
        Hdiag(j) += v * v * D(ind)
        tmp += v * tau(ind)
      })
      Grad(j) = -tmp + xjneg_sum(j)
      val Gp = Grad(j) + 1
      val Gn = Grad(j) - 1
      var violation = 0.

      if (w(j) == 0) {
        if (Gp < 0)
          violation = -Gp
        else if (Gn > 0)
          violation = Gn
        //outer-level shrinking
        else if (Gp > Gmax_old / l && Gn < -Gmax_old / l) {
          active_size -= 1
          swap(index, s, active_size)
          s -= 1
          isContinue = true
        }
      } else if (w(j) > 0)
        violation = Math.abs(Gp)
      else
        violation = Math.abs(Gn)

      if (!isContinue) {
        Gmax_new = Math.max(Gmax_new, violation)
        Gnorm1_new += violation
      }
      s += 1

    }
    val Gnorm1_init = if (_Gnorm1_init < 0) Gnorm1_new else Gnorm1_new
    if (Gnorm1_new > eps * Gnorm1_init) {

      var iter = 0
      var QP_Gmax_old = Double.PositiveInfinity
      var QP_active_size = active_size

      for (i <- 0 until l) xTd(i) = 0
      // optimize QP over wpd
      var isBreak = false
      while (iter < max_iter && !isBreak) {
        QP_Gmax_new = 0
        QP_Gnorm1_new = 0
        for (j <- 0 until QP_active_size) {
          val i = random.nextInt(QP_active_size - j)
          swap(index, i, j)
        }

        s = 0
        while (s < QP_active_size) {
          var isContinue2 = false
          val j = index(s)
          val H = Hdiag(j)
          var G = Grad(j) + (wpd(j) - w(j)) * nu
          ix(j).foreachNoZero((ind, v) => {
            G += v * D(ind) * xTd(ind)
          })

          val Gp = G + l1
          val Gn = G - l1
          var violation = 0.0
          if (wpd(j) == 0) {
            if (Gp < 0)
              violation = -Gp
            else if (Gn > 0)
              violation = Gn
            //inner-level shrinking
            else if (Gp > QP_Gmax_old / l && Gn < -QP_Gmax_old / l) {
              QP_active_size -= 1
              swap(index, s, QP_active_size)
              s -= 1
              isContinue2 = true
            }
          } else if (wpd(j) > 0)
            violation = Math.abs(Gp);
          else
            violation = Math.abs(Gn);

          if (!isContinue2) {
            QP_Gmax_new = Math.max(QP_Gmax_new, violation)
            QP_Gnorm1_new += violation

            // obtain solution of one-variable problem
            if (Gp < H * wpd(j))
              z = -Gp / H
            else if (Gn > H * wpd(j))
              z = -Gn / H
            else
              z = -wpd(j)
            if (Math.abs(z) >= 1.0e-12) {
              z = Math.min(Math.max(z, -10.0), 10.0)

              wpd(j) += z

              ix(j).foreachNoZero((ind, v) => {
                xTd(ind) += v * z
              })

            }
          }
          s += 1
        }
        iter += 1

        //          println(s"${QP_Gnorm1_new},${inner_eps},${Gnorm1_init},${QP_active_size}")
        if (QP_Gnorm1_new <= inner_eps * Gnorm1_init) {
          //inner stopping
          if (QP_active_size == active_size)
            isBreak = true
          //active set reactivation
          else {
            QP_active_size = active_size
            QP_Gmax_old = Double.PositiveInfinity
          }
        }
        else {
          QP_Gmax_old = QP_Gmax_new
        }
      }

      if (iter >= max_iter) println("WARNING: reaching max number of inner iterations\n")


//      Gmax_old = Gmax_new
//      printf("iter %3d  #CD cycles %d\n", newton_iter, iter)
    }


  }

  def solve(ix: Array[Vector], y: Array[Byte], dataWeight: Array[Double], l1: Double, eps: Double): Vector = {

    def getDataWeight(ind: Int): Double = if (dataWeight == null) 1.0 else dataWeight(ind)

    val w_size = ix.length
    val l = y.length
    val w = new DenseVector(new Array[Double](w_size))
    var j = 0
    var s = 0
    var newton_iter = 0
    var iter = 0
    var active_size = 0
    var QP_active_size = 0

    var inner_eps = 1.0
    var w_norm = 0.0
    var w_norm_new = 0.0
    var z = 0.0
    var G = 0.0
    var H = 0.0
    var Gnorm1_init = -1.0 // Gnorm1_init is initialized at the first iteration
    var Gmax_old = Double.PositiveInfinity
    var Gmax_new = 0.
    var Gnorm1_new = 0.
    var QP_Gmax_old = Double.PositiveInfinity
    var QP_Gmax_new = 0.0
    var QP_Gnorm1_new = 0.0
    var delta = 0.
    var negsum_xTd = 0.
    var cond = 0.

    val index = new Array[Int](w_size)
    val Hdiag = new Array[Double](w_size)
    val Grad = new Array[Double](w_size)
    val wpd = new Array[Double](w_size) // x+d
    val xjneg_sum = new Array[Double](w_size) // xjneg_sum(j) = sum_l(x_lj) s.t. y(l)==-1
    val xTd = new Array[Double](l) // xTd(l) = dot(x_l,d)
    val exp_wTx = new Array[Double](l) // exp_wTx(l) = exp{dot(w,x_l)}
    val exp_wTx_new = new Array[Double](l)
    val tau = new Array[Double](l)
    val D = new Array[Double](l)

    w_norm = 0
    j = 0
    while (j < w_size) {
      w_norm += Math.abs(w(j))
      wpd(j) = w(j)
      index(j) = j
      xjneg_sum(j) = 0
      ix(j).foreachNoZero((ind, v) => {
        exp_wTx(ind) += w(j) * v
        if (y(ind) == -1) {
          xjneg_sum(j) += getDataWeight(ind) * v
        }

      })
      j += 1
    }
    w_norm = l1 * w_norm

    j = 0
    while (j < l) {
      exp_wTx(j) = Math.exp(exp_wTx(j))
      val tau_tmp = 1 / (1 + exp_wTx(j))
      tau(j) = getDataWeight(j) * tau_tmp;
      D(j) = getDataWeight(j) * exp_wTx(j) * tau_tmp * tau_tmp;
      j += 1
    }
    var outBreak = false
    while (newton_iter < max_newton_iter & !outBreak) {
      Gmax_new = 0
      Gnorm1_new = 0
      active_size = w_size

      s = 0

      //compute Hdiag,Grad,Gmax,Gnorm1
      //outer-level shrinking
      while (s < active_size) {
        var isContinue = false
        j = index(s)
        Hdiag(j) = nu
        Grad(j) = 0

        var tmp = 0.0
        ix(j).foreachNoZero((ind, v) => {
          Hdiag(j) += v * v * D(ind)
          tmp += v * tau(ind)
        })
        Grad(j) = -tmp + xjneg_sum(j)


        val Gp = Grad(j) + l1
        val Gn = Grad(j) - l1
        var violation = 0.0
        if (w(j) == 0) {
          if (Gp < 0)
            violation = -Gp
          else if (Gn > 0)
            violation = Gn
          //outer-level shrinking
          else if (Gp > Gmax_old / l && Gn < -Gmax_old / l) {
            active_size -= 1
            swap(index, s, active_size)
            s -= 1
            isContinue = true
          }
        } else if (w(j) > 0)
          violation = Math.abs(Gp)
        else
          violation = Math.abs(Gn)

        if (!isContinue) {
          Gmax_new = Math.max(Gmax_new, violation)
          Gnorm1_new += violation
        }
        s += 1

      }

      if (newton_iter == 0) Gnorm1_init = Gnorm1_new

      if (Gnorm1_new > eps * Gnorm1_init) {

        iter = 0
        QP_Gmax_old = Double.PositiveInfinity
        QP_active_size = active_size


        for (i <- 0 until l) xTd(i) = 0
        // optimize QP over wpd
        var isBreak = false
        while (iter < max_iter && !isBreak) {
          QP_Gmax_new = 0
          QP_Gnorm1_new = 0
          for (j <- 0 until QP_active_size) {
            val i = random.nextInt(QP_active_size - j)
            swap(index, i, j)
          }

          s = 0
          while (s < QP_active_size) {
            var isContinue2 = false
            j = index(s)
            H = Hdiag(j)
            G = Grad(j) + (wpd(j) - w(j)) * nu
            ix(j).foreachNoZero((ind, v) => {
              G += v * D(ind) * xTd(ind)
            })

            val Gp = G + l1
            val Gn = G - l1
            var violation = 0.0
            if (wpd(j) == 0) {
              if (Gp < 0)
                violation = -Gp
              else if (Gn > 0)
                violation = Gn
              //inner-level shrinking
              else if (Gp > QP_Gmax_old / l && Gn < -QP_Gmax_old / l) {
                QP_active_size -= 1
                swap(index, s, QP_active_size)
                s -= 1
                isContinue2 = true
              }
            } else if (wpd(j) > 0)
              violation = Math.abs(Gp);
            else
              violation = Math.abs(Gn);

            if (!isContinue2) {
              QP_Gmax_new = Math.max(QP_Gmax_new, violation)
              QP_Gnorm1_new += violation

              // obtain solution of one-variable problem
              if (Gp < H * wpd(j))
                z = -Gp / H
              else if (Gn > H * wpd(j))
                z = -Gn / H
              else
                z = -wpd(j)
              if (Math.abs(z) >= 1.0e-12) {
                z = Math.min(Math.max(z, -10.0), 10.0)

                wpd(j) += z

                ix(j).foreachNoZero((ind, v) => {
                  xTd(ind) += v * z
                })

              }
            }
            s += 1
          }
          iter += 1

          //          println(s"${QP_Gnorm1_new},${inner_eps},${Gnorm1_init},${QP_active_size}")
          if (QP_Gnorm1_new <= inner_eps * Gnorm1_init) {
            //inner stopping
            if (QP_active_size == active_size)
              isBreak = true
            //active set reactivation
            else {
              QP_active_size = active_size
              QP_Gmax_old = Double.PositiveInfinity
            }
          }
          else {
            QP_Gmax_old = QP_Gmax_new
          }
        }

        if (iter >= max_iter) println("WARNING: reaching max number of inner iterations\n")

        delta = 0
        w_norm_new = 0
        for (j <- 0 until w_size) {
          delta += Grad(j) * (wpd(j) - w(j))
          if (wpd(j) != 0) w_norm_new += Math.abs(wpd(j))
        }
        w_norm_new = l1 * w_norm_new
        delta += (w_norm_new - w_norm)

        negsum_xTd = 0
        for (i <- 0 until l)
          if (y(i) == -1) negsum_xTd += getDataWeight(i) * xTd(i)

        var num_linesearch = 0
        var isBreak2 = false
        while (!isBreak2 && num_linesearch < max_num_linesearch) {
          cond = w_norm_new - w_norm + negsum_xTd - sigma * delta
          for (i <- 0 until l) {
            val exp_xTd = Math.exp(xTd(i))
            exp_wTx_new(i) = exp_wTx(i) * exp_xTd
            cond += getDataWeight(i) * Math.log((1 + exp_wTx_new(i)) / (exp_xTd + exp_wTx_new(i)))
          }
          if (cond <= 0) {
            w_norm = w_norm_new
            for (j <- 0 until w_size)
              w(j) = wpd(j)
            for (i <- 0 until l) {
              exp_wTx(i) = exp_wTx_new(i)
              val tau_tmp = 1 / (1 + exp_wTx(i))
              tau(i) = getDataWeight(i) * tau_tmp
              D(i) = getDataWeight(i) * exp_wTx(i) * tau_tmp * tau_tmp
            }
            isBreak2 = true
          } else {
            w_norm_new = 0
            for (j <- 0 until w_size) {
              wpd(j) = (w(j) + wpd(j)) * 0.5
              if (wpd(j) != 0) w_norm_new += Math.abs(wpd(j))
            }
            delta *= 0.5
            negsum_xTd *= 0.5
            for (i <- 0 until l)
              xTd(i) *= 0.5
          }

          num_linesearch += 1
        }
        // Recompute some info due to too many line search steps
        if (num_linesearch >= max_num_linesearch) {
          for (i <- 0 until l) exp_wTx(i) = 0
          for (i <- 0 until w_size) {
            if (w(i) > 0) {
              ix(i).foreachNoZero((ind, v) => {
                exp_wTx(ind) += w(i) * v
              })
            }
          }
          for (i <- 0 until l)
            exp_wTx(i) = Math.exp(exp_wTx(i))
        }
        if (iter == 1) inner_eps *= 0.25

        newton_iter += 1
        Gmax_old = Gmax_new
        printf("iter %3d  #CD cycles %d\n", newton_iter, iter)

      }
      else {
        outBreak = true
      }
      //else break
    }

    printf("=========================%n")
    printf("optimization finished, #iter = %d\n", newton_iter)
    if (newton_iter >= max_newton_iter) printf("WARNING: reaching max number of iterations\n")

    // calculate objective value

    var v = 0.0
    var nnz = 0
    for (j <- 0 until w_size)
      if (w(j) != 0) {
        v += Math.abs(w(j))
        nnz += 1
      }
    for (j <- 0 until l)
      if (y(j) == 1)
        v += getDataWeight(j) * Math.log(1 + 1 / exp_wTx(j))
      else
        v += getDataWeight(j) * Math.log(1 + exp_wTx(j))
    printf("Objective value = %g\n", v);
    printf("#nonzeros/#features = %d/%d\n", nnz, w_size);
    w
  }

}
