package com.example.eurekaclient.services;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class EurekaClientService {

    private static final Logger log = LoggerFactory.getLogger(EurekaClientService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${EUREKA_SERVER_URL:http://localhost:8761/eureka/apps/}")
    private String eurekaServerUrl;

    private String getEurekaHost() {
        try {
            URI uri = new URI(eurekaServerUrl);
            return uri.getHost();
        } catch (Exception e) {
            return eurekaServerUrl;
        }
    }

    public String generateInstanceId(ServiceInstance instance) {
        int port = instance.isSslPreferred() ? instance.getSecurePort() : instance.getHttpPort();
        return String.format("%s:%s:%d", instance.getHostName(), instance.getServiceName(), port);
    }

    public String generateServiceName(ServiceInstance instance) {
        if (instance.getServiceName() == null) {
            log.warn("ServiceName is null, using UNKNOWN (EurekaHost={})", getEurekaHost());
            return "UNKNOWN";
        }
        return instance.getServiceName().trim().toUpperCase();
    }

    public boolean registerInstance(ServiceInstance instance) {
        String serviceName = generateServiceName(instance);
        String appUrl = eurekaServerUrl + serviceName;

        String xmlPayload = buildXmlPayload(instance);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setAccept(List.of(MediaType.APPLICATION_XML));

        HttpEntity<String> request = new HttpEntity<>(xmlPayload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(appUrl, request, String.class);
            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.info("Successfully registered instance {} at {} (EurekaHost={})",
                        instance.getServiceName(), appUrl, getEurekaHost());
                return true;
            } else {
                log.error("Failed to register instance {}. Status: {} (EurekaHost={})",
                        instance.getServiceName(), response.getStatusCode(), getEurekaHost());
                return false;
            }
        } catch (Exception e) {
            log.error("Error registering instance {} at {} (EurekaHost={}): {}",
                    instance.getServiceName(), appUrl, getEurekaHost(), e.getMessage(), e);
            return false;
        }
    }

    public void deregisterInstance(ServiceInstance instance) {
        String serviceName = generateServiceName(instance);
        String instanceId = generateInstanceId(instance);
        String deregisterUrl = eurekaServerUrl + serviceName + "/" + instanceId;

        try {
            restTemplate.delete(deregisterUrl);
            log.info("Successfully deregistered instance {} ({}) (EurekaHost={})",
                    serviceName, instanceId, getEurekaHost());
        } catch (Exception e) {
            log.error("Error deregistering instance {} ({}) (EurekaHost={}): {}",
                    serviceName, instanceId, getEurekaHost(), e.getMessage(), e);
        }
    }

    public boolean sendHeartbeat(ServiceInstance instance) {
        String serviceName = generateServiceName(instance);
        String instanceId = generateInstanceId(instance);
        String heartbeatUrl = eurekaServerUrl + serviceName + "/" + instanceId;

        try {
            restTemplate.put(heartbeatUrl, null);
            log.info("[Heartbeat] Erfolgreich für {} ({}) (EurekaHost={})",
                    serviceName, instanceId, getEurekaHost());
            return true;
        } catch (HttpClientErrorException.NotFound nf) {
            log.error("[Heartbeat] Instance {} ({}) not found in Eureka (EurekaHost={})",
                    serviceName, instanceId, getEurekaHost(), nf);
            throw nf;
        } catch (Exception e) {
            log.error("[Heartbeat] Fehler für {} ({}): {} (EurekaHost={})",
                    serviceName, instanceId, e.getMessage(), getEurekaHost(), e);
            return false;
        }
    }

    private String buildXmlPayload(ServiceInstance instance) {
        boolean ssl = instance.isSslPreferred();

        String portEnabled = ssl ? "false" : "true";
        String secureEnabled = ssl ? "true" : "false";

        String protocol = ssl ? "https" : "http";
        int port = ssl ? instance.getSecurePort() : instance.getHttpPort();

        return """
                <instance>
                  <instanceId>%s:%s:%d</instanceId>
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
                instance.getHostName(), instance.getServiceName(), port,
                instance.getHostName(), instance.getServiceName(),
                instance.getIpAddr(),
                instance.getServiceName().toLowerCase(), instance.getServiceName().toLowerCase(),
                instance.getStatus(),
                portEnabled, instance.getHttpPort(),
                secureEnabled, instance.getSecurePort(),
                protocol, instance.getHostName(), port,
                protocol, instance.getHostName(), port,
                protocol, instance.getHostName(), port,
                instance.getDataCenterInfoName());
    }
}
