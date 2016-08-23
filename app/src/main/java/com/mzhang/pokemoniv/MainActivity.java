package com.mzhang.pokemoniv;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import com.mzhang.pokemoniv.constant.Constant;
import com.mzhang.pokemoniv.model.PokeIV;
import com.mzhang.pokemoniv.model.PokeInfo;
import com.mzhang.pokemoniv.util.PrepareTesseract;
import com.mzhang.pokemoniv.calculator.PokeCalculator;
import org.opencv.core.Core;
import java.io.InputStream;

public class MainActivity extends Activity {
    /***
     * Constant parameters
     */
    private Constant constant = Constant.getInstance();

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PrepareTesseract prepareTesseract = new PrepareTesseract();
        prepareTesseract.prepare(this);

        startOCR();
    }

    /**
     * don't run this code in main thread - it stops UI thread. Create AsyncTask instead.
     * http://developer.android.com/intl/ru/reference/android/os/AsyncTask.html
     */
    private void startOCR() {
        try {
            PokeCalculator pokeCalculator = new PokeCalculator(constant.DATA_PATH, constant.lang);
            try {
                InputStream im = getResources().openRawResource(R.raw.pokehist);
                pokeCalculator.initWithHistFile(im);

                InputStream testImage = getResources().openRawResource(R.raw.testimage);
                PokeInfo pokeInfo = pokeCalculator.getPokeInfo(22, testImage);

                System.out.println("Poke level:" + String.valueOf(pokeInfo.getPokeLevel()));
                System.out.println("Poke CP:" + String.valueOf(pokeInfo.getPokeCP()));
                System.out.println("Poke HP:" + String.valueOf(pokeInfo.getPokeHP()));
                System.out.println("Poke index 1:" + String.valueOf(pokeInfo.getPokeIndex().get(0).first));
                System.out.println("Poke index 2:" + String.valueOf(pokeInfo.getPokeIndex().get(1).first));
                System.out.println("Poke index 3:" + String.valueOf(pokeInfo.getPokeIndex().get(2).first));

                PokeIV pokeIV = pokeCalculator.getPokeIV(pokeInfo.getPokeLevel(), pokeInfo.getPokeHP(), pokeInfo.getPokeCP(),
                        pokeInfo.getPokeIndex().get(0).first);

                System.out.println("Poke attack:" + String.valueOf(pokeIV.getAttack()));
                System.out.println("Poke defence:" + String.valueOf(pokeIV.getDefence()));
                System.out.println("Poke stamina:" + String.valueOf(pokeIV.getStamina()));
                System.out.println("Poke perfection:" + String.valueOf(pokeIV.getPerfection()));

            } catch (Exception e) {
                Log.e(constant.TAG, e.getMessage());
            }

        } catch (Exception e) {
            Log.e(constant.TAG, e.getMessage());
        }
    }
}

