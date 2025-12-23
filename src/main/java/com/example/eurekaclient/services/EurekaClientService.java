package com.example.eurekaclient.services;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class EurekaClientService {

    private static final Logger log = LoggerFactory.getLogger(EurekaClientService.class);

    private final RestTemplate restTemplate;

    @Value("${EUREKA_SERVER_URL:http://localhost:8761/eureka/apps/}")
    String eurekaServerUrl;

    public EurekaClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private String getEurekaHost() {
        try {
            return new URI(eurekaServerUrl).getHost();
        } catch (Exception e) {
            log.warn("Could not parse Eureka URL '{}': {}", eurekaServerUrl, e.getMessage());
            return eurekaServerUrl;
        }
    }

    public String generateInstanceId(ServiceInstance instance) {
        int port = instance.isSslPreferred() ? instance.getSecurePort() : instance.getHttpPort();
        return "%s:%s:%d".formatted(instance.getHostName(), instance.getServiceName(), port);
    }

    public String generateServiceName(ServiceInstance instance) {
        String name = instance.getServiceName();
        if (name == null || name.isBlank()) {
            log.warn("ServiceName is null or blank, using UNKNOWN (EurekaHost={})", getEurekaHost());
            return "UNKNOWN";
        }
        return name.trim().toUpperCase();
    }

    public boolean registerInstance(ServiceInstance instance) {
        String serviceName = generateServiceName(instance);
        String appUrl = eurekaServerUrl + serviceName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setAccept(List.of(MediaType.APPLICATION_XML));

        HttpEntity<String> request = new HttpEntity<>(buildXmlPayload(instance), headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(appUrl, request, String.class);

            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.info("Registered instance {} at {} (EurekaHost={})",
                        serviceName, appUrl, getEurekaHost());
                return true;
            }

            log.error("Failed to register instance {}. Status: {} Body: {} (EurekaHost={})",
                    serviceName, response.getStatusCode(), response.getBody(), getEurekaHost());
            return false;

        } catch (Exception e) {
            log.error("Error registering instance {} at {} (EurekaHost={}): {}",
                    serviceName, appUrl, getEurekaHost(), e.getMessage(), e);
            return false;
        }
    }

    public void deregisterInstance(ServiceInstance instance) {
        String serviceName = generateServiceName(instance);
        String instanceId = generateInstanceId(instance);
        String url = eurekaServerUrl + serviceName + "/" + instanceId;

        try {
            restTemplate.delete(url);
            log.info("Deregistered instance {} ({}) (EurekaHost={})",
                    serviceName, instanceId, getEurekaHost());
        } catch (Exception e) {
            log.error("Error deregistering instance {} ({}) (EurekaHost={}): {}",
                    serviceName, instanceId, getEurekaHost(), e.getMessage(), e);
        }
    }

    public boolean sendHeartbeat(ServiceInstance instance) {
        String serviceName = generateServiceName(instance);
        String instanceId = generateInstanceId(instance);
        String url = eurekaServerUrl + serviceName + "/" + instanceId;

        try {
            restTemplate.put(url, null);
            log.info("[Heartbeat] OK for {} ({}) (EurekaHost={})",
                    serviceName, instanceId, getEurekaHost());
            return true;

        } catch (HttpClientErrorException.NotFound nf) {
            log.error("[Heartbeat] Instance {} ({}) not found (EurekaHost={})",
                    serviceName, instanceId, getEurekaHost(), nf);
            throw nf;

        } catch (Exception e) {
            log.error("[Heartbeat] Error for {} ({}): {} (EurekaHost={})",
                    serviceName, instanceId, e.getMessage(), getEurekaHost(), e);
            return false;
        }
    }

    String buildXmlPayload(ServiceInstance instance) {
        boolean ssl = instance.isSslPreferred();
        String protocol = ssl ? "https" : "http";
        int port = ssl ? instance.getSecurePort() : instance.getHttpPort();

        return """
                <instance>
                  <instanceId>%s</instanceId>
                  <hostName>%s</hostName>
                  <app>%s</app>
                  <ipAddr>%s</ipAddr>
                  <vipAddress>%s</vipAddress>
                  <secureVipAddress>%s</secureVipAddress>
                  <status>%s</status>
                  <port enabled="%s">%d</port>
                  <securePort enabled="%s">%d</securePort>
                  <homePageUrl>%s://%s:%d/</homePageUrl>
                  <statusPageUrl>%s://%s:%d/actuator/info</statusPageUrl>
                  <healthCheckUrl>%s://%s:%d/actuator/health</healthCheckUrl>
                  <dataCenterInfo class="com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo">
                    <name>%s</name>
                  </dataCenterInfo>
                </instance>
                """.formatted(
                generateInstanceId(instance),
                instance.getHostName(),
                instance.getServiceName(),
                instance.getIpAddr(),
                instance.getServiceName().toLowerCase(),
                instance.getServiceName().toLowerCase(),
                instance.getStatus(),
                ssl ? "false" : "true",
                instance.getHttpPort(),
                ssl ? "true" : "false",
                instance.getSecurePort(),
                protocol, instance.getHostName(), port,
                protocol, instance.getHostName(), port,
                protocol, instance.getHostName(), port,
                instance.getDataCenterInfoName());
    }
}
