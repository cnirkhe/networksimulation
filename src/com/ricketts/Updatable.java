package com.ricketts;

/**
 * Denotes any object which must get updated by the simulation on a periodic basis
 */
public interface Updatable
{
    /**
     * Update by the simulation.
     */
    void update();
}