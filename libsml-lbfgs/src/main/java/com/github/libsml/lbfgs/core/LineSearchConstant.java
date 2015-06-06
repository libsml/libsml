package com.github.libsml.lbfgs.core;

public enum LineSearchConstant {



	/** MoreThuente method proposd by More and Thuente. */
	LBFGS_LINESEARCH_MORETHUENTE,
	LBFGS_LINESEARCH_BACKTRACKING,
	/**
	 * Backtracking method with the Armijo condition. The backtracking method
	 * finds the step length such that it satisfies the sufficient decrease
	 * (Armijo) condition, - f(x + a * d) <= f(x) + ftol * a * g(x)^T d,
	 * 
	 * where x is the current point, d is the current search direction, and a is
	 * the step length.
	 */
	LBFGS_LINESEARCH_BACKTRACKING_ARMIJO,
	/**
	 * Backtracking method with regular Wolfe condition. The backtracking method
	 * finds the step length such that it satisfies both the Armijo condition
	 * (LBFGS_LINESEARCH_BACKTRACKING_ARMIJO) and the curvature condition, - g(x
	 * + a * d)^T d >= wolfe * g(x)^T d,
	 * 
	 * where x is the current point, d is the current search direction, and a is
	 * the step length.
	 */
	LBFGS_LINESEARCH_BACKTRACKING_WOLFE,
	/**
	 * Backtracking method with strong Wolfe condition. The backtracking method
	 * finds the step length such that it satisfies both the Armijo condition
	 * (LBFGS_LINESEARCH_BACKTRACKING_ARMIJO) and the following condition, -
	 * |g(x + a * d)^T d| <= wolfe * |g(x)^T d|,
	 * 
	 * where x is the current point, d is the current search direction, and a is
	 * the step length.
	 */
	LBFGS_LINESEARCH_BACKTRACKING_STRONG_WOLFE;

    public static String MORETHUENTE="morethuente";
    public static String BACKTRACKING="backtracking";
    public static String ARMIJO="armijo";
    public static String WOLFE="wolf";
    public static String STRONG_WOLFE="strong_wolf";

    public static LineSearchConstant parse(String s){

        if (MORETHUENTE.equals(s)){
            return LBFGS_LINESEARCH_MORETHUENTE;
        }else if(BACKTRACKING.equals(s)){
            return LBFGS_LINESEARCH_BACKTRACKING;
        }else if (ARMIJO.equals(s)){
            return LBFGS_LINESEARCH_BACKTRACKING_ARMIJO;
        }else if (WOLFE.equals(s)){
            return LBFGS_LINESEARCH_BACKTRACKING_WOLFE;
        }else if (STRONG_WOLFE.equals(s)){
            return LBFGS_LINESEARCH_BACKTRACKING_STRONG_WOLFE;
        }
        return LBFGS_LINESEARCH_BACKTRACKING;
    }
}
