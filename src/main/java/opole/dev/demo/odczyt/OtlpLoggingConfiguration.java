package opole.dev.demo.odczyt;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OtlpLoggingConfiguration {
    OtlpLoggingConfiguration(OpenTelemetry openTelemetry) {
        OpenTelemetryAppender.install(openTelemetry);
    }
}
