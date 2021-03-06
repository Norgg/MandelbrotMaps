package uk.ac.ed.inf.mandelbrotmaps.compute.strategies.old.cpu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import uk.ac.ed.inf.mandelbrotmaps.colouring.old.DefaultColourStrategy;
import uk.ac.ed.inf.mandelbrotmaps.colouring.old.IColourStrategy;
import uk.ac.ed.inf.mandelbrotmaps.compute.FractalComputeArguments;
import uk.ac.ed.inf.mandelbrotmaps.compute.IFractalComputeDelegate;
import uk.ac.ed.inf.mandelbrotmaps.compute.strategies.FractalComputeStrategy;

public abstract class CPUFractalComputeStrategy extends FractalComputeStrategy {
    private final Logger LOGGER = LoggerFactory.getLogger(CPUFractalComputeStrategy.class);

    private ArrayList<LinkedBlockingQueue<FractalComputeArguments>> renderQueueList = new ArrayList<LinkedBlockingQueue<FractalComputeArguments>>();
    private ArrayList<CPURenderThread> renderThreadList;
    private ArrayList<Boolean> rendersComplete;

    protected IColourStrategy cpuColourStrategy;

    private int numberOfThreads = 1;

    @Override
    public void initialise(int width, int height, IFractalComputeDelegate delegate) {
        super.initialise(width, height, delegate);

        this.cpuColourStrategy = new DefaultColourStrategy();

        this.initialiseRenderThreads();
    }

    @Override
    public void tearDown() {
        this.stopAllRendering();
        this.interruptThreads();
    }

    public void initialiseRenderThreads() {
        if (this.renderThreadList != null && !this.renderThreadList.isEmpty()) {
            this.stopAllRendering();
            this.interruptThreads();
        }

        this.renderThreadList = new ArrayList<CPURenderThread>();
        this.rendersComplete = new ArrayList<Boolean>();

        // Create the render threads
        this.numberOfThreads = Runtime.getRuntime().availableProcessors();
//        this.numberOfThreads = 1;
        LOGGER.debug("Using {} cores", this.numberOfThreads);

        for (int i = 0; i < this.numberOfThreads; i++) {
            this.rendersComplete.add(false);
            this.renderQueueList.add(new LinkedBlockingQueue<FractalComputeArguments>());
            this.renderThreadList.add(new CPURenderThread(this, i));
            this.renderThreadList.get(i).start();
        }
    }

    public void interruptThreads() {
        for (CPURenderThread thread : renderThreadList) {
            thread.interrupt();
        }
    }

    void scheduleRendering(FractalComputeArguments arguments) {
        for (int i = 0; i < this.numberOfThreads; i++) {
            renderThreadList.get(i).allowRendering();
            renderQueueList.get(i).add(arguments);
        }
    }

    /* Stop all rendering, including planned and current */
    @Override
    public void stopAllRendering() {
        for (int i = 0; i < this.numberOfThreads; i++) {
            if (!this.renderQueueList.isEmpty())
                this.renderQueueList.get(i).clear();

            if (!this.renderThreadList.isEmpty())
                this.renderThreadList.get(i).abortRendering();

            this.rendersComplete.set(i, false);
        }
    }

    public FractalComputeArguments getNextRendering(int threadID) throws InterruptedException {
        FractalComputeArguments arguments = this.renderQueueList.get(threadID).take();
        this.rendersComplete.set(threadID, false);
        return arguments;
    }

    @Override
    public void computeFractal(FractalComputeArguments arguments) {
        // Package parameters in a structure, add them to the render queue

        // Reset all the tracking
        for (int i = 0; i < this.numberOfThreads; i++) {
            rendersComplete.set(i, false);
        }

        LOGGER.debug("Scheduling render on " + this.numberOfThreads + " threads");
        this.scheduleRendering(arguments);
    }

    public void computeFractalWithThreadID(FractalComputeArguments arguments, int threadID) {
        this.delegate.onComputeStarted(arguments.pixelBlockSize);

        CPURenderThread callingThread = this.renderThreadList.get(threadID);

        int yStart = (arguments.viewHeight / 2) + (threadID * arguments.pixelBlockSize);
        int yEnd = arguments.viewHeight;
        boolean showRenderProgress = (threadID == 0);

        int xPixelMin = 0;
        int xPixelMax = arguments.viewWidth;
        int yPixelMin = yStart;
        int yPixelMax = yEnd;

        int imgWidth = xPixelMax - xPixelMin;

        int xPixel = 0, yPixel = 0, yIncrement = 0;
        int colourCodeHex;
        int pixelBlockA, pixelBlockB;

        this.xMin = arguments.xMin;
        this.yMax = arguments.yMax;
        this.pixelSize = arguments.pixelSize;

        int pixelIncrement = arguments.pixelBlockSize * this.numberOfThreads;
        int originalIncrement = pixelIncrement;

        int loopCount = 0;


        for (yIncrement = yPixelMin; yPixel < yPixelMax + (this.numberOfThreads * arguments.pixelBlockSize); yIncrement += pixelIncrement) {
            yPixel = yIncrement;

            pixelIncrement = (loopCount * originalIncrement);
            if (loopCount % 2 == 0)
                pixelIncrement *= -1;
            loopCount++;

            //If we've exceeded the bounds of the image (as can happen with many threads), exit the loop.
            if (((imgWidth * (yPixel + arguments.pixelBlockSize - 1)) + xPixelMax) > arguments.pixelBufferSizes.length ||
                    yPixel < 0) {
                continue;
            }

//            // Detect rendering abortion.
            if (callingThread.abortSignalled()) {
                return;
            }

            // Set y0 (im part of c)
            //y0 = yMax - ( (double)yPixel * pixelSize );

            for (xPixel = xPixelMin; xPixel < xPixelMax + 1 - arguments.pixelBlockSize; xPixel += arguments.pixelBlockSize) {
                //Check to see if this pixel is already iterated to the necessary block size

                if (arguments.pixelBufferSizes[(imgWidth * yPixel) + xPixel] <= arguments.pixelBlockSize) {
                    continue;
                }

                colourCodeHex = pixelInSet(xPixel, yPixel, arguments.maxIterations);

                //Note that the pixel being calculated has been calculated in full (upper right of a block)
//                if (fractalViewSize == FractalViewSize.LARGE)
                arguments.pixelBufferSizes[(imgWidth * yPixel) + (xPixel)] = arguments.defaultPixelSize;

                // Save colour info for this pixel. int, interpreted: 0xAARRGGBB
                int p = 0;
                for (pixelBlockA = 0; pixelBlockA < arguments.pixelBlockSize; pixelBlockA++) {
                    for (pixelBlockB = 0; pixelBlockB < arguments.pixelBlockSize; pixelBlockB++) {
//                        if (fractalViewSize == FractalViewSize.LARGE) {
                        if (p != 0) {
                            arguments.pixelBufferSizes[imgWidth * (yPixel + pixelBlockB) + (xPixel + pixelBlockA)] = arguments.pixelBlockSize;
                        }
                        p++;
//                        }
                        if (arguments.pixelBuffer == null) return;
                        arguments.pixelBuffer[imgWidth * (yPixel + pixelBlockB) + (xPixel + pixelBlockA)] = colourCodeHex;
                    }
                }
            }
            // Show thread's work in progress
            if (showRenderProgress && (loopCount % arguments.linesPerProgressUpdate == 0) && !callingThread.abortSignalled()) {
                LOGGER.debug("Posting update for thread {}", threadID);
                this.delegate.postUpdate(arguments.pixelBuffer, arguments.pixelBufferSizes);
            }
        }

        rendersComplete.set(threadID, true);

        boolean allComplete = true;

        for (Boolean complete : rendersComplete) {
            if (!complete)
                allComplete = false;
        }

        if (allComplete) {
            double allTime = (System.nanoTime() - arguments.startTime) / 1000000000D;
            this.delegate.postFinished(arguments.pixelBuffer, arguments.pixelBufferSizes, arguments.pixelBlockSize, allTime);

            LOGGER.info("Took {} seconds to do CPU compute", allTime);
        }
    }

    // Abstract methods

    protected abstract int pixelInSet(int xPixel, int yPixel, int maxIterations);
}
