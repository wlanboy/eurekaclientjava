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
@Schema(description = "Request-Body zum Aktualisieren der Verbindungsparameter einer Service-Instance.")
public class UpdateInstanceRequest {

    @Schema(description = "Name der zu aktualisierenden Service-Instance (Pflichtfeld, dient als Identifikation).", example = "payment-service", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serviceName;

    @Schema(description = "Neuer Hostname, unter dem die Instance erreichbar ist.", example = "payment-host-02")
    private String newHostName;

    @Schema(description = "Neue IP-Adresse der Instance.", example = "192.168.1.99")
    private String newIpAddress;

    @Schema(description = "Neuer HTTP-Port der Instance.", example = "8081")
    private int httpPort;

    @Schema(description = "Neuer HTTPS/Secure-Port der Instance (0 = deaktiviert).", example = "8444")
    private int securePort;

    @Schema(description = "Gibt an, ob Clients den Secure-Port bevorzugen sollen.", example = "false")
    private boolean sslPreferred;
}
