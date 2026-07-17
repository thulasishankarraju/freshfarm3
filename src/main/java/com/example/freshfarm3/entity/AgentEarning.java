package com.example.freshfarm3.entity;

import com.example.freshfarm3.enums.AgentEarningType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * AgentEarning — One earning record for a delivery agent.
 *
 * A DELIVERY_FEE row is created every time an agent completes a
 * delivery (OTP verified). A BONUS row is created automatically
 * whenever the agent completes their 5th, 10th, 15th... delivery of
 * the calendar day (see DeliveryService.AGENT_BONUS_EVERY_N_DELIVERIES
 * / AGENT_BONUS_AMOUNT).
 *
 * The agent's dashboard sums these rows to show today's earnings,
 * total earnings, and total bonuses earned.
 */
@Entity
@Table(name = "agent_earnings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentEarning extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_agent_id", nullable = false)
    private DeliveryAgent agent;

    // Null for BONUS rows that aren't tied to one specific delivery beyond
    // the one that triggered the milestone.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgentEarningType type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 200)
    private String note;
}
