package com.example.freshfarm3.service;

import com.example.freshfarm3.dto.request.PayoutRequest;
import com.example.freshfarm3.dto.response.PayoutResponse;
import com.example.freshfarm3.entity.Shop;
import com.example.freshfarm3.entity.ShopPayout;
import com.example.freshfarm3.enums.PayoutStatus;
import com.example.freshfarm3.exception.ResourceNotFoundException;
import com.example.freshfarm3.repository.ShopPayoutRepository;
import com.example.freshfarm3.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PayoutService — Admin fixes how much a shop is owed for the products
 * they've sold, and later marks that amount as paid once the money has
 * actually been sent to the shop's registered bank account. The shop
 * sees both PENDING and PAID amounts on their dashboard.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutService {

    private final ShopPayoutRepository shopPayoutRepository;
    private final ShopRepository       shopRepository;
    private final NotificationService  notificationService;

    // ── ADMIN: Fix the amount owed to a shop ───────────────────────
    @Transactional
    public PayoutResponse createPayout(Long shopId, PayoutRequest req) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found: " + shopId));

        ShopPayout payout = ShopPayout.builder()
                .shop(shop)
                .amount(req.getAmount())
                .status(PayoutStatus.PENDING)
                .note(req.getNote())
                .build();

        ShopPayout saved = shopPayoutRepository.save(payout);
        notificationService.notifyPayoutCreated(shop, saved);

        log.info("Payout of {} fixed for shop {}", req.getAmount(), shopId);
        return mapToResponse(saved);
    }

    // ── ADMIN: Mark a payout as sent/paid ───────────────────────────
    @Transactional
    public PayoutResponse markPaid(Long payoutId) {
        ShopPayout payout = shopPayoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found: " + payoutId));

        if (payout.getStatus() == PayoutStatus.PAID) {
            throw new RuntimeException("This payout has already been marked as paid");
        }

        payout.setStatus(PayoutStatus.PAID);
        payout.setPaidAt(LocalDateTime.now());
        ShopPayout saved = shopPayoutRepository.save(payout);

        notificationService.notifyPayoutPaid(payout.getShop(), saved);

        log.info("Payout {} marked PAID", payoutId);
        return mapToResponse(saved);
    }

    // ── ADMIN: All payouts for one shop ─────────────────────────────
    @Transactional(readOnly = true)
    public List<PayoutResponse> getPayoutsForShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found: " + shopId));
        return shopPayoutRepository.findByShopOrderByCreatedAtDesc(shop).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── ADMIN: All payouts across every shop ────────────────────────
    @Transactional(readOnly = true)
    public List<PayoutResponse> getAllPayouts() {
        return shopPayoutRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── SHOP: My own payouts ─────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<PayoutResponse> getMyPayouts(String shopEmail) {
        Shop shop = shopRepository.findByUser_Email(shopEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
        return shopPayoutRepository.findByShopOrderByCreatedAtDesc(shop).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PayoutResponse mapToResponse(ShopPayout p) {
        return PayoutResponse.builder()
                .id(p.getId())
                .shopId(p.getShop().getId())
                .shopName(p.getShop().getShopName())
                .amount(p.getAmount())
                .status(p.getStatus().name())
                .note(p.getNote())
                .createdAt(p.getCreatedAt())
                .paidAt(p.getPaidAt())
                .build();
    }
}
