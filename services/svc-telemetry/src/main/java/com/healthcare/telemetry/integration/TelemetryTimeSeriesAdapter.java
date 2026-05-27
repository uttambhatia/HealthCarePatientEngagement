package com.healthcare.telemetry.integration;

import com.healthcare.telemetry.domain.TelemetryReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelemetryTimeSeriesAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryTimeSeriesAdapter.class);

    public void persistMetric(TelemetryReading aggregate, String correlationId) {
        LOGGER.info("External integration invoked aggregateId={} correlationId={}", aggregate.id(), correlationId);
    }
}
