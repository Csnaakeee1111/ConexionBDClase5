package app.facerec;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import static javax.swing.text.StyleConstants.Size;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class FaceService {
    private final CascadeClassifier faceDetector;

    public FaceService(String cascadePath) {
        faceDetector = new CascadeClassifier(cascadePath);
        if (faceDetector.empty()) {
            throw new IllegalStateException("No se pudo cargar el cascade: " + cascadePath);
        }
    }

    public RectVector detectFaces(Mat bgrFrame) {
        return detectFaces(bgrFrame, new Size(80, 80));
    }

    public RectVector detectFaces(Mat bgrFrame, Size minSize) {
        Mat gray = new Mat();
        cvtColor(bgrFrame, gray, COLOR_BGR2GRAY);
        equalizeHist(gray, gray);

        RectVector faces = new RectVector();
        // scaleFactor=1.05, minNeighbors=3, minSize configurable
        faceDetector.detectMultiScale(gray, faces, 1.05, 3, 0, minSize, new Size());
        return faces;
    }
}