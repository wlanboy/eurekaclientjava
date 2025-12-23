package com.example.eurekaclient.services;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ServiceInstance {

    private Long id;

    private String serviceName;
    private String hostName;
    private int httpPort;
    private int securePort;
    private String ipAddr;
    private String dataCenterInfoName;
    private String status;
    private boolean sslPreferred;
}
