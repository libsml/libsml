package com.github.libsml.optimization.linear

/**
 * Created by huangyu on 15/8/26.
 */
case class LinerSearchParameter(/**
                                * The minimum step of the line search routine. The default value is \c
                                * 1e-20. This value need not be modified unless the exponents are too large
                                * for the machine being used, or unless the problem is extremely badly
                                * scaled (in which case the exponents should be increased).
                                */
                               var minStep: Float = 1e-20f,

                               /**
                                * The maximum step of the line search. The default value is 1e+20. This
                                * value need not be modified unless the exponents are too large for the
                                * machine being used, or unless the problem is extremely badly scaled (in
                                * which case the exponents should be increased).
                                */
                               var maxStep: Float = 1e+20f,

                               /**
                                * A parameter to control the accuracy of the line search routine. The
                                * default value is 1e-4. This parameter should be greater than zero and
                                * smaller than 0.5.
                                */
                               var ftol: Float = 1e-4f,

                               /**
                                * A coefficient for the Wolfe condition. This parameter is valid only when
                                * the backtracking line-search algorithm is used with the Wolfe condition,
                                * ::LBFGS_LINESEARCH_BACKTRACKING_STRONG_WOLFE or
                                * ::LBFGS_LINESEARCH_BACKTRACKING_WOLFE . The default value is 0.9. This
                                * parameter should be greater the ftol parameter and smaller than 1.0.
                                */
                               var wolfe: Float = 0.9f,

                               /**
                                * A parameter to control the accuracy of the line search routine. The
                                * default value is 0.9. If the function and gradient evaluations are
                                * inexpensive with respect to the cost of the iteration (which is sometimes
                                * the case when solving very large problems) it may be advantageous to set
                                * this parameter to a small value. A typical small value is 0.1. This
                                * parameter shuold be greater than the ftol parameter (1e-4) and smaller
                                * than 1.0.
                                */
                               var gtol: Float = 0.9f,

                               /**
                                * The machine precision for floating-point values. This parameter must be a
                                * positive value set by a client program to estimate the machine precision.
                                * The line search routine will terminate with the status code
                                * (::LBFGSERR_ROUNDING_ERROR) if the relative width of the interval of
                                * uncertainty is less than this parameter.
                                */
                               var xtol: Float = 1.0e-16f,

                               /**
                                * The maximum number of trials for the line search. This parameter controls
                                * the number of function and gradients evaluations per iteration for the
                                * line search routine. The default value is 20.
                                */
                               var maxLinesearch: Int = 40) {

}
