package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.request.AgentLoginRequest;
import com.example.freshfarm3.dto.request.AgentRegisterRequest;
import com.example.freshfarm3.dto.response.AuthResponse;
import com.example.freshfarm3.entity.DeliveryAgent;
import com.example.freshfarm3.repository.DeliveryAgentRepository;
import com.example.freshfarm3.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AuthService             authService;
    private final DeliveryAgentRepository deliveryAgentRepository;

    // POST /api/auth/agent/register
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody AgentRegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerAgent(req));
    }

    // POST /api/auth/agent/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AgentLoginRequest req) {
        return ResponseEntity.ok(authService.loginAgent(req));
    }

    // GET /api/auth/agent/profile
    @GetMapping("/profile")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        DeliveryAgent agent = deliveryAgentRepository
                .findByUser_Email(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Agent profile not found"));

        return ResponseEntity.ok(Map.of(
                "agentId",       agent.getId(),
                "name",          agent.getUser().getName(),
                "email",         agent.getUser().getEmail(),
                "phone",         agent.getPhone(),
                "vehicleNumber", agent.getVehicleNumber(),
                "vehicleType",   agent.getVehicleType(),
                "isAvailable",   agent.getIsAvailable()
        ));
    }
}