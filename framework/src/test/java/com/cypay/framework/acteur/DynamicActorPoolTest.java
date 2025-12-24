package com.cypay.framework.acteur;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

class DynamicActorPoolTest {
    @Test
    void poolScaleUpAndDown() throws InterruptedException {
        AtomicInteger created = new AtomicInteger(0);
        DynamicActorPool<String> pool = new DynamicActorPool<>(
                1, 3, 2, 0,
                () -> {
                    created.incrementAndGet();
                    return new DummyActeur();
                }
        );
        // Envoyer plusieurs messages pour déclencher le scaling
        for (int i = 0; i < 10; i++) {
            pool.envoyer(new Message<>("test", "msg" + i));
        }
        Thread.sleep(3000); // Laisser le scheduler agir
        assertTrue(created.get() > 1, "Le pool doit avoir créé plus d'un acteur");
    }

    static class DummyActeur extends Acteur<String> {
        DummyActeur() { super("Dummy", false, null, null, null); }
        @Override protected void traiterMessage(String message) {}
    }
}
