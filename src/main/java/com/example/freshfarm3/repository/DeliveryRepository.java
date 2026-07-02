package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.Delivery;
import com.example.freshfarm3.entity.DeliveryAgent;
import com.example.freshfarm3.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByOrder_Id(Long orderId);

    List<Delivery> findByDeliveryAgent(DeliveryAgent agent);

    List<Delivery> findByDeliveryAgentAndDeliveryStatusNot(
            DeliveryAgent agent, DeliveryStatus status);

    List<Delivery> findByDeliveryStatus(DeliveryStatus status);
}
