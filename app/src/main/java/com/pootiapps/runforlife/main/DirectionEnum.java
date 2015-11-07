package com.pootiapps.runforlife.main;

/**
 * Created by ksingh on 11/7/2015.
 */
public enum DirectionEnum {
    NE(45),SE(135),SW(225),NW(315);

    private int direction;

    private DirectionEnum(int d){
        direction = d;
    }

    public int getDirection(){
        return direction;
    }

    private static DirectionEnum[] vals = values();

    public DirectionEnum next(){
        return vals[(this.ordinal()+1)%vals.length];
    }
}
