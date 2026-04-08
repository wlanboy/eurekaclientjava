package com.example.eurekaclient.services;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    @NotBlank(message = "serviceName ist ein Pflichtfeld")
    @Schema(description = "Name der zu aktualisierenden Service-Instance (Pflichtfeld, dient als Identifikation).", example = "payment-service", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serviceName;

    @NotBlank(message = "newHostName darf nicht leer sein")
    @Schema(description = "Neuer Hostname, unter dem die Instance erreichbar ist.", example = "payment-host-02")
    private String newHostName;

    @NotBlank(message = "newIpAddress darf nicht leer sein")
    @Pattern(regexp = "^(([0-9]{1,3}\\.){3}[0-9]{1,3}|[a-zA-Z0-9._-]+)$", message = "Ungültige IP-Adresse oder Hostname")
    @Schema(description = "Neue IP-Adresse der Instance.", example = "192.168.1.99")
    private String newIpAddress;

    @Min(value = 1, message = "httpPort muss zwischen 1 und 65535 liegen")
    @Max(value = 65535, message = "httpPort muss zwischen 1 und 65535 liegen")
    @Schema(description = "Neuer HTTP-Port der Instance.", example = "8081")
    private int httpPort;

    @Min(value = 0, message = "securePort muss zwischen 0 und 65535 liegen")
    @Max(value = 65535, message = "securePort muss zwischen 0 und 65535 liegen")
    @Schema(description = "Neuer HTTPS/Secure-Port der Instance (0 = deaktiviert).", example = "8444")
    private int securePort;

    @Schema(description = "Gibt an, ob Clients den Secure-Port bevorzugen sollen.", example = "false")
    private boolean sslPreferred;
}
