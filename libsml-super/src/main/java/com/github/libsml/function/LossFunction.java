package com.github.libsml.function;

public interface LossFunction {

	/**
	 * Callback interface to provide objective function and gradient
	 * evaluations.
	 * 
	 * The lbfgs() function call this function to obtain the values of objective
	 * function and its gradients when needed. A client program must implement
	 * this function to  evaluate the values of the objective function and its
	 * gradients, given current values of variables.
	 * 

	 * @param g
	 *            The gradient vector. The callback function must compute the
	 *            gradient values for the current variables.
	 * @param k
	 *            The iteration number.
	 * @return lbfgsfloatval_t The value of the objective function for the
	 *         current variables.
	 */
	float lossAndGrad(float[] w, float[] g, int k);

	void Hv(float[] w, float[] d, float[] Hs,int k,int cgIter);
}
