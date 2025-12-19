package com.example.eurekaclient.services;

public class UpdateInstanceRequest {

    private String serviceName;
    private String newHostName;
    private String newIpAddress;
    private int httpPort;
    private int securePort;
    private boolean sslPreferred;

    // Getter & Setter
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getNewHostName() {
        return newHostName;
    }

    public void setNewHostName(String newHostName) {
        this.newHostName = newHostName;
    }

    public String getNewIpAddress() {
        return newIpAddress;
    }

    public void setNewIpAddress(String newIpAddress) {
        this.newIpAddress = newIpAddress;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public int getSecurePort() {
        return securePort;
    }

    public void setSecurePort(int securePort) {
        this.securePort = securePort;
    }

    public boolean isSslPreferred() {
        return sslPreferred;
    }

    public void setSslPreferred(boolean sslPreferred) {
        this.sslPreferred = sslPreferred;
    }
}
