package com.mzhang.pokemoniv.model;

import android.util.Pair;

import java.util.Vector;

/**
 * Created by ming.zhang on 8/24/16.
 */
public class PokeInfo {
    private float pokeLevel;
    private int pokeCP;
    private int pokeHP;
    private Vector<Pair<Integer, Double>> pokeIndex;

    public float getPokeLevel() {
        return pokeLevel;
    }

    public void setPokeLevel(float pokeLevel) {
        this.pokeLevel = pokeLevel;
    }

    public int getPokeCP() {
        return pokeCP;
    }

    public void setPokeCP(int pokeCP) {
        this.pokeCP = pokeCP;
    }

    public int getPokeHP() {
        return pokeHP;
    }

    public void setPokeHP(int pokeHP) {
        this.pokeHP = pokeHP;
    }

    public Vector<Pair<Integer, Double>> getPokeIndex() {
        return pokeIndex;
    }

    public void setPokeIndex(Vector<Pair<Integer, Double>> pokeIndex) {
        this.pokeIndex = pokeIndex;
    }
}
