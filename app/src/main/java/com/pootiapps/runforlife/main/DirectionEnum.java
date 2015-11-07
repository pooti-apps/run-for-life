package com.pootiapps.runforlife.main;

/**
 * Created by ksingh on 11/7/2015.
 */
public enum DirectionEnum {
    NE,SE,SW,NW;

    private static DirectionEnum[] vals = values();

    public DirectionEnum next(){
        return vals[(this.ordinal()+1)%vals.length];
    }
}
