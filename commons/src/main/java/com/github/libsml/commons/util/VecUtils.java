package com.github.libsml.commons.util;


import com.google.common.base.Preconditions;

public class VecUtils {

	public static float vec2norminv(float[] x) {
		return 1.f / (float) vec2norm(x);
	}

	public static float vec2norm(float[] x) {
		return (float)Math.sqrt(vecdot(x, x));
	}


	public static float vecdot(float[] x, float[] y) {
		double re = 0;
		for (int i = 0; i < x.length; i++) {
			re += x[i] * y[i];
		}
		return (float)re;
	}


	public static void veccpy(float[] y, float[] x) {
		System.arraycopy(x, 0, y, 0, y.length);
	}


	public static void vecncpy(float[] y, float[] x) {
		for (int i = 0; i < y.length; i++) {
			y[i] = -x[i];
		}
	}


	public static void vecadd(float[] y, float[] x, float c) {
		for (int i = 0; i < y.length; i++) {
			y[i] += c * x[i];
		}
	}

	public static void vecadd(double[] y, float[] x, float c) {
		for (int i = 0; i < y.length; i++) {
			y[i] += c * x[i];
		}
	}

	public static void vecSale( float[] x, float c) {
		if(c==1.){
			return;
		}
		for (int i = 0; i < x.length; i++) {
			x[i] *= c ;
		}
	}

	public static void vecdiff(float[] z, float[] x, float[] y) {
		for (int i = 0; i < z.length; ++i) {
			z[i] = x[i] - y[i];
		}
	}

	public static float euclideanNorm(float vector[]) {

		int n = vector.length;

		if (n < 1) {
			return 0;
		}

		if (n == 1) {
			return Math.abs(vector[0]);
		}

		// this algorithm is (often) more accurate than just summing up the squares and taking the square-root afterwards

		double scale = 0; // scaling factor that is factored out
		double sum = 1; // basic sum of squares from which scale has been factored out
		for (int i = 0; i < n; i++) {
			if (vector[i] != 0) {
				double abs = Math.abs(vector[i]);
				// try to get the best scaling factor
				if (scale < abs) {
					double t = scale / abs;
					sum = 1 + sum * (t * t);
					scale = abs;
				} else {
					double t = abs / scale;
					sum += t * t;
				}
			}
		}

		return (float)(scale * Math.sqrt(sum));
	}

	public static float owlqnL1Norm(float[] x, int start, int end) {
		double norm = 0f;
		// for (int i = start; i < end; i++) {
		// if (x.containsKey(i)) {
		// norm += Math.abs(x.get(i));
		// }
		// }

		for (int i = start; i < end; ++i) {
			norm += Math.abs(x[i]);
		}
		return (float)norm;
	}

	public static void owlqnPseudoGradient(float[] pg, float[] x, float[] g,
											int n, float c, int start, int end) {
		int i;

		/* Compute the negative of gradients. */
		for (i = 0; i < start; ++i) {
			pg[i] = g[i];
		}

		/* Compute the psuedo-gradients. */
		for (i = start; i < end; ++i) {
			if (x[i] < 0.) {
				/* Differentiable. */
				pg[i] = g[i] - c;
			} else if (0. < x[i]) {
				/* Differentiable. */
				pg[i] = g[i] + c;
			} else {
				if (g[i] < -c) {
					/* Take the right partial derivative. */
					pg[i] = g[i] + c;
				} else if (c < g[i]) {
					/* Take the left partial derivative. */
					pg[i] = g[i] - c;
				} else {
					pg[i] = 0.f;
				}
			}
		}

		for (i = end; i < n; ++i) {
			pg[i] = g[i];
		}
	}

	public static void arrayCopy(double[] src,float[] des){
		Preconditions.checkArgument(src.length==des.length,"Array copy exception:%d!=%d",src.length,des.length);
		for(int i=0;i<src.length;i++){
			des[i]=(float)src[i];
		}
	}


}
