package com.ricketts;

/**
 * Denotes any object which must get updated by the simulation on a periodic basis
 */
public interface Updatable
{
    /**
     * Update by the simulation.
     * @param intervalTime The time step of the simulation
     * @param overallTime Overall simulation time
     */
    void update(Integer intervalTime, Integer overallTime);
}
