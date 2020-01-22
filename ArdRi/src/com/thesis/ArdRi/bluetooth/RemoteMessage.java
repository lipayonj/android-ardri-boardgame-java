package com.thesis.ArdRi.bluetooth;

import java.io.Serializable;

/**
 * Created by jerwinlipayon on 2/12/15.
 */
public class RemoteMessage implements Serializable {

    private MessageType type = null;
    private String message = null;

    public void setMessage(String message) {
        this.message = message;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public MessageType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
