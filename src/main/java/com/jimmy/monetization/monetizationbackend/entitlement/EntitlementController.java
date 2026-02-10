package com.jimmy.monetization.monetizationbackend.entitlement;

import com.jimmy.monetization.monetizationbackend.entitlement.dto.EntitlementView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/entitlements")
public class EntitlementController {

    private final EntitlementQueryService queryService;

    public EntitlementController(EntitlementQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public EntitlementsResponse query(
            @RequestParam String externalUserId,
            @RequestParam String productSku
    ) {
        var entitlements = queryService.findActiveEntitlements(externalUserId, productSku);

        var views = entitlements.stream()
                .map(e -> new EntitlementView(
                        e.getId(),
                        e.getOrderId(),
                        e.getStatus().name(),
                        e.getGrantedAt(),
                        e.getExpiresAt()
                ))
                .toList();

        return new EntitlementsResponse(views);
    }

    public record EntitlementsResponse(List<EntitlementView> entitlements) {}
}
