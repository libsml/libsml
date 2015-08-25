package com.github.libsml.optimization.liblinear

import com.github.libsml.math.linalg.BLAS._
import com.github.libsml.math.linalg.BLAS.euclideanNorm
import com.github.libsml.math.linalg.{BLAS, Vector}
import com.github.libsml.math.function.Function
import com.github.libsml.math.util.VectorUtils
import com.github.libsml.commons.util.MapWrapper._
import com.github.libsml.optimization.{Optimizer, OptimizerResult}

/**
 * Created by huangyu on 15/8/18.
 */

//TODO:Serializable
class Tron(var _weight: Vector, val parameter: LiblinearParameter) extends Optimizer {

  def this(_weight: Vector, parameter: LiblinearParameter, function: Function) = {
    this(_weight, parameter)
    this.function = function
  }

  def this(_weight: Vector, map: Map[String, String], function: Function) = {
    this(_weight, new LiblinearParameter())
    val para = this.parameter
    this.parameter.maxIterations = map.getInt("tron.maxIterations", para.maxIterations)
    this.parameter.epsilon = map.getDouble("tron.epsilon", para.epsilon)
    this.function = function
  }

  def this(_weight: Vector, map: Map[String, String]) = {
    this(_weight, new LiblinearParameter())
    val para = this.parameter
    this.parameter.maxIterations = map.getInt("tron.maxIterations", para.maxIterations)
    this.parameter.epsilon = map.getDouble("tron.epsilon", para.epsilon)
  }

  var function: Function = _

  //  private[this] val log: Logger = LoggerFactory.getLogger(classOf[Tron])

  private[this] var iter = 0
  private[this] var isStop = false

  // Parameters for updating the iterates.
  val eta0: Double = 1e-4f
  val eta1: Double = 0.25f
  val eta2: Double = 0.75f
  // Parameters for updating the trust region size delta.
  val sigma1: Double = 0.25f
  val sigma2: Double = 0.5f
  val sigma3: Double = 4

  var cg_iter: Int = 0
  var delta: Double = 0.0
  var snorm: Double = 0.0
  val one: Double = 1.0

  var alpha: Double = .0
  var fnew: Double = .0
  var prered: Double = .0
  var actred: Double = .0
  var gs: Double = .0

  var fun: Double = 0.0

  override def f: Double = fun

  override def weight: Vector = _weight

  val s = VectorUtils.newVectorAs(_weight)
  val r = VectorUtils.newVectorAs(_weight)
  val w_new = VectorUtils.newVectorAs(_weight)
  val g = VectorUtils.newVectorAs(_weight)
  val g_new = VectorUtils.newVectorAs(_weight)

  var gnorm1: Double = 0.0
  var gnorm: Double = gnorm1
  var xnorm: Double = 0.0


  override def prior(weight: Vector): Tron.this.type = {
    this._weight = weight
    iter = 0
    isStop = false
    this
  }

  override def setFunction(function: Function): Tron.this.type = {
    this.function = function
    iter = 0
    isStop = false
    this
  }

  override def isConvergence(): Boolean = (parameter.maxIterations != 0 && iter > parameter.maxIterations) || isStop

  override def nextIteration(): OptimizerResult = {
    var msg = ""
    if (iter == 0) {
      fun = function.gradient(_weight, g)
      delta = BLAS.euclideanNorm(g)
      gnorm1 = delta;
      gnorm = gnorm1;

      if (gnorm <= parameter.epsilon * gnorm1) isStop = true

      iter = 1

    } else {

      val (cg_iter, cg_msg) = trcg(delta, _weight, g, s, r, iter)
      msg = cg_msg
      snorm = euclideanNorm(s)
      copy(_weight, w_new)

      axpy(one, s, w_new)

      gs = dot(g, s)
      prered = -0.5f * (gs - dot(s, r))

      fnew = function.gradient(w_new, g_new)

      // Compute the actual reduction.
      actred = fun - fnew


      // On the first iteration, adjust the initial step bound.
      //            snorm = euclideanNorm(s);
      if (iter == 1) delta = Math.min(delta, snorm)

      // Compute prediction alpha*snorm of the step.
      if (fnew - fun - gs <= 0) alpha = sigma3
      else alpha = Math.max(sigma1, -0.5 * (gs / (fnew - fun - gs)))

      // Update the trust region bound according to the ratio of actual to
      // predicted reduction.
      if (actred < eta0 * prered) delta = Math.min(Math.max(alpha, sigma1) * snorm, sigma2 * delta)
      else if (actred < eta1 * prered) delta = Math.max(sigma1 * delta, Math.min(alpha * snorm, sigma2 * delta))
      else if (actred < eta2 * prered) delta = Math.max(sigma1 * delta, Math.min(alpha * snorm, sigma3 * delta))
      else {
        delta = Math.max(delta, Math.min(alpha * snorm, sigma3 * delta))
      }

      xnorm = euclideanNorm(_weight)

      msg += "iter %2d act %5.3e pre %5.3e delta %5.3e f %5.3e |g| %5.3e CG %3d\n".format(
        iter, actred, prered, delta, fun, gnorm, cg_iter)

      if (actred > eta0 * prered) {
        iter += 1
        copy(w_new, _weight)
        copy(g_new, g)
        fun = fnew
        gnorm = euclideanNorm(g)
        if (gnorm <= parameter.epsilon * gnorm1) {
          msg += "Convergence\n"
          isStop = true
        }
      }

      if (fun < -1.0e+32) {
        msg += "WARNING: f < -1.0e+32\n"
        isStop = true
      }
      if (Math.abs(actred) <= 0 && prered <= 0) {
        msg += "WARNING: actred and prered <= 0\n"
        isStop = true
      }
      if (Math.abs(actred) <= 1.0e-12 * Math.abs(fun) && Math.abs(prered) <= 1.0e-12 * Math.abs(fun)) {
        msg += "WARNING: actred and prered too small\n"
        isStop = true
      }


    }
    new OptimizerResult(_weight, Some(g), Some(fun), Some(msg))
  }

  private[this] def trcg(delta: Double, w: Vector, g: Vector,
                         s: Vector, r: Vector, iter: Int): (Int, String) = {
    val d = VectorUtils.newVectorAs(w)
    val Hd = VectorUtils.newVectorAs(w)

    var rTr: Double = 0.0
    var rnewTrnew: Double = 0.0
    var cgtol: Double = 0.0

    BLAS.zero(s)
    BLAS.zero(r)
    BLAS.axpy(-1.0, g, r)
    BLAS.copy(r, d)

    cgtol = 0.1f * BLAS.euclideanNorm(g)

    var cg_iter: Int = 0
    rTr = dot(r, r)
    while (true) {
      if (euclideanNorm(r) <= cgtol) return (cg_iter, "")
      cg_iter += 1
      //      BLAS.zero(Hd)
      function.hessianVector(w, d, Hd, cg_iter == 1)

      var alpha = rTr / dot(d, Hd)

      axpy(alpha, d, s)

      if (euclideanNorm(s) > delta) {
        alpha = -alpha
        axpy(alpha, d, s)

        val std = dot(s, d)
        val sts = dot(s, s)
        val dtd = dot(d, d)
        val dsq = delta * delta
        val rad = Math.sqrt(std * std + dtd * (dsq - sts))

        alpha = if (std >= 0) (dsq - sts) / (std + rad) else (rad - std) / dtd
        axpy(alpha, d, s)
        alpha = -alpha
        axpy(alpha, Hd, r)

        return (cg_iter, "cg reaches trust region boundary\n")
      }

      alpha = -alpha
      axpy(alpha, Hd, r)
      rnewTrnew = dot(r, r)
      val beta = rnewTrnew / rTr
      scal(beta, d)
      axpy(one, r, d)
      rTr = rnewTrnew
    }
    (cg_iter, "")
  }
}


