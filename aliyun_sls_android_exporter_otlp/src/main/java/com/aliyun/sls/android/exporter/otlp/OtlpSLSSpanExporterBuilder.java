package com.aliyun.sls.android.exporter.otlp;

/**
 * @author yulong.gyl
 * @date 2023/9/7
 */
public final class OtlpSLSSpanExporterBuilder {
    private String scope;
    private String endpoint;
    private String project;
    private String logstore;
    private String accessKeyId;
    private String accessKeySecret;
    private String accessKeyToken;
    private boolean isPersistentFlush;

    public OtlpSLSSpanExporterBuilder() {
    }

    public OtlpSLSSpanExporterBuilder setScope(String scope) {
        this.scope = scope;
        return this;
    }

    public OtlpSLSSpanExporterBuilder setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public OtlpSLSSpanExporterBuilder setProject(String project) {
        this.project = project;
        return this;
    }

    public OtlpSLSSpanExporterBuilder setLogstore(String logstore) {
        this.logstore = logstore;
        return this;
    }

    public OtlpSLSSpanExporterBuilder setAccessKey(String accessKeyId, String accessKeySecret, String accessKeyToken) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.accessKeyToken = accessKeyToken;
        return this;
    }

    public OtlpSLSSpanExporterBuilder setPersistentFlush(boolean force) {
        this.isPersistentFlush = force;
        return this;
    }

    public OtlpSLSSpanExporter build() {
        return new OtlpSLSSpanExporter(
            scope,
            endpoint,
            project,
            logstore,
            isPersistentFlush,
            accessKeyId,
            accessKeySecret,
            accessKeyToken
        );
    }

}
