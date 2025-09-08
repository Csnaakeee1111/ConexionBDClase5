// src/main/java/app/facerec/CameraSmokeTest.java
package app.facerec;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import static org.bytedeco.opencv.global.opencv_videoio.CAP_ANY;
import static org.bytedeco.opencv.global.opencv_videoio.CAP_AVFOUNDATION;

public class CameraSmokeTest {
    public static void main(String[] args) throws Exception {
        int[] backendCandidates = new int[]{CAP_AVFOUNDATION, CAP_ANY}; // mac: AVFoundation primero
        int[] indexCandidates   = new int[]{0, 1}; // prueba 0 y 1

        for (int backend : backendCandidates) {
            for (int idx : indexCandidates) {
                System.out.println(">>> Probando cámara idx=" + idx + " backend=" + backend);
                VideoCapture cam = new VideoCapture(idx, backend);
                if (!cam.isOpened()) { System.out.println("No abrió"); continue; }

                Mat frame = new Mat();
                int good = 0;
                long start = System.currentTimeMillis();
                for (int i = 0; i < 60; i++) { // ~2s
                    if (cam.read(frame) && !frame.empty()) good++;
                    Thread.sleep(30);
                }
                cam.release();
                System.out.println("Frames OK: " + good + " en " + (System.currentTimeMillis()-start) + " ms\n");
            }
        }
    }
}