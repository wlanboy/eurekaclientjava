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
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class EurekaClientServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ServiceInstance instance;

    @InjectMocks
    private EurekaClientService service;

    @BeforeEach
    void setup() {
        // WICHTIG: Da @Value in Unit Tests nicht funktioniert, 
        // setzen wir den Wert hier manuell für das Mock-Objekt.
        service.eurekaServerUrl = "http://localhost:8761/eureka/apps/";
    }

    @Test
    void registerInstance_success() {
        // GIVEN
        mockInstanceForRegister();
        // Wir simulieren eine erfolgreiche Antwort vom Server (204 No Content)
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        // WHEN
        boolean result = service.registerInstance(instance);

        // THEN
        assertTrue(result);
        // VERIFIKATION: Sicherstellen, dass restTemplate.postForEntity aufgerufen wurde, 
        // ohne dass eine echte Verbindung aufgebaut wurde.
        verify(restTemplate, times(1)).postForEntity(contains("/TEST"), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void deregisterInstance_success() {
        // GIVEN
        when(instance.getServiceName()).thenReturn("TEST");
        when(instance.getHostName()).thenReturn("host");
        when(instance.isSslPreferred()).thenReturn(false);
        when(instance.getHttpPort()).thenReturn(8080);

        // WHEN
        service.deregisterInstance(instance);

        // THEN
        // Wir verifizieren, dass die DELETE Methode des Mocks aufgerufen wurde
        verify(restTemplate, times(1)).delete(contains("TEST/host:TEST:8080"));
    }

    @Test
    void sendHeartbeat_success() {
        // GIVEN
        when(instance.getServiceName()).thenReturn("TEST");
        when(instance.getHostName()).thenReturn("host");
        when(instance.isSslPreferred()).thenReturn(false);
        when(instance.getHttpPort()).thenReturn(8080);

        // WHEN
        boolean result = service.sendHeartbeat(instance);

        // THEN
        assertTrue(result);
        // Verifikation für PUT (Heartbeat)
        verify(restTemplate, times(1)).put(anyString(), isNull());
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