package com.sivayahealth.lims.config;

import io.micrometer.core.instrument.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom LIMS business metrics registered with Micrometer.
 * Exposed at /actuator/prometheus for scraping by Prometheus / Google Cloud Monitoring.
 */
@Configuration
public class MetricsConfig {

    // ── Document lifecycle counters ──────────────────────────────────────

    @Bean
    public Counter documentUploadCounter(MeterRegistry registry) {
        return Counter.builder("lims.documents.uploaded")
                .description("Number of DOCX files uploaded")
                .register(registry);
    }

    @Bean
    public Counter documentPublishedCounter(MeterRegistry registry) {
        return Counter.builder("lims.documents.published")
                .description("Number of document versions published")
                .register(registry);
    }

    @Bean
    public Counter documentRetiredCounter(MeterRegistry registry) {
        return Counter.builder("lims.documents.retired")
                .description("Number of document versions retired")
                .register(registry);
    }

    // ── Sample / test counters ───────────────────────────────────────────

    @Bean
    public Counter sampleRegisteredCounter(MeterRegistry registry) {
        return Counter.builder("lims.samples.registered")
                .description("Number of samples registered")
                .register(registry);
    }

    @Bean
    public Counter oosDetectedCounter(MeterRegistry registry) {
        return Counter.builder("lims.oos.detected")
                .description("Number of OOS/OOT cases detected")
                .register(registry);
    }

    // ── Storage upload timer ─────────────────────────────────────────────

    @Bean
    public Timer gcsUploadTimer(MeterRegistry registry) {
        return Timer.builder("lims.gcs.upload.duration")
                .description("Time taken to upload a document to Google Cloud Storage")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    // ── Active sessions gauge ────────────────────────────────────────────

    @Bean
    public Gauge jwtActiveSessionsGauge(MeterRegistry registry) {
        // Placeholder backed by an AtomicLong — real tracking wired in AuthService
        return Gauge.builder("lims.auth.active_sessions",
                        com.sivayahealth.lims.config.MetricsConfig.ActiveSessionsHolder.COUNT,
                        j -> j.get())
                .description("Estimated number of active JWT sessions")
                .register(registry);
    }

    // ── Instrument calibration overdue gauge ─────────────────────────────

    @Bean
    public Gauge calibrationOverdueGauge(MeterRegistry registry) {
        return Gauge.builder("lims.calibration.overdue",
                        com.sivayahealth.lims.config.MetricsConfig.OverdueCalibrationHolder.COUNT,
                        j -> j.get())
                .description("Number of instruments with overdue calibration")
                .register(registry);
    }

    /** Shared atomic counters updated by services at runtime */
    public static class ActiveSessionsHolder {
        public static final java.util.concurrent.atomic.AtomicLong COUNT =
                new java.util.concurrent.atomic.AtomicLong(0);
    }

    public static class OverdueCalibrationHolder {
        public static final java.util.concurrent.atomic.AtomicLong COUNT =
                new java.util.concurrent.atomic.AtomicLong(0);
    }
}
