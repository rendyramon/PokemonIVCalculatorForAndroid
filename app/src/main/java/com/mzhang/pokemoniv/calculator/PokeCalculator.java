package com.mzhang.pokemoniv.calculator;

import android.graphics.Bitmap;
import android.util.Pair;
import com.googlecode.tesseract.android.TessBaseAPI;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfFloat4;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Vector;

import com.mzhang.pokemoniv.constant.Constant;
import com.mzhang.pokemoniv.model.*;
import com.mzhang.pokemoniv.util.YamlReaderAndWriter;

/**
 * Created by ming.zhang on 8/19/16.
 */
public class PokeCalculator {
    private Vector<Mat> all_pokemons = new Vector<>();
    private YamlReaderAndWriter yamlReaderAndWriter = new YamlReaderAndWriter();
    private Constant constantValue = Constant.getInstance();
    final static int get_pokemon_count = 3;
    final TessBaseAPI baseApi = new TessBaseAPI();

    public PokeCalculator(String data_path, String lang) {
        /** Initial sth. */
        baseApi.init(data_path, lang);
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
        baseApi.setVariable(TessBaseAPI.VAR_SAVE_BLOB_CHOICES, TessBaseAPI.VAR_TRUE);
    }

    /**
     *  Calculate the distance of two points.
     *
     *  @param p1 First point
     *  @param p2 Secode point
     *
     *  @return Distance
     */
    static double getLength(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    /**
     *  Calculate the degree between line c-p1 and c-p2
     *
     *  @param p1 Point 1
     *  @param p2 Point 2
     *  @param c  Center point
     *
     *  @return Degree
     */
    static double getDegree(Point p1, Point p2, Point c) {
        double p1_c = getLength(p1, c);
        double p2_c = getLength(p2, c);
        double p1_p2 = getLength(p1, p2);

        return Math.acos((Math.pow(p1_c, 2) + Math.pow(p2_c, 2) - Math.pow(p1_p2, 2))
                / (2 * p1_c * p2_c)) / 3.14159265258979 * 180;
    }

    /**
     *  Calculate pokemon level
     *
     *  @param player_level player level
     *  @param degree      degree of pokemon level arc
     *
     *  @return pokemon level
     */
    float getPokemonLevel(int player_level, double degree) {
        float min_value = Float.MAX_VALUE;
        int min_index = 0;

        for (int i = 0; i < player_level * 2 + 2; i++) {
            float degree_at_i = (constantValue.CpM[i]-0.094f)*202.037116f/constantValue.CpM[player_level*2-2];
            if (Math.pow(degree - degree_at_i, 2) < min_value) {
                min_value = (float) Math.pow(degree - degree_at_i, 2);
                min_index = i;
            }
        }

        return (min_index + 1) / 2.0f;
    }

    /**
     *  Extract cp number from image
     *
     *  @param image Pokemon image
     *
     *  @return CP number
     */
    long getCpNumber(Mat image) {
        final float top_to_cp_rate = 0.05f;
        final float left_to_cp_rate = 0.3f;
        final float cp_height_rate = 0.06f;
        final int image_width = image.width();
        final int image_height = image.height();

        float left = left_to_cp_rate * image_width;
        float up = top_to_cp_rate * image_height;
        float width = (1 - left_to_cp_rate * 2) * image_width;
        float height = cp_height_rate * image_height;

        Rect roi = new Rect(new Point(left, up), new Point(left + width, up + height));
        image = new Mat(image, roi);

        // Binary thresh
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(gray, gray, 254, 255, Imgproc.THRESH_BINARY);

        // Recognize by using ocr
        Bitmap dst = Bitmap.createBitmap(gray.width(), gray.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(gray, dst);

        baseApi.setImage(dst);
        String recognizedText = baseApi.getUTF8Text().trim();

        // Split string by 'c p C P' and convert the last number string to int.
        String[] split = recognizedText.split("(c|C|p|P)");
        return Long.parseLong(split[split.length - 1]);
    }

    /**
     *  Extract hp number from image
     *
     *  @param image
     *
     *  @return HP number
     */
    long getHpNumber(Mat image) {
        float top_to_cp_rate = 0.525f;
        float left_to_cp_rate = 0.33f;
        float cp_height_rate = 0.03f;
        int image_width = image.width();
        int image_height = image.height();

        float left = left_to_cp_rate * image_width;
        float up = top_to_cp_rate * image_height;
        float width = (1 - left_to_cp_rate * 2) * image_width;
        float height = cp_height_rate * image_height;

        Rect roi = new Rect(new Point(left, up), new Point(left + width, up + height));
        image = new Mat(image, roi);

        // Binary thresh
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

        // Recognize by using ocr
        Bitmap dst = Bitmap.createBitmap(gray.width(), gray.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(gray, dst);

        baseApi.setImage(dst);
        String recognizedText = baseApi.getUTF8Text().trim();

        // Split string by 'c p C P' and convert the last number string to int.
        String[] split = recognizedText.split("(HP|/)");
        return Long.parseLong(split[split.length - 1]);
    }

    /**
     *  Calculate color histogram
     *
     *  @param image image
     *
     *  @return Color histogram
     */
    Mat calculateHist(Mat image) {
        float left = 0.4f * image.width();
        float up = 0.25f * image.height();
        float width = 0.2f * image.width();
        float height = 0.15f *  image.height();

        Rect roi = new Rect(new Point(left, up), new Point(left + width, up + height));
        image = new Mat(image, roi);

        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

        ArrayList<Mat> images = new ArrayList<>();
        images.add(gray);

        Mat hist = new Mat();
        Imgproc.calcHist(images, new MatOfInt(0),  new Mat(), hist, new MatOfInt(30), new MatOfFloat(0, 180));

        return hist;
    }

    /**
     *  Load all histograms from file
     *
     *  @param path Configure file path
     *
     *  @return All histograms
     */
    Vector<Mat> loadAllHistgramFromFile(InputStream input) {
        return yamlReaderAndWriter.getMatYml(input);
    }

    /**
     Load all histogram from configure file
     - parameter histFile: Configure file
     - returns:
     */
    public void initWithHistFile(InputStream histFile) {
        all_pokemons = loadAllHistgramFromFile(histFile);
    }

    /**
     *  Get the most likely pokemons top 3.
     *
     *  @param image Input image
     *
     *  @return Top 3 pokemons.
     */
    Vector<Pair<Integer, Double>> getPokeName(Mat image) {
        image = calculateHist(image);
        Vector<Pair<Integer, Double>> result = new Vector<>();

        for(int i = 0; i < all_pokemons.size(); i++) {
            double compared = Imgproc.compareHist(image, all_pokemons.get(i), 1);
            if (result.size() <= get_pokemon_count) {
                result.add(new Pair<>(i, compared));
            } else {
                for (int j = 0; j < result.size(); j++) {
                    if (result.get(j).second > compared) {
                        result.remove(j);
                        result.add(new Pair<>(i, compared));
                        break;
                    }
                }
            }
        }

        return result;
    }

    /***
     *
     * @param stream
     * @return
     * @throws IOException
     */
    private static byte[] readStream(InputStream stream) throws IOException {
        // Copy content of the image to byte-array
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        byte[] temporaryImageInMemory = buffer.toByteArray();
        buffer.close();
        stream.close();
        return temporaryImageInMemory;
    }

    /**
     *  Get pokemon information include(pokemon level, cp, hp and pokemon number)
     *
     *  @param player_level Player level
     *  @param path         Input image path
     *
     *  @return Pokemon information
     */
    public PokeInfo getPokeInfo(int player_level, InputStream inputStream) {
        PokeInfo emptyInfo = new PokeInfo();
        emptyInfo.setPokeCP(-1);
        emptyInfo.setPokeHP(-1);
        emptyInfo.setPokeIndex(null);
        emptyInfo.setPokeLevel(-1.0f);

        Mat im;

        try {
            byte[] temporaryImageInMemory = readStream(inputStream);
            im = Imgcodecs.imdecode(new MatOfByte(temporaryImageInMemory), Imgcodecs.IMREAD_COLOR);
        } catch (IOException e) {
            return null;
        }

        final float arc_center_x_ratio = 0.5f;
        final float arc_center_y_ratio = 0.355f;
        final float top_rate = 0.11f;
        final float top_height_rate = 0.25f;

        if (im.empty()) return emptyInfo;

        Mat im_top = new Mat(im, new Rect(0, (int)(im.size().height * top_rate), (int)im.size().width,
                (int)(im.size().height * top_height_rate)));
        int centerX = (int)(im.size().width * arc_center_x_ratio);
        int centerY = (int)(im.size().height * arc_center_y_ratio);

        Mat im_top_gray = new Mat();
        Vector<MatOfPoint2f> contours = new Vector<>();
        Vector<MatOfPoint> _contours = new Vector<>();
        Mat hierarchy = new Mat();
        int thresh = 100;

        /// Convert it to gray
        Imgproc.cvtColor(im_top, im_top_gray, Imgproc.COLOR_BGR2GRAY);

        /// Reduce the noise so we avoid false circle detection
        //GaussianBlur(im_top_gray, im_top_gray, cv::Size(3, 3), 2, 2);
        Imgproc.threshold(im_top_gray, im_top_gray, 254, 255, Imgproc.THRESH_BINARY);

        Vector<MatOfFloat4> circles = new Vector<>();

        /// Apply the Hough Transform to find the circles
        Imgproc.Canny(im_top_gray, im_top_gray, thresh, thresh * 2);

        /// Find contours
        Imgproc.findContours(im_top_gray, _contours, hierarchy, Imgproc.RETR_TREE,
                Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        for (int i = 0; i < _contours.size(); i++)
            contours.add(new MatOfPoint2f(_contours.get(i).toArray()));

        /// Approximate contours to polygons + get bounding rects and circles
        Vector<Rect> bound_rect = new Vector<>();
        bound_rect.setSize(contours.size());

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint2f contours_poly = new MatOfPoint2f();
            Imgproc.approxPolyDP(contours.get(i), contours_poly, 3, true);
            bound_rect.set(i, Imgproc.boundingRect(new MatOfPoint(contours_poly.toArray())));
        }

        /// Draw polygonal contour + bonding rects + circles
        int _thresh = 100;
        Point max_x = new Point(0, 0);
        for (int i = 0; i < contours.size(); i++) {
            if (bound_rect.get(i).area() > _thresh
                    && max_x.x < bound_rect.get(i).x + bound_rect.get(i).width) {
                int _y = (int)(bound_rect.get(i).y + im.size().height * top_rate);
                if (bound_rect.get(i).x + bound_rect.get(i).width >= centerX)
                    _y = (int)(bound_rect.get(i).y + bound_rect.get(i).height + im.size().height * top_rate);
                max_x = new Point(bound_rect.get(i).x + bound_rect.get(i).width, _y);
            }
        }

        PokeInfo pokeInfo = new PokeInfo();
        pokeInfo.setPokeLevel(getPokemonLevel(player_level, getDegree(new Point(0, centerY), max_x, new Point(centerX, centerY))));
        pokeInfo.setPokeCP((int)getCpNumber(im));
        pokeInfo.setPokeHP((int)getHpNumber(im));
        pokeInfo.setPokeIndex(getPokeName(im));

        return pokeInfo;
    }

    /**
     *  Calculate pokemon iv
     *
     *  @param poke_level pokemon level
     *  @param poke_hp    pokemon hp
     *  @param poke_cp    pokemon cp
     *  @param poke_index pokemon index
     *
     *  @return pokemon iv
     */
    public PokeIV getPokeIV(float poke_level, int poke_hp, int poke_cp, int poke_index) {
        PokeIV empty_poke_iv = new PokeIV();
        empty_poke_iv.setAttack(-1);
        empty_poke_iv.setDefence(-1);
        empty_poke_iv.setPerfection(-1.0f);
        empty_poke_iv.setStamina(-1);

        if (poke_level * 2 > 80 || poke_level < 0) return empty_poke_iv;
        int base_attack = constantValue.BASE_STATUS[poke_index][0];
        int base_defence = constantValue.BASE_STATUS[poke_index][1];
        int base_stamina = constantValue.BASE_STATUS[poke_index][2];
        float CPM = constantValue.CpM[(int)(poke_level * 2 - 2)];
        int stamina_iv = 0;

        for (int i = 15; i >= 0; i--) {
            if ((int)((i + base_stamina) * CPM) == poke_hp) {
                stamina_iv = i;
                break;
            }
        }

        for (int i = 15; i >= 0; i--) {
            for (int j = 15; j >= 0; j--) {
                int CP = (int)((base_attack + i)* Math.pow(base_defence + j, 0.5)* Math.pow(base_stamina + stamina_iv, 0.5)* Math.pow(CPM, 2)/10);
                if (CP == poke_cp) {
                    PokeIV poke_iv = new PokeIV();
                    poke_iv.setAttack(i);
                    poke_iv.setDefence(j);
                    poke_iv.setPerfection((float)(i + j + stamina_iv)/45.0f);
                    poke_iv.setStamina(stamina_iv);

                    return poke_iv;
                }
            }
        }

        return empty_poke_iv;
    }
}

