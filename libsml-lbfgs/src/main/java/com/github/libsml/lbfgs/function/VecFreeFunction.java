package com.github.libsml.lbfgs.function;

public interface VecFreeFunction {

	void vecsDot(float[][] b, int k, float[] x, float[] xp, float[] g,
				 float[] gp, float[] pg);

	void computeDirect(float[] d, float[] theta, int k);
}
