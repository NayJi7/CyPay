package com.cypay.framework.acteur;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Pool dynamique d'acteurs : ajuste le nombre d'instances selon la charge (taille de la mailbox)
 */
public class DynamicActorPool<T> {
    private final List<Acteur<T>> pool = new ArrayList<>();
    private final int minActors;
    private final int maxActors;
    private final int highWatermark;
    private final int lowWatermark;
    private final ActeurFactory<T> factory;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public DynamicActorPool(int minActors, int maxActors, int highWatermark, int lowWatermark, ActeurFactory<T> factory) {
        this.minActors = minActors;
        this.maxActors = maxActors;
        this.highWatermark = highWatermark;
        this.lowWatermark = lowWatermark;
        this.factory = factory;
        for (int i = 0; i < minActors; i++) {
            addActor();
        }
        scheduler.scheduleAtFixedRate(this::adjustPool, 2, 2, TimeUnit.SECONDS);
    }

    public synchronized void envoyer(Message<T> message) {
        // Simple round-robin
        Acteur<T> acteur = pool.get((int) (System.nanoTime() % pool.size()));
        acteur.envoyer(message);
    }

    private synchronized void adjustPool() {
        int totalQueue = pool.stream().mapToInt(a -> a.getMailboxSize()).sum();
        int current = pool.size();
        if (totalQueue > highWatermark && current < maxActors) {
            addActor();
        } else if (totalQueue < lowWatermark && current > minActors) {
            removeActor();
        }
    }

    private void addActor() {
        Acteur<T> acteur = factory.create();
        acteur.demarrer();
        pool.add(acteur);
        System.out.println("[SCALING] Ajout d'un acteur. Pool: " + pool.size());
    }

    private void removeActor() {
        if (!pool.isEmpty()) {
            Acteur<T> acteur = pool.remove(pool.size() - 1);
            acteur.arreter();
            System.out.println("[SCALING] Suppression d'un acteur. Pool: " + pool.size());
        }
    }

    public interface ActeurFactory<T> {
        Acteur<T> create();
    }
}
