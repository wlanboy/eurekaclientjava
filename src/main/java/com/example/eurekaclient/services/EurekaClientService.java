package com.example.eurekaclient.services;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class EurekaClientService {

    private static final Logger log = LoggerFactory.getLogger(EurekaClientService.class);

    private final RestClient restClient;

    @Value("${EUREKA_SERVER_URL:http://localhost:8761/eureka/apps/}")
    String eurekaServerUrl;

    public EurekaClientService(RestClient restClient) {
        this.restClient = restClient;
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

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(appUrl)
                    .contentType(MediaType.APPLICATION_XML)
                    .accept(MediaType.APPLICATION_XML)
                    .body(buildXmlPayload(instance))
                    .retrieve()
                    .onStatus(status -> !status.equals(HttpStatus.NO_CONTENT), (req, res) -> {
                        log.error("Failed to register instance {}. Status: {} (EurekaHost={})",
                                serviceName, res.getStatusCode(), getEurekaHost());
                    })
                    .toBodilessEntity();

            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.info("Registered instance {} at {} (EurekaHost={})", serviceName, appUrl, getEurekaHost());
                return true;
            }

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
            restClient.delete()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Deregistered instance {} ({}) (EurekaHost={})", serviceName, instanceId, getEurekaHost());
        } catch (RestClientResponseException e) {
            log.error("Error deregistering instance {} ({}) (EurekaHost={}): {} {}",
                    serviceName, instanceId, getEurekaHost(), e.getStatusCode(), e.getMessage(), e);
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
            restClient.put()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[Heartbeat] OK for {} ({}) (EurekaHost={})", serviceName, instanceId, getEurekaHost());
            return true;

        } catch (HttpClientErrorException.NotFound nf) {
            log.error("[Heartbeat] Instance {} ({}) not found (EurekaHost={})", serviceName, instanceId, getEurekaHost(), nf);
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

        String instanceId  = escapeXml(generateInstanceId(instance));
        String hostName    = escapeXml(instance.getHostName());
        String serviceName = escapeXml(instance.getServiceName());
        String serviceNameLower = escapeXml(instance.getServiceName() != null ? instance.getServiceName().toLowerCase() : "");
        String ipAddr      = escapeXml(instance.getIpAddr());
        String status      = escapeXml(instance.getStatus());
        String dataCenterInfoName = escapeXml(instance.getDataCenterInfoName() != null ? instance.getDataCenterInfoName() : "MyOwn");

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
                instanceId,
                hostName,
                serviceName,
                ipAddr,
                serviceNameLower,
                serviceNameLower,
                status,
                ssl ? "false" : "true",
                instance.getHttpPort(),
                ssl ? "true" : "false",
                instance.getSecurePort(),
                protocol, hostName, port,
                protocol, hostName, port,
                protocol, hostName, port,
                dataCenterInfoName);
    }

    private static String escapeXml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
