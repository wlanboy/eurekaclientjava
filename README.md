# Projektbeschreibung

Dieses Repository enthält eine kleine Java-Anwendung (Eureka Client) zur Demonstration von Service-Discovery- und Konfigurationsmustern. Die Anwendung ist als Spring Boot–basiertes Microservice-Beispiel aufgebaut, lässt sich lokal mit Maven bauen, in einem Docker-Container betreiben.

Wichtige Anwendungsfälle:
- Registrieren/Abfragen von Diensten (Eureka-Client-Verhalten).
- Lokale Konfiguration über `services.json` und `application.properties`.
- Container-Deployment mit Volumen-Mounts für Konfiguration.

---

# Voraussetzungen

- Linux (Docker "--network host" wird in diesem README verwendet; auf macOS/Windows weicht Netzwerkverhalten ab)
- SDKMAN (empfohlen) oder eine andere Java-Installation
- Maven 3.9.9
- Docker (falls Container-Images gebaut/gestartet werden)
- Für native Builds: GraalVM mit musl-Unterstützung

---

# Konfiguration (wichtige Kommentare)

- services.json: Erwartetes Format ist ein JSON-Objekt mit Service-Einträgen. 
- application.properties: Spring Boot Konfiguration. Mindestens Port- und Eureka-Einstellungen anpassen.
- Volumen-Mounts in Docker: lokale Dateien werden in /app/ gemountet. Änderungen an diesen Dateien wirken sofort beim Neustart des Containers.
- Ports: Standard-HTTP-Port ist 8080 (anpassbar über application.properties oder Umgebungsvariablen).
- Netzwerk: `--network host` vereinfacht Service-Discovery lokal, kann aber Sicherheits- und Port-Konflikte verursachen.

# Java build

```bash
sdk install java 21-tem
sdk install maven 3.9.9 
mvn package
```

# Docker build
```bash
docker build -t eurekaclient:latest .

docker run --rm -p 8080:8080 \
  --network host \
  -v $(pwd)/services.json:/app/services.json \
  -v $(pwd)/application.properties:/app/application.properties \
  --name eurekaclient \
  eurekaclient:latest
```

# Docker run with published image from dockerhub
```bash
docker run --rm -p 8080:8080 \
  --network host \
  -v $(pwd)/services.json:/app/services.json \
  -v $(pwd)/application.properties:/app/application.properties \
  --name eurekaclient \
  wlanboy/eurekaclientjava:latest
```

# Update hostname and port of running instance
```bash
http://localhost:8761/eureka/apps/DUMMY-SERVICE

curl -X PUT "http://localhost:8080/instances/update" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "DUMMY-SERVICE",
    "newHostName": "localhost",
    "newIpAddress": "127.0.0.1",
    "httpPort": 8080,
    "securePort": 7443,
    "sslPreferred": false
  }'
````

# Helm install
```bash
kubectl create namespace eurekaclient
helm install eurekaclient ./eurekaclient-chart --namespace eurekaclient

kubectl logs -l app=eurekaclient -n eurekaclient

```

# Java Native build
```bash
sdk install java 21.0.2-graalce
sudo apt-get install musl musl-dev musl-tools

export GRAALVM_HOME=~/.sdkman/candidates/java/21.0.2-graalce/

mvn -Pnative -DskipTests -Dspring.aot.enabled=true -Dspring.native.buildArgs="--static --libc=musl" native:compile
```