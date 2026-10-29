package com.payflow.provider;

/**
 * Deterministic provider behavior scenarios, selectable via request metadata
 * (e.g. a `simulationScenario` field or an X-Simulate-Scenario header) so that
 * tests and the demo Failure Simulation panel can force exact behavior —
 * never Math.random() for anything a test or demo needs to assert on.
 */
public enum SimulationScenario {
    NORMAL,
    PROVIDER_TIMEOUT,
    PROVIDER_FAILURE,
    SLOW_PROVIDER,
    TRANSIENT_FAILURE
}
