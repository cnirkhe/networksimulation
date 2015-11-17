package com.ricketts;

/**
 * Created by chinmay on 11/16/15.
 */
public abstract class Node {

    public Node() {}

    public abstract void receivePacket(Packet packet);
}