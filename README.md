# Shipwreckeds Backend

Backend de la aplicación "Shipwreckeds" (Spring Boot). Provee autenticación, gestión de partidas y el motor de juego con soporte WebSocket.

## Diagramas

### Diagrama de clases (modelo)

![alt text](src/img/modelo.png)

### Diagrama de componentes

![alt text](src/img/componenentes.png)

### Diagramas de actividades (Fuel War y Votación)

![alt text](<src/img/Shipwreckeds flujo (Fuel War).png>)

![alt text](<src/img/Shipwreckeds flujo (votación).png>)

### Diagrama de despliegue

![alt text](src/img/Despliegue.jpeg)

## Decisiones de Arquitectura

La arquitectura de comunicación en tiempo real de Shipwreckeds se diseñó para soportar interacciones bidireccionales de baja latencia entre clientes (jugadores) y el servidor, con un enfoque en escalabilidad razonable, claridad en el modelado de mensajes y compatibilidad con el ecosistema Spring.

1. Comunicación en tiempo real: WebSocket

- Justificación: WebSocket proporciona un canal full‑duplex persistente entre cliente y servidor, lo cual es esencial para un juego multijugador donde se requieren actualizaciones frecuentes (posiciones, eventos de juego, chat, votaciones) y respuesta rápida. A diferencia de polling o long‑polling, WebSocket reduce la sobrecarga de conexión y la latencia promedio, mejorando la experiencia de los jugadores.
- Uso en el proyecto: la capa de transporte usa la especificación WebSocket para mantener sesiones activas y transmitir eventos del `GameEngine` hacia los clientes y viceversa.

2. Protocolo de mensajería: STOMP sobre WebSocket

- Justificación: STOMP (Simple Text Oriented Messaging Protocol) sobre WebSocket aporta una capa de mensajería estructurada (con tópicos y colas lógicas, suscripciones y destinos) que facilita el diseño pub/sub de eventos de juego (p. ej. broadcast de estado del juego, mensajes privados, notificaciones de votación). Al usar STOMP se mejora la separación entre la lógica de transporte y la semántica del mensaje, lo que simplifica la evolución del protocolo de juego y la integración con componentes de Spring que ya ofrecen soporte nativo.
- Beneficios prácticos: enrutamiento sencillo de mensajes por destino, control de suscripciones por sesión, posibilidad de integrar brokers si se requiere escalabilidad horizontal (p. ej. RabbitMQ, ActiveMQ o un broker Redis) sin cambiar la API de los clientes.

3. Decisiones operacionales y de seguridad

- Broker y escalabilidad: la implementación inicial emplea el broker simple/embebido que provee Spring para facilitar el desarrollo.
- Autenticación y autorización: las conexiones WebSocket se establecen tras la autenticación del usuario (ver `AuthController` / `AuthService`), y se aplican reglas de acceso sobre destinos/STOMP headers para evitar que usuarios no autorizados envíen o se suscriban a canales restringidos.
- Consistencia y orden: los mensajes relevantes para el estado de juego se diseñan para ser idempotentes o incluir suficiente contexto (timestamps, identificadores de secuencia) cuando el orden y la duplicación puedan afectar la lógica de las partidas.

4. Implementación y mapeo al código

- Configuración de transporte y endpoints WebSocket: `src/main/java/com/arsw/shipwreckeds/config/WebSocketConfig.java` — define los endpoints, los prefijos de destino y la configuración STOMP.
- Controladores y manejo de mensajes: `src/main/java/com/arsw/shipwreckeds/controller/WebSocketController.java` y `GameController.java` exponen destinos y manejadores que traducen entre los DTOs del dominio (ej. `GameState`, `MoveCommand`, `ChatMessage`) y los mensajes STOMP enviados/recibidos por los clientes.
- Motor de juego: `src/main/java/com/arsw/shipwreckeds/service/GameEngine.java` publica eventos de estado y procesa comandos recibidos desde WebSocket/STOMP.

## Estructura principal

- Aplicación principal: [`com.arsw.shipwreckeds.ShipwreckedsBackendApplication`](src/main/java/com/arsw/shipwreckeds/ShipwreckedsBackendApplication.java) — [src/main/java/com/arsw/shipwreckeds/ShipwreckedsBackendApplication.java](src/main/java/com/arsw/shipwreckeds/ShipwreckedsBackendApplication.java)
- Configuración WebSocket: [`com.arsw.shipwreckeds.config.WebSocketConfig`](src/main/java/com/arsw/shipwreckeds/config/WebSocketConfig.java) — [src/main/java/com/arsw/shipwreckeds/config/WebSocketConfig.java](src/main/java/com/arsw/shipwreckeds/config/WebSocketConfig.java)

### Controladores (REST / WebSocket)

- [`com.arsw.shipwreckeds.controller.AuthController`](src/main/java/com/arsw/shipwreckeds/controller/AuthController.java) — [src/main/java/com/arsw/shipwreckeds/controller/AuthController.java](src/main/java/com/arsw/shipwreckeds/controller/AuthController.java)
- [`com.arsw.shipwreckeds.controller.MatchController`](src/main/java/com/arsw/shipwreckeds/controller/MatchController.java) — [src/main/java/com/arsw/shipwreckeds/controller/MatchController.java](src/main/java/com/arsw/shipwreckeds/controller/MatchController.java)
- [`com.arsw.shipwreckeds.controller.GameController`](src/main/java/com/arsw/shipwreckeds/controller/GameController.java) — [src/main/java/com/arsw/shipwreckeds/controller/GameController.java](src/main/java/com/arsw/shipwreckeds/controller/GameController.java)
- [`com.arsw.shipwreckeds.controller.WebSocketController`](src/main/java/com/arsw/shipwreckeds/controller/WebSocketController.java) — [src/main/java/com/arsw/shipwreckeds/controller/WebSocketController.java](src/main/java/com/arsw/shipwreckeds/controller/WebSocketController.java)

### Servicios y motor de juego

- [`com.arsw.shipwreckeds.service.AuthService`](src/main/java/com/arsw/shipwreckeds/service/AuthService.java) — [src/main/java/com/arsw/shipwreckeds/service/AuthService.java](src/main/java/com/arsw/shipwreckeds/service/AuthService.java)
- [`com.arsw.shipwreckeds.service.MatchService`](src/main/java/com/arsw/shipwreckeds/service/MatchService.java) — [src/main/java/com/arsw/shipwreckeds/service/MatchService.java](src/main/java/com/arsw/shipwreckeds/service/MatchService.java)
- [`com.arsw.shipwreckeds.service.GameEngine`](src/main/java/com/arsw/shipwreckeds/service/GameEngine.java) — [src/main/java/com/arsw/shipwreckeds/service/GameEngine.java](src/main/java/com/arsw/shipwreckeds/service/GameEngine.java)
- [`com.arsw.shipwreckeds.service.NpcService`](src/main/java/com/arsw/shipwreckeds/service/NpcService.java) — [src/main/java/com/arsw/shipwreckeds/service/NpcService.java](src/main/java/com/arsw/shipwreckeds/service/NpcService.java)
- [`com.arsw.shipwreckeds.service.RoleService`](src/main/java/com/arsw/shipwreckeds/service/RoleService.java) — [src/main/java/com/arsw/shipwreckeds/service/RoleService.java](src/main/java/com/arsw/shipwreckeds/service/RoleService.java)

### Modelos y DTOs (ejemplos)

Modelos principales:

- [`com.arsw.shipwreckeds.model.Match`](src/main/java/com/arsw/shipwreckeds/model/Match.java) — [src/main/java/com/arsw/shipwreckeds/model/Match.java](src/main/java/com/arsw/shipwreckeds/model/Match.java)
- [`com.arsw.shipwreckeds.model.Player`](src/main/java/com/arsw/shipwreckeds/model/Player.java) — [src/main/java/com/arsw/shipwreckeds/model/Player.java](src/main/java/com/arsw/shipwreckeds/model/Player.java)
- [`com.arsw.shipwreckeds.model.ChatMessage`](src/main/java/com/arsw/shipwreckeds/model/ChatMessage.java) — [src/main/java/com/arsw/shipwreckeds/model/ChatMessage.java](src/main/java/com/arsw/shipwreckeds/model/ChatMessage.java)
- [`com.arsw.shipwreckeds.model.Position`](src/main/java/com/arsw/shipwreckeds/model/Position.java) — [src/main/java/com/arsw/shipwreckeds/model/Position.java](src/main/java/com/arsw/shipwreckeds/model/Position.java)
- [`com.arsw.shipwreckeds.model.Task`](src/main/java/com/arsw/shipwreckeds/model/Task.java) — [src/main/java/com/arsw/shipwreckeds/model/Task.java](src/main/java/com/arsw/shipwreckeds/model/Task.java)
- [`com.arsw.shipwreckeds.model.Npc`](src/main/java/com/arsw/shipwreckeds/model/Npc.java) — [src/main/java/com/arsw/shipwreckeds/model/Npc.java](src/main/java/com/arsw/shipwreckeds/model/Npc.java)
- [`com.arsw.shipwreckeds.model.MatchStatus`](src/main/java/com/arsw/shipwreckeds/model/MatchStatus.java) — [src/main/java/com/arsw/shipwreckeds/model/MatchStatus.java](src/main/java/com/arsw/shipwreckeds/model/MatchStatus.java)

DTOs de uso público:

- [`com.arsw.shipwreckeds.model.dto.LoginRequest`](src/main/java/com/arsw/shipwreckeds/model/dto/LoginRequest.java) — [src/main/java/com/arsw/shipwreckeds/model/dto/LoginRequest.java](src/main/java/com/arsw/shipwreckeds/model/dto/LoginRequest.java)
- [`com.arsw.shipwreckeds.model.dto.CreateMatchRequest`](src/main/java/com/arsw/shipwreckeds/model/dto/CreateMatchRequest.java) — [src/main/java/com/arsw/shipwreckeds/model/dto/CreateMatchRequest.java](src/main/java/com/arsw/shipwreckeds/model/dto/CreateMatchRequest.java)
- [`com.arsw.shipwreckeds.model.dto.CreateMatchResponse`](src/main/java/com/arsw/shipwreckeds/model/dto/CreateMatchResponse.java) — [src/main/java/com/arsw/shipwreckeds/model/dto/CreateMatchResponse.java](src/main/java/com/arsw/shipwreckeds/model/dto/CreateMatchResponse.java)
- [`com.arsw.shipwreckeds.model.dto.GameState`](src/main/java/com/arsw/shipwreckeds/model/dto/GameState.java) — [src/main/java/com/arsw/shipwreckeds/model/dto/GameState.java](src/main/java/com/arsw/shipwreckeds/model/dto/GameState.java)
- Otros DTOs: [`AvatarState`](src/main/java/com/arsw/shipwreckeds/model/dto/AvatarState.java), [`MoveCommand`](src/main/java/com/arsw/shipwreckeds/model/dto/MoveCommand.java), votación y combustible — [src/main/java/com/arsw/shipwreckeds/model/dto/](src/main/java/com/arsw/shipwreckeds/model/dto/)

### Recursos y tests

- Configuración: [src/main/resources/application.properties](src/main/resources/application.properties)
- Test base: [`com.arsw.shipwreckeds.ShipwreckedsBackendApplicationTests`](src/test/java/com/arsw/shipwreckeds/ShipwreckedsBackendApplicationTests.java) — [src/test/java/com/arsw/shipwreckeds/ShipwreckedsBackendApplicationTests.java](src/test/java/com/arsw/shipwreckeds/ShipwreckedsBackendApplicationTests.java)

## Requisitos

- Java 11+ (o la versión que especifique el pom.xml)
- Maven

## Compilar y ejecutar

Compilar:

```sh
mvn clean package
```
