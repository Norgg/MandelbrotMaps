package uk.ac.ed.inf.mandelbrotmaps.compute.strategies.old.cpu;

import uk.ac.ed.inf.mandelbrotmaps.compute.strategies.JuliaSeedSettable;

public class JuliaCPUFractalComputeStrategy extends CPUFractalComputeStrategy implements JuliaSeedSettable {
    private double juliaX = 0;
    private double juliaY = 0;

    // Set the "maximum iteration" calculation constants
    // Empirically determined values for Julia sets.
    @Override
    public double getIterationBase() {
        return 1.58D;
    }

    @Override
    public double getIterationConstantFactor() {
        return 6.46D;
    }

    public double getMaxZoomLevel() {
        return -20;
    }

    @Override
    public double[] getJuliaSeed() {
        return new double[]{this.juliaX, this.juliaY};
    }

    @Override
    public void setJuliaSeed(double juliaX, double juliaY) {
        this.juliaX = juliaX;
        this.juliaY = juliaY;
    }

    @Override
    protected int pixelInSet(int xPixel, int yPixel, int maxIterations) {
        boolean inside = true;
        int iterationNr;
        double newx, newy;
        double x, y;

        x = xMin + ((double) xPixel * pixelSize);
        y = yMax - ((double) yPixel * pixelSize);

        for (iterationNr = 0; iterationNr < maxIterations; iterationNr++) {
            // z^2 + c
            newx = (x * x) - (y * y) + juliaX;
            newy = (2 * x * y) + juliaY;

            x = newx;
            y = newy;

            // Well known result: if distance is >2, escapes to infinity...
            if ((x * x + y * y) > 4) {
                inside = false;
                break;
            }
        }

        if (inside)
            return this.cpuColourStrategy.colourInsidePoint();
        else
            return this.cpuColourStrategy.colourOutsidePoint(iterationNr, maxIterations);
    }

    @Override
    public boolean shouldPerformCrudeFirst() {
        return false;
    }
}
