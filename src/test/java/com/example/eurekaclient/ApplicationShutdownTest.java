package com.example.eurekaclient;

import com.example.eurekaclient.services.LifecycleManager;
import com.example.eurekaclient.services.ServiceInstanceStore;
import com.example.eurekaclient.services.TestData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class ApplicationShutdownTest {

    @Test
    void shutdown_stopsAllInstances() {
        // Arrange
        ServiceInstanceStore store = mock(ServiceInstanceStore.class);
        LifecycleManager lifecycle = mock(LifecycleManager.class);

        var instance = TestData.instance();
        when(store.getInstances()).thenReturn(List.of(instance));

        Application app = new Application(store, lifecycle);

        // Act
        app.onShutdown();

        // Assert
        verify(store).getInstances();
        verify(lifecycle).stopAll(List.of(instance));
        verifyNoMoreInteractions(lifecycle);
    }
}
