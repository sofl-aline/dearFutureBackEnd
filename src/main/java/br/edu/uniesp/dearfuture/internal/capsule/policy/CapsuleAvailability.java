package br.edu.uniesp.dearfuture.internal.capsule.policy;

import java.time.Instant;
public final class CapsuleAvailability {
    private CapsuleAvailability() {}
    public static boolean isAvailable(Instant opensAt, Instant now) { return !now.isBefore(opensAt); }
}
