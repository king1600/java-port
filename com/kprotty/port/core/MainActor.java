package com.kprotty.port.core;

public abstract class MainActor extends Actor {

    @Behaviour
    public abstract void start(final String[] args);

}
