package com.example.eurekaclient.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceInstanceStoreTest {

    @Test
    void save_assignsId() {
        ServiceInstanceStore store = new ServiceInstanceStore();
        ServiceInstance instance = new ServiceInstance();

        store.save(instance);

        assertNotNull(instance.getId());
    }

    @Test
    void findByServiceName_caseInsensitive() {
        ServiceInstanceStore store = new ServiceInstanceStore();

        ServiceInstance instance = TestData.instance();
        store.save(instance);

        assertNotNull(store.findByServiceName("test-service"));
    }
}
