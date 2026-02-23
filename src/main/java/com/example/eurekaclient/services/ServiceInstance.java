package com.example.eurekaclient.services;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Repräsentiert eine bei Eureka registrierbare oder registrierte Service-Instance.")
public class ServiceInstance {

    @Schema(description = "Interne ID der Instance.", example = "1")
    private Long id;

    @Schema(description = "Eindeutiger Name des Services, unter dem er sich bei Eureka registriert.", example = "payment-service")
    private String serviceName;

    @Schema(description = "Hostname der Instance, den Eureka an Clients kommuniziert.", example = "payment-host-01")
    private String hostName;

    @Schema(description = "HTTP-Port, auf dem der Service erreichbar ist.", example = "8080")
    private int httpPort;

    @Schema(description = "HTTPS/Secure-Port des Services (0 = deaktiviert).", example = "8443")
    private int securePort;

    @Schema(description = "IP-Adresse der Instance.", example = "192.168.1.42")
    private String ipAddr;

    @Schema(description = "Name des Data-Center-Providers (z. B. 'MyOwn' oder 'Amazon').", example = "MyOwn")
    private String dataCenterInfoName;

    @Schema(description = "Aktueller Eureka-Status der Instance (UP, DOWN, STARTING, OUT_OF_SERVICE).", example = "UP")
    private String status;

    @Schema(description = "Gibt an, ob Clients den Secure-Port bevorzugen sollen.", example = "false")
    private boolean sslPreferred;
}
