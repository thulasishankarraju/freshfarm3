package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.AgentEarning;
import com.example.freshfarm3.entity.DeliveryAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgentEarningRepository extends JpaRepository<AgentEarning, Long> {

    List<AgentEarning> findByAgentOrderByCreatedAtDesc(DeliveryAgent agent);

    List<AgentEarning> findByAgentAndCreatedAtBetween(
            DeliveryAgent agent, LocalDateTime from, LocalDateTime to);
}
