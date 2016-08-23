package com.mzhang.pokemoniv.util;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Vector;

/**
 * Created by ming.zhang on 8/19/16.
 */
public class YamlReaderAndWriter {
    // This nested class specifies the expected variables in the file
    // Mat cannot be used directly because it lacks rows and cols variables
    protected static class MatStorage {
        public int rows;
        public int cols;
        public double[] data;

        public int getRows() {
            return rows;
        }

        public void setRows(int rows) {
            this.rows = rows;
        }

        public int getCols() {
            return cols;
        }

        public void setCols(int cols) {
            this.cols = cols;
        }

        // The empty constructor is required by YamlReader
        public MatStorage() {
        }

        public double[] getData() {
            return data;
        }

        public void setData(double[] data) {
            this.data = new double[rows * cols];
            for(int i = 0; i < data.length && i < this.data.length; i++) {
                this.data[i] = data[i];
            }
        }
    }

    // Loading function
    public Vector<Mat> getMatYml(InputStream input) {
        YamlReader reader = new YamlReader(new InputStreamReader(input));
        Vector<Mat> allMats = new Vector<>();

        try {
            while (true) {
                Map map = (Map) reader.read();
                if (map == null) break;

                MatStorage data = new MatStorage();
                data.rows = Integer.parseInt(map.get("rows").toString());
                data.cols = Integer.parseInt(map.get("cols").toString());
                String[] numbers = map.get("data").toString().split("(,|\\]|\\[)");
                data.data = new double[80];
                for (int i = 0, j = 0; i < numbers.length && j < 80; i++) {
                    if (numbers[i].equals("")) continue;
                    data.data[j++] = Double.parseDouble(numbers[i]);
                }

                // Create a new Mat to hold the extracted data
                Mat m = new Mat(data.rows, data.cols, CvType.CV_32FC1);
                m.put(0, 0, data.getData());
                allMats.add(m);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return allMats;
    }

    // Loading function
    public void setMatYml(String path, String matrixName, Mat matrix) {
        try {
            YamlReader reader = new YamlReader(new FileReader(path));

            // Set the tag "opencv-matrix" to process as MatStorage
            // I'm not sure why the tag is parsed as
            // "tag:yaml.org,2002:opencv-matrix"
            // rather than "opencv-matrix", but I determined this value by
            // debugging
            reader.getConfig().setClassTag("MatStorage", MatStorage.class);

            // Read the string
            Map map = (Map) reader.read();

            YamlWriter writer = new YamlWriter(new FileWriter(path));
            writer.getConfig().setClassTag("MatStorage", MatStorage.class);

            MatStorage data = new MatStorage();
            data.rows = matrix.rows();
            data.cols = matrix.cols();
            data.setData(matrix.get(data.rows, data.cols));
            map.put(matrixName, data);

            writer.write(map);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
