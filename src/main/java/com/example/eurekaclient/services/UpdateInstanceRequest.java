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
public class UpdateInstanceRequest {

    private String serviceName;
    private String newHostName;
    private String newIpAddress;
    private int httpPort;
    private int securePort;
    private boolean sslPreferred;
}
