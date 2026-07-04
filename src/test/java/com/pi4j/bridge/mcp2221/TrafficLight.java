package com.pi4j.bridge.mcp2221;

import com.pi4j.io.OnOffWrite;

public class TrafficLight {
    private final OnOffWrite red;
    private final OnOffWrite yellow;
    private final OnOffWrite green;
    private State state;

    public TrafficLight(OnOffWrite red, OnOffWrite yellow, OnOffWrite green) {
        this.red = red;
        this.yellow = yellow;
        this.green = green;
    }

    public void setState(State state) {
        if (state == this.state) {
            return;
        }
        this.state = state;
        red.setState(state == State.RED || state == State.RED_YELLOW);
        yellow.setState(state == State.YELLOW || state == State.RED_YELLOW);
        green.setState(state == State.GREEN);
    }

    public State getState() {
        return state;
    }


    enum State {
        RED, RED_YELLOW, GREEN, YELLOW, ERROR;
    }
}
