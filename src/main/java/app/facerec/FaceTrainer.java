// src/main/java/app/facerec/FaceTrainer.java
package app.facerec;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;

import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.CV_32SC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

public class FaceTrainer {

    public static void train(String facesRoot, String modelPath) {
        try {
            List<Mat> images = new ArrayList<>();
            List<Integer> labels = new ArrayList<>();

            File root = new File(facesRoot);
            if (!root.exists()) throw new RuntimeException("No existe carpeta: " + facesRoot);

            File[] users = root.listFiles(File::isDirectory);
            if (users == null || users.length == 0)
                throw new RuntimeException("No hay subcarpetas de usuarios dentro de " + facesRoot);

            for (File userDir : users) {
                int userId = Integer.parseInt(userDir.getName());
                File[] imgs = userDir.listFiles((d, n) -> n.toLowerCase().endsWith(".png") && !n.contains("_nodet_"));
                if (imgs == null) continue;

                for (File img : imgs) {
                    Mat m = imread(img.getAbsolutePath(), IMREAD_GRAYSCALE);
                    if (m == null || m.empty()) continue;
                    Mat resized = new Mat();
                    resize(m, resized, new Size(200, 200));
                    images.add(resized);
                    labels.add(userId);
                }
            }

            if (images.isEmpty())
                throw new RuntimeException("No hay imágenes válidas para entrenar");

            MatVector mats = new MatVector(images.size());
            for (int i = 0; i < images.size(); i++) mats.put(i, images.get(i));

            Mat labelsMat = new Mat(images.size(), 1, CV_32SC1);
            IntBuffer labelsBuf = labelsMat.createBuffer();
            for (int i = 0; i < labels.size(); i++) labelsBuf.put(i, labels.get(i));

            LBPHFaceRecognizer lbph = LBPHFaceRecognizer.create();
            lbph.setRadius(1);
            lbph.setNeighbors(8);
            lbph.setGridX(8);
            lbph.setGridY(8);
            lbph.setThreshold(80);

            lbph.train(mats, labelsMat);

            File modelFile = new File(modelPath);
            modelFile.getParentFile().mkdirs();
            lbph.save(modelPath);

            System.out.println("✅ Modelo entrenado y guardado en: " + modelPath +
                    " | Imágenes: " + images.size());
        } catch (Exception e) {
            throw new RuntimeException("Error entrenando LBPH: " + e.getMessage(), e);
        }
    }

    // deja el main si quieres seguir pudiendo ejecutarlo aparte
    public static void main(String[] args) {
        train("data/faces", "data/modelos/lbph_model.xml");
    }
}