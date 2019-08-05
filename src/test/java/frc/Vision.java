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

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        String path = "./src/test/java/frc/assets/RocketTape.jpeg";
        Mat defaultFrame = Imgcodecs.imread(path);

        Mat grayFrame = new Mat();
        Mat blurredGrayFrame = new Mat();
        Mat threshFrame = new Mat();
        Mat cannyFrame = new Mat();
        Mat hierarchy = new Mat();
        Mat rectFrame = new Mat();
        Mat targetsFrame = new Mat();

        List<MatOfPoint> contours = new ArrayList<>();

        Imgproc.cvtColor(defaultFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.blur(grayFrame, blurredGrayFrame, new Size(3, 3));
        Imgproc.threshold(blurredGrayFrame, threshFrame, 240, 255, Imgproc.THRESH_BINARY);
        Imgproc.Canny(threshFrame, cannyFrame, 240, 255);
        Imgproc.findContours(cannyFrame, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        rectFrame = drawRectAroundContours(cannyFrame, contours);
        targetsFrame = getTargets(cannyFrame, contours);

        HighGui.imshow("Initial", defaultFrame);
        HighGui.imshow("Gray Scale", grayFrame);
        HighGui.imshow("Blurred Gray Scale", blurredGrayFrame);
        HighGui.imshow("Threshold", threshFrame);
        HighGui.imshow("Canny", cannyFrame);
        HighGui.imshow("Rectangles", rectFrame);
        HighGui.imshow("Targets", targetsFrame);

        Imgcodecs.imwrite("./src/test/java/frc/output/01_grayScale.jpeg", grayFrame);
        Imgcodecs.imwrite("./src/test/java/frc/output/02_blurredGray.jpeg", blurredGrayFrame);
        Imgcodecs.imwrite("./src/test/java/frc/output/03_threshold.jpeg", threshFrame);
        Imgcodecs.imwrite("./src/test/java/frc/output/04_canny.jpeg", cannyFrame);
        Imgcodecs.imwrite("./src/test/java/frc/output/05_rectangles.jpeg", rectFrame);
        Imgcodecs.imwrite("./src/test/java/frc/output/06_targets.jpeg", targetsFrame);

        HighGui.waitKey();
    }

    public Mat drawRectAroundContours(Mat cannyOutput, List<MatOfPoint> contours) {
        Mat frame = Mat.zeros(cannyOutput.size(), CvType.CV_8UC3);

        MatOfPoint2f[] contoursPoly = new MatOfPoint2f[contours.size()];
        Rect[] boundRect = new Rect[contours.size()];
        Point[] tl_Corners = new Point[contours.size()];

        for (int i = 0; i < contours.size(); i++) {
            contoursPoly[i] = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
            boundRect[i] = Imgproc.boundingRect(new MatOfPoint(contoursPoly[i].toArray()));
        }

        List<MatOfPoint> contoursPolyList = new ArrayList<>(contoursPoly.length);
        for (MatOfPoint2f poly : contoursPoly) {
            contoursPolyList.add(new MatOfPoint(poly.toArray()));
        }

        for (int i = 0; i < contours.size(); i++) {
            tl_Corners[i] = boundRect[i].tl();
        }

        for (int i = 0; i < contours.size(); i++) {
            Scalar green = new Scalar(0, 255, 0);
            Scalar white = new Scalar(255, 255, 255);
            Imgproc.drawContours(frame, contoursPolyList, i, green);
            Imgproc.rectangle(frame, boundRect[i].tl(), boundRect[i].br(), white, 1);

        }

        return frame;
    }

    public Mat getTargets(Mat cannyOutput, List<MatOfPoint> contours) {
        double areaTolerance = 3;
        double MIN_AREA = 10;

        Scalar green = new Scalar(0, 255, 0);
        Scalar white = new Scalar(255, 255, 255);

        Mat frame = Mat.zeros(cannyOutput.size(), CvType.CV_8UC3);

        MatOfPoint2f[] contoursPoly = new MatOfPoint2f[contours.size()];
        Rect[] boundRect = new Rect[contours.size()];
        Point[] tl_Corners = new Point[contours.size()];
        double[] areas = new double[contours.size()];

        List<Rect> potentialTargets = new ArrayList<>(2);

        for (int i = 0; i < contours.size(); i++) {
            contoursPoly[i] = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
            boundRect[i] = Imgproc.boundingRect(new MatOfPoint(contoursPoly[i].toArray()));
        }

        List<MatOfPoint> contoursPolyList = new ArrayList<>(contoursPoly.length);
        for (MatOfPoint2f poly : contoursPoly) {
            contoursPolyList.add(new MatOfPoint(poly.toArray()));
        }

        for (int i = 0; i < contours.size(); i++) {
            tl_Corners[i] = boundRect[i].tl();
            areas[i] = boundRect[i].area();

        }

        for (int i = 0; i < contours.size(); i++) {
            if (potentialTargets.size() == 2)
                break;
            for (int j = 0; j < contours.size(); j++) {
                if (j != i && Math.abs(areas[j] - boundRect[i].area()) <= areaTolerance
                        && boundRect[i].width < boundRect[i].height && boundRect[i].area() > MIN_AREA) {
                    potentialTargets.add(boundRect[i]);
                    Imgproc.drawContours(frame, contoursPolyList, i, green);
                    break;
                }
            }
        }

        Imgproc.rectangle(frame, potentialTargets.get(1).tl(), potentialTargets.get(0).br(), white, 1);

        return frame;
    }

}
