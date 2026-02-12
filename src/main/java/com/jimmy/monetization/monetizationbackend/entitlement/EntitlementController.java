package com.jimmy.monetization.monetizationbackend.entitlement;

import com.jimmy.monetization.monetizationbackend.entitlement.dto.ListEntitlementsResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/entitlements")
public class EntitlementController {

    private final EntitlementQueryService service;

    public EntitlementController(EntitlementQueryService service) {
        this.service = service;
    }

    @GetMapping
    public ListEntitlementsResponse list(@RequestParam("userId") String userId) {
        var owned = service.listOwnedSkus(userId);
        return new ListEntitlementsResponse(userId, owned);
    }
}
