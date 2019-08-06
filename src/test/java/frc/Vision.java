/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * Vision code for robot
 */
public class Vision {
    @Test
    public void visionTest() {
        // Pre-load
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        // Get the default image
        String path = "./src/test/java/frc/assets/RocketTape.jpeg";
        Mat defaultFrame = Imgcodecs.imread(path);
        // Mats (can be thought of as digital frames, contains info about each
        // individual pixel in a frame)
        Mat grayFrame = new Mat();
        Mat blurredGrayFrame = new Mat();
        Mat threshFrame = new Mat();
        Mat cannyFrame = new Mat();
        Mat hierarchy = new Mat();
        Mat rectFrame = new Mat();
        Mat targetsFrame = new Mat();
        // Our contours that we will be using later to find the bounding boxes
        List<MatOfPoint> contours = new ArrayList<>();
        // Image processing
        Imgproc.cvtColor(defaultFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.blur(grayFrame, blurredGrayFrame, new Size(3, 3));
        Imgproc.threshold(blurredGrayFrame, threshFrame, 240, 255, Imgproc.THRESH_BINARY);
        Imgproc.Canny(threshFrame, cannyFrame, 240, 255);
        Imgproc.findContours(cannyFrame, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        rectFrame = drawRectAroundContours(cannyFrame, contours);
        targetsFrame = getTargets(cannyFrame, contours);

        // Visual output when running the test
        HighGui.imshow("Initial", defaultFrame);
        HighGui.imshow("Gray Scale", grayFrame);
        HighGui.imshow("Blurred Gray Scale", blurredGrayFrame);
        HighGui.imshow("Threshold", threshFrame);
        HighGui.imshow("Canny", cannyFrame);
        HighGui.imshow("Rectangles", rectFrame);
        HighGui.imshow("Targets", targetsFrame);

        // Save the visual output into files in the output folder
        Imgcodecs.imwrite("./src/test/java/frc/output/01_grayScale.jpeg", grayFrame);
        Imgcodecs.imwrite("./src/test/java/frc/output/02_blurredGray.jpeg", blurredGrayFrame);
        Imgcodecs.imwrite("./src/test/java/frc/output/03_threshold.jpeg", threshFrame);
        Imgcodecs.imwrite("./src/test/java/frc/output/04_canny.jpeg", cannyFrame);
        Imgcodecs.imwrite("./src/test/java/frc/output/05_rectangles.jpeg", rectFrame);
        Imgcodecs.imwrite("./src/test/java/frc/output/06_targets.jpeg", targetsFrame);
        // Don't finish the test so that we can see the visual output
        HighGui.waitKey();
    }

    /**
     * Draw bounding boxes based on the contours found
     * 
     * @param cannyOutput - The output Mat produced after running Canny()
     * @param contours    - The output contours produced after running Canny()
     * @return A new Mat with the contours and bounding boxes drawn
     */
    public Mat drawRectAroundContours(Mat cannyOutput, List<MatOfPoint> contours) {
        // Our output frame currently set as a black frame
        Mat frame = Mat.zeros(cannyOutput.size(), CvType.CV_8UC3);

        // The contours that we find
        MatOfPoint2f[] contoursPoly = new MatOfPoint2f[contours.size()];
        // The bounding boxes we make
        Rect[] boundRect = new Rect[contours.size()];

        // Iterate through each contour and approximate the bounding boxes
        for (int i = 0; i < contours.size(); i++) {
            contoursPoly[i] = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
            boundRect[i] = Imgproc.boundingRect(new MatOfPoint(contoursPoly[i].toArray()));
        }

        // Store each polygon we were able to create after approximating the bounding
        // box curves
        List<MatOfPoint> contoursPolyList = new ArrayList<>(contoursPoly.length);
        for (MatOfPoint2f poly : contoursPoly) {
            contoursPolyList.add(new MatOfPoint(poly.toArray()));
        }

        // Draw the bounding boxes
        for (int i = 0; i < contours.size(); i++) {
            // Color scalars
            Scalar green = new Scalar(0, 255, 0);
            Scalar white = new Scalar(255, 255, 255);
            // Draw functions
            Imgproc.drawContours(frame, contoursPolyList, i, green);
            Imgproc.rectangle(frame, boundRect[i].tl(), boundRect[i].br(), white, 1);

        }
        // Return the modified frame
        return frame;
    }

    /**
     * Draw bounding boxes after filtering the contours found to show the targets
     * only
     * 
     * @param cannyOutput - The output Mat produced after running Canny()
     * @param contours    - The output contours produced after running Canny()
     * @return A new Mat with the contours and bounding boxes drawn on only the
     *         targets
     */
    public Mat getTargets(Mat cannyOutput, List<MatOfPoint> contours) {
        // The margin of area allowed when filtering the targets
        double areaTolerance = 3;
        // The minimum area for the object to be considered
        double MIN_AREA = 10;
        // Color scalars
        Scalar green = new Scalar(0, 255, 0);
        Scalar white = new Scalar(255, 255, 255);
        // Empty frame
        Mat frame = Mat.zeros(cannyOutput.size(), CvType.CV_8UC3);
        // List of the polygons found
        MatOfPoint2f[] contoursPoly = new MatOfPoint2f[contours.size()];
        // List of the bounding boxes found
        Rect[] boundRect = new Rect[contours.size()];
        // List of the top left corners of the bounding boxes for filtering
        Point[] tl_Corners = new Point[contours.size()];
        // List of the areas of each bounding box for filtering
        double[] areas = new double[contours.size()];
        // Array list of the potential targets found
        List<Rect> potentialTargets = new ArrayList<>(2);

        // Iterate through each contour and approximate the bounding boxes
        for (int i = 0; i < contours.size(); i++) {
            contoursPoly[i] = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
            boundRect[i] = Imgproc.boundingRect(new MatOfPoint(contoursPoly[i].toArray()));
            // Add the filtering info after the bounding box has been created
            tl_Corners[i] = boundRect[i].tl();
            areas[i] = boundRect[i].area();
        }
        // Store each polygon we were able to create after approximating the bounding
        // box curves
        List<MatOfPoint> contoursPolyList = new ArrayList<>(contoursPoly.length);
        for (MatOfPoint2f poly : contoursPoly) {
            contoursPolyList.add(new MatOfPoint(poly.toArray()));
        }

        // Filter algorithm
        for (int i = 0; i < contours.size(); i++) {
            // If we have 2 potential targets then stop running the check
            if (potentialTargets.size() == 2)
                break;
            // Loop through every other bounding box and perform checks relative to itself
            for (int j = 0; j < contours.size(); j++) {
                if (j != i && Math.abs(areas[j] - boundRect[i].area()) <= areaTolerance
                        && boundRect[i].width < boundRect[i].height && boundRect[i].area() > MIN_AREA) {
                    potentialTargets.add(boundRect[i]);
                    Imgproc.drawContours(frame, contoursPolyList, i, green);
                    break;
                }
            }
        }
        // Draw the bounding box encapsulating the targets
        Imgproc.rectangle(frame, potentialTargets.get(1).tl(), potentialTargets.get(0).br(), white, 1);
        // Return the processed frame
        return frame;
    }

}
