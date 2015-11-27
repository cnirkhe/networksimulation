package com.ricketts;

/**
 * Denotes any object which must get updated by the simulation on a periodic basis
 */
public interface Updatable
{
    void update(Integer intervalTime, Integer overallTime);
}
