package com.thesis.ArdRi.bluetooth.messages;

import com.thesis.ArdRi.models.State;

import java.io.Serializable;

/**
 * Created by jerwinlipayon on 2/27/15.
 */
public class ServerSelectPiece implements Serializable {
    public State piece = State.EMPTY;

    public ServerSelectPiece(State state){
        this.piece = state;
    }
}
