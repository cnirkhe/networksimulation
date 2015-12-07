package com.ricketts;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by chinmay on 11/16/15.
 */
public class RunSim
{
    public RunSim() {}

    private static int startMillis;
    public static int getCurrentTime() {
        return (int) System.currentTimeMillis() - startMillis;
    }

    // I think all we have to update are hosts and flows, the others should always be the
    // same, but easy to fix if I'm wrong.
    // probably want links b/c of dynamic queues or something idk
    // Right now runtimeMillis < 0 means never ending
    public static void run(final ArrayList<Updatable> updatableList,
                           final Integer intervalTimeMillis, final Integer runtimeMillis,
                           final ArrayList<Link> links, final ArrayList<Flow> flows) {
        startMillis = getCurrentTime();
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final Runnable updater = new Runnable() {
            public void run() {
                for(Updatable u : updatableList)
                {
                    u.update(intervalTimeMillis, getCurrentTime() - startMillis);
                }
            }
        };
        final ScheduledFuture<?> updaterHandle =
                scheduler.scheduleAtFixedRate(updater, 0, intervalTimeMillis, TimeUnit.MILLISECONDS);

        // Father statistics and stop the scheduler at a specified time if one is input.
        if (runtimeMillis >= 0) {
            scheduler.schedule(new Runnable() {
                public void run() {
                    // Get flow and link stats
                    for (Flow flow : flows) {
                        flow.generateFlowGraphs();
                    }
                    for (Link link : links) {
                        link.generateLinkGraphs();
                    }
                    System.out.println("generated graphs");
                    updaterHandle.cancel(true);
                    scheduler.shutdownNow();
                }
            }, runtimeMillis, TimeUnit.MILLISECONDS);
        }
    }
}
