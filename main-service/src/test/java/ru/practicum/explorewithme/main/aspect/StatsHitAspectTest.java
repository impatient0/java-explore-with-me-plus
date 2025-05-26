package ru.practicum.explorewithme.main.aspect;

import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.practicum.explorewithme.stats.client.StatsClient;
import ru.practicum.explorewithme.stats.dto.EndpointHitDto;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для StatsHitAspect")
class StatsHitAspectTest {

    @Mock
    private StatsClient statsClient;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private org.aspectj.lang.Signature signature;

    @InjectMocks
    private StatsHitAspect statsHitAspect;

    private MockHttpServletRequest mockRequest;

    @Captor
    private ArgumentCaptor<EndpointHitDto> endpointHitDtoCaptor;

    private final String testAppName = "test-app-for-aspect";

    @BeforeEach
    void setUp() {
        try {
            java.lang.reflect.Field appNameField = StatsHitAspect.class.getDeclaredField("appName");
            appNameField.setAccessible(true);
            appNameField.set(statsHitAspect, testAppName);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set appName for testing", e);
        }

        mockRequest = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("logHit должен отправлять EndpointHitDto в StatsClient с корректными данными")
    void logHit_whenRequestAvailable_shouldSendHitToStatsClient() {
        String testUri = "/test/uri";
        String testIp = "123.123.123.123";
        mockRequest.setRequestURI(testUri);
        mockRequest.setRemoteAddr(testIp);

        statsHitAspect.logHit(joinPoint);

        verify(statsClient, times(1)).saveHit(endpointHitDtoCaptor.capture());
        EndpointHitDto capturedDto = endpointHitDtoCaptor.getValue();

        assertNotNull(capturedDto);
        assertEquals(testAppName, capturedDto.getApp());
        assertEquals(testUri, capturedDto.getUri());
        assertEquals(testIp, capturedDto.getIp());
        assertNotNull(capturedDto.getTimestamp());
        assertTrue(capturedDto.getTimestamp().isAfter(LocalDateTime.now().minusSeconds(5)));
        assertTrue(capturedDto.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(5)));
    }

    @Test
    @DisplayName("logHit не должен вызывать StatsClient, если HttpServletRequest недоступен")
    void logHit_whenRequestNotAvailable_shouldNotCallStatsClientAndLogWarning() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("testMethod()");
        RequestContextHolder.resetRequestAttributes();

        statsHitAspect.logHit(joinPoint);

        verifyNoInteractions(statsClient);
    }

    @Test
    @DisplayName("logHit должен обрабатывать исключение от StatsClient и не пробрасывать его дальше")
    void logHit_whenStatsClientThrowsException_shouldCatchAndLogError() {
        String testUri = "/test/uri";
        String testIp = "123.123.123.123";
        mockRequest.setRequestURI(testUri);
        mockRequest.setRemoteAddr(testIp);

        doThrow(new RuntimeException("Stats service unavailable")).when(statsClient).saveHit(any(EndpointHitDto.class));

        assertDoesNotThrow(() -> statsHitAspect.logHit(joinPoint));

        verify(statsClient, times(1)).saveHit(any(EndpointHitDto.class));
    }
}