package com.mzhang.pokemoniv.model;

/**
 * Created by ming.zhang on 8/24/16.
 */
public class PokeIV {
    private int attack;
    private int defence;
    private int stamina;
    private float perfection;

    public int getAttack() {
        return attack;
    }

    public void setAttack(int attack) {
        this.attack = attack;
    }

    public int getDefence() {
        return defence;
    }

    public void setDefence(int defence) {
        this.defence = defence;
    }

    public int getStamina() {
        return stamina;
    }

    public void setStamina(int stamina) {
        this.stamina = stamina;
    }

    public float getPerfection() {
        return perfection;
    }

    public void setPerfection(float perfection) {
        this.perfection = perfection;
    }
}
