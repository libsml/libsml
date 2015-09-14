package com.github.libsml.optimization.lbfgs


/**
 * Created by huangyu on 15/9/10.
 */
class LBFGSParameter {
  /**
   * The number of corrections to approximate the inverse hessian matrix. The
   * L-BFGS routine stores the computation results of previous m iterations to
   * approximate the inverse hessian matrix of the current iteration. This
   * parameter controls the size of the limited memories (corrections). The
   * default value is 6. Values less than 3 are not recommended. Large values
   * will result in excessive computing time.
   */
  var m: Int = 6
  /**
   * Epsilon for convergence test. This parameter determines the accuracy with
   * which the solution is to be found. A minimization terminates when ||g|| <
   * epsilon * max(1, ||x||), where ||.|| denotes the Euclidean (L2) norm. The
   * default value is 1e-5.
   */
  var epsilon: Double = 1e-5f
  /**
   * Distance for delta-based convergence test. This parameter determines the
   * distance, in iterations, to compute the rate of decrease of the objective
   * function. If the value of this parameter is zero, the library does not
   * perform the delta-based convergence test. The default value is 0.
   */
  var past: Int = 0
  /**
   * Delta for convergence test. This parameter determines the minimum rate of
   * decrease of the objective function. The library stops iterations when the
   * following condition is met: (f' - f) / f < delta, where f' is the
   * objective value of past iterations ago, and f is the objective value of
   * the current iteration. The default value is 0.
   */
  var delta: Double = 0
  /**
   * The maximum number of iterations. The lbfgs() function terminates an
   * optimization process with ::LBFGSERR_MAXIMUMITERATION status code when
   * the iteration count exceedes this parameter. Setting this parameter to
   * zero continues an optimization process until a convergence or error. The
   * default value is 0.
   */
  var maxIterations: Int = 0
  /**
   * Start index for computing L1 norm of the variables. This parameter is
   * valid only for OWL-QN method (i.e., orthantwiseC != 0). This parameter b
   * (0 <= b < N) specifies the index number from which the library computes
   * the L1 norm of the variables x, |x| := |x_{b}| + |x_{b+1}| + ... +
   * |x_{N}| . In other words, variables x_1, ..., x_{b-1} are not used for
   * computing the L1 norm. Setting b (0 < b < N), one can protect variables,
   * x_1, ..., x_{b-1} (e.g., a bias term of logistic regression) from being
   * regularized. The default value is zero.
   */
  var l1Start: Int = 0
  /**
   * End index for computing L1 norm of the variables. This parameter is valid
   * only for OWL-QN method (i.e., orthantwiseC != 0). This parameter e (0 < e
   * <= N) specifies the index number at which the library stops computing the
   * L1 norm of the variables x,
   */
  var l1End: Int = 0

}
