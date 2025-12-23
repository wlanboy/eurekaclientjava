package com.example.eurekaclient.services;

public class TestData {

    public static ServiceInstance instance() {
        return ServiceInstance.builder()
                .id(1L)
                .serviceName("TEST-SERVICE")
                .hostName("localhost")
                .ipAddr("127.0.0.1")
                .httpPort(8080)
                .securePort(8443)
                .sslPreferred(false)
                .status("UP")
                .dataCenterInfoName("MyDC")
                .build();
    }

    public static UpdateInstanceRequest updateRequest() {
        return UpdateInstanceRequest.builder()
                .serviceName("TEST-SERVICE")
                .newHostName("new-host")
                .newIpAddress("10.0.0.5")
                .httpPort(8081)
                .securePort(8444)
                .sslPreferred(true)
                .build();
    }
}
