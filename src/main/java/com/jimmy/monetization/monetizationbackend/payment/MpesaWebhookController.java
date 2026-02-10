package com.jimmy.monetization.monetizationbackend.payment;

import com.jimmy.monetization.monetizationbackend.payment.mpesa.dto.MpesaCallbackDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks/mpesa")
public class MpesaWebhookController {

    private final MpesaCallbackService service;

    public MpesaWebhookController(MpesaCallbackService service) {
        this.service = service;
    }

    @PostMapping("/callback")
    public ResponseEntity<?> callback(@RequestBody MpesaCallbackDTO dto) {
        service.handle(dto);
        return ResponseEntity.ok(Map.of("status", "processed"));
    }
}
