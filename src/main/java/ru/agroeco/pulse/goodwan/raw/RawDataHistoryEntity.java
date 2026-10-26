package ru.agroeco.pulse.goodwan.raw;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "raw_data_history", schema = "goodwan")
public class RawDataHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_device_id", nullable = false, length = 64)
    private String externalDeviceId;

    @Column(name = "source_record_id", nullable = false)
    private Long sourceRecordId;

    @Column(name = "endpoint", nullable = false, length = 128)
    private String endpoint;

    @Column(name = "request_parameters", nullable = false, columnDefinition = "jsonb")
    private String requestParameters;

    @Column(name = "response_item", nullable = false, columnDefinition = "jsonb")
    private String responseItem;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RawDataHistoryEntity() {
    }

    private RawDataHistoryEntity(
        String externalDeviceId,
        Long sourceRecordId,
        String endpoint,
        String requestParameters,
        String responseItem,
        Instant requestedAt,
        Instant receivedAt,
        String payloadHash,
        Instant createdAt
    ) {
        this.externalDeviceId = externalDeviceId;
        this.sourceRecordId = sourceRecordId;
        this.endpoint = endpoint;
        this.requestParameters = requestParameters;
        this.responseItem = responseItem;
        this.requestedAt = requestedAt;
        this.receivedAt = receivedAt;
        this.payloadHash = payloadHash;
        this.createdAt = createdAt;
    }

    public static RawDataHistoryEntity create(
        String externalDeviceId,
        Long sourceRecordId,
        String endpoint,
        String requestParameters,
        String responseItem,
        Instant requestedAt,
        Instant receivedAt,
        String payloadHash
    ) {
        return new RawDataHistoryEntity(
            externalDeviceId,
            sourceRecordId,
            endpoint,
            requestParameters,
            responseItem,
            requestedAt,
            receivedAt,
            payloadHash,
            receivedAt
        );
    }

    public Long getId() {
        return id;
    }

    public String getExternalDeviceId() {
        return externalDeviceId;
    }

    public Long getSourceRecordId() {
        return sourceRecordId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getRequestParameters() {
        return requestParameters;
    }

    public String getResponseItem() {
        return responseItem;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
