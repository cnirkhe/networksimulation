package com.ricketts;

/**
 * Created by chinmay on 11/16/15.
 */
public abstract class Node implements Updatable
{
    public abstract void receivePacket(Packet packet);

    public abstract void update(Integer intervalTime, Integer overallTime);
}