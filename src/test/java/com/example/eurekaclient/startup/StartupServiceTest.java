package com.example.eurekaclient.startup;

import com.example.eurekaclient.services.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.mockito.Mockito.*;

import java.util.List;

@SpringBootTest
@TestPropertySource("classpath:application-test.properties")
class StartupServiceTest {

    @MockBean
    private LifecycleManager lifecycleManager;

    @MockBean
    private ServiceInstanceStore store;

    @Autowired
    private ApplicationRunner runner;

    @Test
    void startup_startsLifecycleForAllInstances() throws Exception {
        // Arrange
        ServiceInstance instance1 = mock(ServiceInstance.class);
        ServiceInstance instance2 = mock(ServiceInstance.class);

        when(store.getInstances()).thenReturn(List.of(instance1, instance2));

        // Act
        runner.run(null);

        // Assert
        verify(lifecycleManager, times(2)).startLifecycle(any());
    }
}
