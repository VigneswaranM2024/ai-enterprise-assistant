package com.enterprise.assistant.service.dashboard.impl;

import com.enterprise.assistant.dto.response.PlatformDashboardResponse;
import com.enterprise.assistant.repository.document.DocumentChunkRepository;
import com.enterprise.assistant.repository.document.DocumentRepository;
import com.enterprise.assistant.repository.tenant.TenantRepository;
import com.enterprise.assistant.repository.user.UserRepository;
import com.enterprise.assistant.service.dashboard.PlatformDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Implementation of PlatformDashboardService for global platform analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformDashboardServiceImpl implements PlatformDashboardService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "platform-dashboard-summary", key = "'platform:dashboard:summary'", unless = "#result == null", sync = true)
    public PlatformDashboardResponse getPlatformDashboardMetrics() {
        log.info("Computing global platform-wide dashboard metrics");

        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countByStatus("ACTIVE");
        long totalUsers = userRepository.count();
        long totalDocs = documentRepository.count();
        long totalChunks = documentChunkRepository.count();
        long totalBytes = documentRepository.sumAllFileSizeBytes();
        String formattedStorage = formatStorageBytes(totalBytes);

        String systemHealth = "UP";
        String cacheStatus = checkRedisCacheStatus();

        return new PlatformDashboardResponse(
                totalTenants,
                activeTenants,
                totalUsers,
                totalDocs,
                totalChunks,
                totalBytes,
                formattedStorage,
                systemHealth,
                cacheStatus
        );
    }

    private String checkRedisCacheStatus() {
        try {
            if (redisConnectionFactory != null && redisConnectionFactory.getConnection() != null) {
                return "HEALTHY";
            }
        } catch (Exception ex) {
            log.warn("Redis health probe warning: {}", ex.getMessage());
            return "DEGRADED";
        }
        return "DEGRADED";
    }

    public static String formatStorageBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);
        return String.format(Locale.US, "%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
