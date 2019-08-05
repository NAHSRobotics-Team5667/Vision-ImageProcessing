# Image Processing with Vision Tracking

A simple vision tracking example using a sample image from the internet and OpenCV.
The vision tracking code can be found [here](src/test/java/frc/Vision.java).

We start the image processing by converting the image into gray scale.

![gray scale image](src/test/java/frc/output/01_grayScale.jpeg)

We then blur the image so that we can smooth out our image.

![blur](src/test/java/frc/output/02_blurredGray.jpeg)

Next we use a technique called thresholding to "filter" any values we don't want.

![thresholding](src/test/java/frc/output/03_threshold.jpeg)

We then use an algorithm called canny, which finds the edges of each item left after the threshold and returns the contours of each object.

![canny](src/test/java/frc/output/04_canny.jpeg)

We can now draw bounding boxes on each object we were able to find.

![bounding boxes](src/test/java/frc/output/05_rectangles.jpeg)

Finally, we perform some checks on each object to filter the noise and leave only the actual targets.

![targets](src/test/java/frc/output/06_targets.jpeg)
