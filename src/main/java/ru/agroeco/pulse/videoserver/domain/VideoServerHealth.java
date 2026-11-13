package ru.agroeco.pulse.videoserver.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "server_health", schema = "videoserver")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoServerHealth {

    @Id
    @Column(name = "video_server_id")
    private UUID videoServerId;

    @Column(name = "observed_at", nullable = false) private Instant observedAt;
    @Column(name = "server_name")                  private String serverName;
    @Column(name = "startup_at")                   private Instant startupAt;

    @Column(name = "server_ok")              private Boolean serverOk;
    @Column(name = "cpu_usage")              private Float cpuUsage;
    @Column(name = "gpu_usage")              private Float gpuUsage;
    @Column(name = "db_connected")           private Boolean dbConnected;
    @Column(name = "db_is_slow")             private Boolean dbIsSlow;
    @Column(name = "db_last_error")          private String dbLastError;
    @Column(name = "scripts_ok")             private Integer scriptsOk;
    @Column(name = "scripts_total")          private Integer scriptsTotal;

    @Column(name = "disks_error_count")      private Integer disksErrorCount;
    @Column(name = "disks_is_slow")          private Boolean disksIsSlow;
    @Column(name = "disks_last_error")       private String disksLastError;
    @Column(name = "disks_stat_main_days")   private Float disksStatMainDays;
    @Column(name = "disks_stat_subs_days")   private Float disksStatSubsDays;

    @Column(name = "ip_devices_total")       private Integer ipDevicesTotal;
    @Column(name = "ip_devices_connected")   private Integer ipDevicesConnected;
    @Column(name = "channels_network_total") private Integer channelsNetworkTotal;
    @Column(name = "channels_network_online") private Integer channelsNetworkOnline;

    @Column(name = "network_should_be_connected") private Boolean networkShouldBeConnected;
    @Column(name = "network_really_connected")    private Boolean networkReallyConnected;
    @Column(name = "network_last_error")          private String networkLastError;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_values", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rawValues = new HashMap<>();

    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public static VideoServerHealth forServer(UUID serverId) {
        VideoServerHealth h = new VideoServerHealth();
        h.videoServerId = serverId;
        return h;
    }

    public void apply(Instant observedAt, String serverName, Instant startupAt,
                      Boolean serverOk, Float cpuUsage, Float gpuUsage,
                      Boolean dbConnected, Boolean dbIsSlow, String dbLastError,
                      Integer scriptsOk, Integer scriptsTotal,
                      Integer disksErrorCount, Boolean disksIsSlow, String disksLastError,
                      Float disksStatMainDays, Float disksStatSubsDays,
                      Integer ipDevicesTotal, Integer ipDevicesConnected,
                      Integer channelsNetworkTotal, Integer channelsNetworkOnline,
                      Boolean networkShouldBeConnected, Boolean networkReallyConnected,
                      String networkLastError, Map<String, Object> rawValues) {
        this.observedAt = observedAt;
        this.serverName = serverName;
        this.startupAt = startupAt;
        this.serverOk = serverOk;
        this.cpuUsage = cpuUsage;
        this.gpuUsage = gpuUsage;
        this.dbConnected = dbConnected;
        this.dbIsSlow = dbIsSlow;
        this.dbLastError = dbLastError;
        this.scriptsOk = scriptsOk;
        this.scriptsTotal = scriptsTotal;
        this.disksErrorCount = disksErrorCount;
        this.disksIsSlow = disksIsSlow;
        this.disksLastError = disksLastError;
        this.disksStatMainDays = disksStatMainDays;
        this.disksStatSubsDays = disksStatSubsDays;
        this.ipDevicesTotal = ipDevicesTotal;
        this.ipDevicesConnected = ipDevicesConnected;
        this.channelsNetworkTotal = channelsNetworkTotal;
        this.channelsNetworkOnline = channelsNetworkOnline;
        this.networkShouldBeConnected = networkShouldBeConnected;
        this.networkReallyConnected = networkReallyConnected;
        this.networkLastError = networkLastError;
        this.rawValues = rawValues == null ? new HashMap<>() : new HashMap<>(rawValues);
        this.updatedAt = Instant.now();
    }
}