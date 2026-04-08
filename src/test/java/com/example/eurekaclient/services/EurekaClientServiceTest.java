package com.example.eurekaclient.services;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class EurekaClientServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient restClient;

    @Mock
    private ServiceInstance instance;

    @InjectMocks
    private EurekaClientService service;

    @BeforeEach
    void setup() {
        service.eurekaServerUrl = "http://localhost:8761/eureka/apps/";
    }

    @Test
    void registerInstance_success() {
        // GIVEN
        mockInstanceForRegister();
        when(restClient.post()
                .uri(anyString())
                .contentType(any())
                .accept(any(MediaType.class))
                .body(anyString())
                .retrieve()
                .onStatus(any(), any())
                .toBodilessEntity())
                .thenReturn(ResponseEntity.noContent().build());

        // WHEN
        boolean result = service.registerInstance(instance);

        // THEN
        assertTrue(result);
    }

    @Test
    void registerInstance_failure_returnsNoContent() {
        // GIVEN
        mockInstanceForRegister();
        when(restClient.post()
                .uri(anyString())
                .contentType(any())
                .accept(any(MediaType.class))
                .body(anyString())
                .retrieve()
                .onStatus(any(), any())
                .toBodilessEntity())
                .thenReturn(ResponseEntity.ok().build());

        // WHEN
        boolean result = service.registerInstance(instance);

        // THEN
        assertFalse(result);
    }

    @Test
    void deregisterInstance_success() {
        // GIVEN
        when(instance.getServiceName()).thenReturn("TEST");
        when(instance.getHostName()).thenReturn("host");
        when(instance.isSslPreferred()).thenReturn(false);
        when(instance.getHttpPort()).thenReturn(8080);
        when(restClient.delete()
                .uri(anyString())
                .retrieve()
                .toBodilessEntity())
                .thenReturn(ResponseEntity.noContent().build());

        // WHEN + THEN: kein Exception erwartet
        assertDoesNotThrow(() -> service.deregisterInstance(instance));
    }

    @Test
    void sendHeartbeat_success() {
        // GIVEN
        when(instance.getServiceName()).thenReturn("TEST");
        when(instance.getHostName()).thenReturn("host");
        when(instance.isSslPreferred()).thenReturn(false);
        when(instance.getHttpPort()).thenReturn(8080);
        when(restClient.put()
                .uri(anyString())
                .retrieve()
                .toBodilessEntity())
                .thenReturn(ResponseEntity.noContent().build());

        // WHEN
        boolean result = service.sendHeartbeat(instance);

        // THEN
        assertTrue(result);
    }

    @Test
    void sendHeartbeat_notFound_throwsException() {
        // GIVEN
        when(instance.getServiceName()).thenReturn("TEST");
        when(instance.getHostName()).thenReturn("host");
        when(instance.isSslPreferred()).thenReturn(false);
        when(instance.getHttpPort()).thenReturn(8080);
        when(restClient.put()
                .uri(anyString())
                .retrieve()
                .toBodilessEntity())
                .thenThrow(HttpClientErrorException.NotFound.class);

        // WHEN + THEN
        assertThrows(HttpClientErrorException.NotFound.class, () -> service.sendHeartbeat(instance));
    }

    private void mockInstanceForRegister() {
        when(instance.getServiceName()).thenReturn("TEST");
        when(instance.getHostName()).thenReturn("host");
        when(instance.getIpAddr()).thenReturn("127.0.0.1");
        when(instance.getStatus()).thenReturn("UP");
        when(instance.getDataCenterInfoName()).thenReturn("MyDC");
        when(instance.isSslPreferred()).thenReturn(false);
        when(instance.getHttpPort()).thenReturn(8080);
        when(instance.getSecurePort()).thenReturn(8443);
    }
}
