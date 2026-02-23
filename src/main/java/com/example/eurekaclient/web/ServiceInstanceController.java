package com.example.eurekaclient.web;

import com.example.eurekaclient.services.LifecycleManager;
import com.example.eurekaclient.services.ServiceInstance;
import com.example.eurekaclient.services.UpdateInstanceRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/instances")
@RequiredArgsConstructor
@Tag(
    name = "Service Instances",
    description = "Verwaltung von Eureka-Service-Instances: Abfrage konfigurierter und aktiv registrierter Instanzen " +
                  "sowie Aktualisierung von Verbindungsparametern (Host, IP, Port) mit automatischer De-Registrierung und Neuregistrierung bei Eureka."
)
public class ServiceInstanceController {

    private final LifecycleManager lifecycleManager;

    @GetMapping("/configured")
    @Operation(
        operationId = "getConfiguredInstances",
        summary = "Konfigurierte Instanzen abrufen",
        description = "Gibt alle Service-Instances zurück, die statisch in der Anwendungskonfiguration (z. B. application.yml) " +
                      "definiert sind – unabhängig davon, ob sie aktuell bei Eureka registriert sind."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Konfigurierte Instanzen erfolgreich zurückgegeben (leere Liste möglich).",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ServiceInstance.class)
        )
    )
    public ResponseEntity<List<ServiceInstance>> getConfiguredInstances() {
        return ResponseEntity.ok(lifecycleManager.getConfiguredInstances());
    }

    @GetMapping("/running")
    @Operation(
        operationId = "getRunningInstances",
        summary = "Aktiv registrierte Instanzen abrufen",
        description = "Gibt alle Service-Instances zurück, die aktuell aktiv bei Eureka registriert sind und Heartbeats senden " +
                      "(Status UP). Instanzen, die konfiguriert, aber noch nicht gestartet oder bereits abgemeldet wurden, " +
                      "erscheinen hier nicht."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Aktiv registrierte Instanzen erfolgreich zurückgegeben (leere Liste möglich).",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ServiceInstance.class)
        )
    )
    public ResponseEntity<List<ServiceInstance>> getRunningInstances() {
        return ResponseEntity.ok(lifecycleManager.getRunningInstances());
    }

    @PutMapping("/update")
    @Operation(
        operationId = "updateInstance",
        summary = "Verbindungsparameter einer Instanz aktualisieren",
        description = "Aktualisiert Host, IP-Adresse und/oder Ports einer laufenden Service-Instance. " +
                      "Der Lifecycle-Manager meldet die Instanz zunächst von Eureka ab, übernimmt die neuen Parameter " +
                      "und registriert die Instanz anschließend neu. Der Vorgang ist idempotent, sofern dieselben " +
                      "Parameter erneut übergeben werden."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Instanz erfolgreich aktualisiert und neu bei Eureka registriert.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "string", example = "Instanz 'my-service' erfolgreich aktualisiert und neu gestartet.")
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Keine konfigurierte Instance mit dem angegebenen serviceName gefunden.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "string", example = "ServiceInstance 'unknown-service' nicht gefunden.")
            )
        )
    })
    public ResponseEntity<String> updateInstance(
        @RequestBody(
            description = "Neue Verbindungsparameter der zu aktualisierenden Service-Instance. " +
                          "Pflichtfeld: serviceName (Identifikation). Alle übrigen Felder überschreiben die bisherige Konfiguration.",
            required = true,
            content = @Content(schema = @Schema(implementation = UpdateInstanceRequest.class))
        )
        UpdateInstanceRequest request
    ) {

        log.info("Update-Request für ServiceInstance: {}", request.getServiceName());

        ServiceInstance instance = lifecycleManager.updateInstance(request);

        if (instance == null) {
            log.warn("ServiceInstance '{}' nicht gefunden", request.getServiceName());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("ServiceInstance '" + request.getServiceName() + "' nicht gefunden.");
        }

        log.info("ServiceInstance '{}' erfolgreich aktualisiert", request.getServiceName());
        return ResponseEntity.ok(
                "Instanz '" + request.getServiceName() + "' erfolgreich aktualisiert und neu gestartet."
        );
    }
}
