package com.zoopick.server.controller;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.zoopick.server.dto.CommonResponse;
import com.zoopick.server.dto.chat.*;
import com.zoopick.server.security.UserPrincipal;
import com.zoopick.server.service.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat Room API", description = "채팅방 생성, 조회 및 메시지 전송 API")
@RestController
@RequestMapping("/api/chat-rooms")
@NullMarked
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    public ChatRoomController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    @Operation(summary = "채팅방 목록 조회", description = "현재 로그인한 사용자가 참여 중인 채팅방 ID 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채팅방 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/")
    public ResponseEntity<CommonResponse<ListChatRoomResult>> getChatRooms(@AuthenticationPrincipal UserPrincipal principal) {
        ListChatRoomResult result = chatRoomService.getChatRooms(principal.id());
        return ResponseEntity.ok(CommonResponse.success(result));
    }

    @Operation(summary = "게시글 기준 채팅방 조회", description = "현재 로그인한 사용자와 특정 게시글 기준으로 연결된 채팅방을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채팅방 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "채팅방 또는 게시글을 찾을 수 없음")
    })
    @GetMapping("/find/{itemId}")
    public ResponseEntity<CommonResponse<FindChatRoomResult>> findChatRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "채팅방을 찾을 게시글 ID", example = "1")
            @PathVariable long itemId
    ) {
        FindChatRoomResult result = chatRoomService.findChatRoom(principal.id(), itemId);
        return ResponseEntity.ok(CommonResponse.success(result));
    }

    @Operation(summary = "채팅방 생성", description = "게시글과 상대 사용자 정보를 기반으로 새로운 채팅방을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "기존 채팅방"),
            @ApiResponse(responseCode = "201", description = "채팅방 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청값"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "게시글 또는 사용자를 찾을 수 없음")
    })
    @PostMapping("/create")
    public ResponseEntity<CommonResponse<CreateChatRoomResult>> createChatRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid CreateChatRoomRequest createChatRoomRequest
    ) {
        CreateChatRoomResult result = chatRoomService.createChatRoom(principal.id(), createChatRoomRequest);
        HttpStatusCode httpStatue = result.isCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(httpStatue).body(CommonResponse.success(result));
    }

    @PostMapping("/by-owner")
    public ResponseEntity<CommonResponse<CreateChatRoomResult>> createChatRoomByOwner(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid CreateChatRoomByOwnerRequest request
    ) {
        CreateChatRoomResult result = chatRoomService.createChatRoomByOwner(principal.id(), request.getOwnerId());
        HttpStatusCode httpStatue = result.isCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(httpStatue).body(CommonResponse.success(result));
    }

    @Operation(summary = "채팅방 단건 조회", description = "채팅방 ID로 채팅방 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채팅방 조회 성공"),
            @ApiResponse(responseCode = "400", description = "채팅방 참여자가 아님"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    })
    @GetMapping("/{roomId}")
    public ResponseEntity<CommonResponse<ChatRoomRecord>> getChatRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "조회할 채팅방 ID", example = "1")
            @PathVariable long roomId
    ) {
        ChatRoomRecord record = chatRoomService.getChatRoom(principal.id(), roomId);
        return ResponseEntity.ok(CommonResponse.success(record));
    }

    @Operation(summary = "채팅 메시지 조회", description = "채팅방의 메시지 목록을 조회합니다. 필요하면 필터를 함께 전달할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "메시지 조회 성공"),
            @ApiResponse(responseCode = "400", description = "채팅방 참여자가 아님"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    })
    @RequestMapping(value = "/{roomId}/messages", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<CommonResponse<ListMessagesResult>> getChatRoomMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "메시지를 조회할 채팅방 ID", example = "1")
            @PathVariable long roomId,
            @RequestBody(required = false) MessageFilter messageFilter
    ) {
        ListMessagesResult result = chatRoomService.getMessages(principal.id(), roomId, messageFilter);
        return ResponseEntity.ok(CommonResponse.success(result));
    }

    @Operation(summary = "메시지 전송", description = "채팅방에 메시지를 전송하고 상대방에게 알림을 보냅니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "메시지 전송 성공"),
            @ApiResponse(responseCode = "400", description = "채팅방 참여자가 아니거나 잘못된 요청값"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "알림 전송 실패")
    })
    @PostMapping("/{roomId}/messages/send")
    public ResponseEntity<CommonResponse<String>> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "메시지를 전송할 채팅방 ID", example = "1")
            @PathVariable long roomId,
            @RequestBody @Valid SendMessageRequest sendMessageRequest
    ) throws FirebaseMessagingException {
        chatRoomService.sendMessage(principal.id(), roomId, sendMessageRequest.getMessage());
        return ResponseEntity.ok(CommonResponse.success("done"));
    }

    @Operation(summary = "채팅방 종료", description = "채팅방을 종료 상태로 변경합니다. 종료 사유를 함께 전달합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채팅방 종료 성공"),
            @ApiResponse(responseCode = "400", description = "채팅방 참여자가 아니거나 잘못된 요청값"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "알림 전송 실패")
    })
    @PatchMapping("/{roomId}/close")
    public ResponseEntity<CommonResponse<String>> closeChatRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "종료할 채팅방 ID", example = "1")
            @PathVariable long roomId,
            @RequestBody @Valid CloseChatRoomRequest closeChatRoomRequest
    ) throws FirebaseMessagingException {
        chatRoomService.closeChatRoom(principal.id(), roomId, closeChatRoomRequest.getReason());
        return ResponseEntity.ok(CommonResponse.success("성공"));
    }

    @Operation(summary = "채팅방 재개", description = "종료된 채팅방을 다시 활성 상태로 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채팅방 재개 성공"),
            @ApiResponse(responseCode = "400", description = "채팅방 참여자가 아니거나 재개할 수 없는 상태"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "알림 전송 실패")
    })
    @PatchMapping("/{roomId}/reopen")
    public ResponseEntity<CommonResponse<String>> reopenChatRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "재개할 채팅방 ID", example = "1")
            @PathVariable long roomId
    ) throws FirebaseMessagingException {
        chatRoomService.reopenChatRoom(principal.id(), roomId);
        return ResponseEntity.ok(CommonResponse.success("성공"));
    }
}
