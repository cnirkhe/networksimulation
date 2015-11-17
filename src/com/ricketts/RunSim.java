package com.ricketts;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by chinmay on 11/16/15.
 */
public class RunSim
{
    public RunSim() {}

    // I think all we have to update are hosts and flows, the others should always be the
    // same, but easy to fix if I'm wrong.
    // probably want links b/c of dynamic queues or something idk
    // Right now runtimeMillis < 0 means never ending
    public static void run(final ArrayList<Link> links, final ArrayList<Host> hosts, final Integer intervalTimeMillis,
                           final Integer runtimeMillis) {
        final int startMillis = (int) System.currentTimeMillis();
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final Runnable updater = new Runnable() {
            public void run() {
                for (Link l : links) {
                    l.update(intervalTimeMillis, (int) (System.currentTimeMillis() - startMillis));
                }
                for (Host h : hosts) {
                    h.update(intervalTimeMillis, (int) (System.currentTimeMillis() - startMillis));
                }
            }
        };
        final ScheduledFuture<?> updaterHandle =
                scheduler.scheduleAtFixedRate(updater, 0, intervalTimeMillis, TimeUnit.MILLISECONDS);

        // Stop the scheduler at a specified time if one is input.
        if (runtimeMillis >= 0) {
            scheduler.schedule(new Runnable() {
                public void run() {
                    updaterHandle.cancel(true);
                    scheduler.shutdownNow();
                }
            }, runtimeMillis, TimeUnit.MILLISECONDS);
        }
    }
}
