package ru.practicum.explorewithme.main.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.practicum.explorewithme.stats.client.StatsClient;
import ru.practicum.explorewithme.stats.dto.EndpointHitDto;

import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class StatsHitAspect {

    private final StatsClient statsClient;

    @Value("${spring.application.name:ewm-main-service}")
    private String appName;

    @Pointcut("@annotation(LogStatsHit)")
    public void methodsToLogHit() {
    }

    @AfterReturning(pointcut = "methodsToLogHit()")
    public void logHit(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("Cannot log hit: HttpServletRequest is not available in the current context for method: {}",
                joinPoint.getSignature().toShortString());
            return;
        }
        HttpServletRequest request = attributes.getRequest();

        String uri = request.getRequestURI();

        String ip;
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) { // StringUtils.hasText проверяет на null, "", "   "
            ip = xRealIp;
            log.debug("StatsHitAspect: Using IP from X-Real-IP header: {}", ip);
        } else {
            ip = request.getRemoteAddr();
            log.debug("StatsHitAspect: X-Real-IP header not found or empty, using remoteAddr: {}", ip);
        }

        LocalDateTime timestamp = LocalDateTime.now();

        log.debug("StatsHitAspect: Logging hit for app='{}', uri='{}', ip='{}'", appName, uri, ip);

        EndpointHitDto hitDto = EndpointHitDto.builder()
            .app(appName)
            .uri(uri)
            .ip(ip)
            .timestamp(timestamp)
            .build();

        try {
            statsClient.saveHit(hitDto);
            log.debug("StatsHitAspect: Hit successfully sent to stats service for URI: {}", uri);
        } catch (Exception e) {
            log.error("StatsHitAspect: Failed to send hit to stats service for URI: {}. Error: {}", uri, e.getMessage());
        }
    }
}