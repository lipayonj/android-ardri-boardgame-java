package com.thesis.ArdRi.bluetooth.messages;

import java.io.Serializable;

/**
 * Created by jerwinlipayon on 3/6/15.
 */
public class DrawGame implements Serializable {

    private boolean draw = false;
    public DrawType type = DrawType.NONE;

    public DrawGame(DrawType drawType){
        this.type = drawType;
    }

    public boolean isDraw() {
        return draw;
    }
}
