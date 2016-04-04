package core.math

import java.util.*

/**
 * Interpolates values between the given data points using a [SplineFunction].
 *
 * Implementation uses the Natural Cubic Spline algorithm as described in
 * R. L. Burden and J. D. Faires (2011), *Numerical Analysis*. 9th ed. Boston, MA: Brooks/Cole, Cengage Learning. p149-150.
 */
class CubicSplineInterpolator(val xs: DoubleArray, val ys: DoubleArray) {
    init {
        require(xs.size == ys.size) { "x and y dimensions should match: ${xs.size} != ${ys.size}" }
        require(xs.size >= 3) { "At least 3 data points are required for interpolation, received: ${xs.size}" }
    }

    private val splineFunction by lazy { computeSplineFunction() }

    fun interpolate(x: Double): Double {
        require(x >= xs.first() && x <= xs.last()) { "Can't interpolate below ${xs.first()} or above ${xs.last()}" }
        return splineFunction.getValue(x)
    }

    private fun computeSplineFunction(): SplineFunction {
        val n = xs.size - 1

        // Coefficients of polynomial
        val b = DoubleArray(n) // linear
        val c = DoubleArray(n + 1) // quadratic
        val d = DoubleArray(n) // cubic

        // Helpers
        val h = DoubleArray(n)
        val g = DoubleArray(n)

        for (i in 0..n - 1)
            h[i] = xs[i + 1] - xs[i]
        for (i in 1..n - 1)
            g[i] = 3 / h[i] * (ys[i + 1] - ys[i]) - 3 / h[i - 1] * (ys[i] - ys[i - 1])

        // Solve tridiagonal linear system (using Crout Factorization)
        val m = DoubleArray(n)
        val z = DoubleArray(n)
        for (i in 1..n - 1) {
            val l = 2 * (xs[i + 1] - xs[i - 1]) - h[i - 1] * m[i - 1]
            m[i] = h[i]/l
            z[i] = (g[i] - h[i - 1] * z[i - 1]) / l
        }
        for (j in n - 1 downTo 0) {
            c[j] = z[j] - m[j] * c[j + 1]
            b[j] = (ys[j + 1] - ys[j]) / h[j] - h[j] * (c[j + 1] + 2.0 * c[j]) / 3.0
            d[j] = (c[j + 1] - c[j]) / (3.0 * h[j])
        }

        val segmentMap = TreeMap<Double, Polynomial>()
        for (i in 0..n - 1) {
            val coefficients = doubleArrayOf(ys[i], b[i], c[i], d[i])
            segmentMap.put(xs[i], Polynomial(coefficients))
        }
        return SplineFunction(segmentMap)
    }
}

class Polynomial(private val coefficients: DoubleArray) {
    fun getValue(x: Double) = coefficients.reversed().fold(0.0, { result, c -> result * x + c })
}

/**
 * A *spline* is function piecewise-defined by polynomial functions.
 * Points at which polynomial pieces connect are known as *knots*.
 *
 * @param segmentMap a mapping between a knot and the polynomial that covers the subsequent interval
 */
class SplineFunction(val segmentMap: TreeMap<Double, Polynomial>) {
    fun getValue(x: Double): Double {
        val (knot, polynomial) = segmentMap.floorEntry(x)
        return polynomial.getValue(x - knot)
    }
}
