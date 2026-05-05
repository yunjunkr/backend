package com.zoopick.server.controller;

import com.zoopick.server.entity.LockerCommand;
import com.zoopick.server.service.LockerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Tag(name = "Locker API", description = "락커(iot)통신 관련 API")
@RestController
@RequestMapping("/api/lockers")
@RequiredArgsConstructor
public class LockerController {

    private final LockerService lockerService;

    @Operation(
            summary = "사물함 해제 요청",
            description = """
                    사물함을 여는 요청을 큐에 등록합니다.                                       
                    요청 본문에 itemId가 있으면 보관 흐름, 없으면 회수 흐름으로 자동 분기됩니다.
                    - 보관: 빈 사물함에 신고된 물품을 넣을 때                
                    - 회수: 사물함에 보관된 물품을 가져갈 때            
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "해제 명령 등록 성공"),
            @ApiResponse(responseCode = "400", description = "보관 가능한 상태가 아니거나 itemId 누락"),
            @ApiResponse(responseCode = "404", description = "사물함 또는 물품을 찾을 수 없음"),
    })
    @PostMapping("/{lockerId}/unlock")
    public ResponseEntity<Map<String, Object>> unlock(
            @PathVariable Long lockerId,
            @RequestBody(required = false) UnlockRequest req) {

        Long itemId = (req != null) ? req.itemId() : null;
        LockerCommand cmd = lockerService.requestUnlock(lockerId, itemId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "command_id", cmd.getId(),
                "command", cmd.getCommand(),
                "message", "자물쇠가 곧 열립니다"
        ));
    }

    @Operation(
            summary = "사물함 잠금 요청",
            description = "사물함 잠금 명령을 큐에 등록합니다. R4가 다음 폴링 시 가져가 솔레노이드를 잠급니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "잠금 명령 등록 성공"),
            @ApiResponse(responseCode = "404", description = "사물함을 찾을 수 없음"),
    })
    @PostMapping("/{lockerId}/lock")
    public ResponseEntity<Map<String, Object>> lock(@PathVariable Long lockerId) {
        LockerCommand cmd = lockerService.requestLock(lockerId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "command_id", cmd.getId(),
                "command", cmd.getCommand(),
                "message", "자물쇠가 곧 잠깁니다"
        ));
    }

    @Operation(
            summary = "R4 폴링 — 대기 중인 명령 조회",
            description = """
                    Arduino R4 디바이스가 2초마다 호출하는 엔드포인트입니다.
                    
                    대기 중(PENDING)인 명령이 있으면 가장 오래된 것을 반환하고 CONSUMED 상태로 전환합니다.
                    없으면 `command: "NONE"`을 반환합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "명령 조회 성공 (NONE 또는 OPEN/CLOSE)"),
    })
    @GetMapping("/{lockerId}/pending")
    public ResponseEntity<Map<String, Object>> pollCommand(@PathVariable Long lockerId) {
        Optional<LockerCommand> cmd = lockerService.pollNextCommand(lockerId);
        if (cmd.isEmpty()) {
            return ResponseEntity.ok(Map.of("command", "NONE"));
        }
        return ResponseEntity.ok(Map.of(
                "command", cmd.get().getCommand().name(),
                "command_id", cmd.get().getId()
        ));
    }

    @Operation(
            summary = "R4 ACK — 명령 실행 완료 보고",
            description = """
                    Arduino R4가 솔레노이드 동작을 완료한 뒤 호출하는 엔드포인트입니다.       
                    해당 명령의 상태를 COMPLETED로 전환하고 completed_at 타임스탬프를 기록합니다.
                    
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ACK 처리 성공"),
            @ApiResponse(responseCode = "400", description = "명령이 해당 사물함의 것이 아님"),
            @ApiResponse(responseCode = "404", description = "명령을 찾을 수 없음"),
    })
    public ResponseEntity<Void> ack(
            @PathVariable Long lockerId,
            @PathVariable Long commandId) {
        lockerService.ackCommand(lockerId, commandId);
        return ResponseEntity.ok().build();
    }

    public record UnlockRequest(Long itemId) {
    }
}