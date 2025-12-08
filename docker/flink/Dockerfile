FROM flink:1.18-scala_2.12-java17

# Enable Prometheus Metrics Reporter by downloading it from Maven Central
RUN mkdir -p /opt/flink/plugins/metrics-prometheus && \
    wget -O /opt/flink/plugins/metrics-prometheus/flink-metrics-prometheus-1.18.1.jar \
    https://repo1.maven.org/maven2/org/apache/flink/flink-metrics-prometheus/1.18.1/flink-metrics-prometheus-1.18.1.jar

# Verify it's there
RUN ls -l /opt/flink/plugins/metrics-prometheus/
