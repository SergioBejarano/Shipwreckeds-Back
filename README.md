# Shipwreckeds Backend

Backend de la aplicación "Shipwreckeds" (Spring Boot). Provee autenticación, gestión de partidas y el motor de juego con soporte WebSocket.

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