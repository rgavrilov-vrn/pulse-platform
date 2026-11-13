package ru.agroeco.pulse.videoserver.adapter.http.dto;

import ru.agroeco.pulse.videoserver.domain.VideoServerHealth;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ServerHealthView(
        UUID videoServerId,
        Instant observedAt,
        String serverName,
        Instant startupAt,
        Boolean serverOk,
        Float cpuUsage,
        Float gpuUsage,
        Boolean dbConnected,
        Boolean dbIsSlow,
        String dbLastError,
        Integer scriptsOk,
        Integer scriptsTotal,
        Integer disksErrorCount,
        Boolean disksIsSlow,
        String disksLastError,
        Float disksStatMainDays,
        Float disksStatSubsDays,
        Integer ipDevicesTotal,
        Integer ipDevicesConnected,
        Integer channelsNetworkTotal,
        Integer channelsNetworkOnline,
        Boolean networkShouldBeConnected,
        Boolean networkReallyConnected,
        String networkLastError,
        Map<String, Object> rawValues,
        Instant updatedAt
) {
    public static ServerHealthView from(VideoServerHealth h) {
        return new ServerHealthView(
                h.getVideoServerId(), h.getObservedAt(), h.getServerName(), h.getStartupAt(),
                h.getServerOk(), h.getCpuUsage(), h.getGpuUsage(),
                h.getDbConnected(), h.getDbIsSlow(), h.getDbLastError(),
                h.getScriptsOk(), h.getScriptsTotal(),
                h.getDisksErrorCount(), h.getDisksIsSlow(), h.getDisksLastError(),
                h.getDisksStatMainDays(), h.getDisksStatSubsDays(),
                h.getIpDevicesTotal(), h.getIpDevicesConnected(),
                h.getChannelsNetworkTotal(), h.getChannelsNetworkOnline(),
                h.getNetworkShouldBeConnected(), h.getNetworkReallyConnected(),
                h.getNetworkLastError(), h.getRawValues(), h.getUpdatedAt());
    }
}