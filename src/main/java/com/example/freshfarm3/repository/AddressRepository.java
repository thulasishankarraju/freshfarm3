package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.Address;
import com.example.freshfarm3.entity.Buyer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByBuyer(Buyer buyer);

    Optional<Address> findByBuyerAndIsDefaultTrue(Buyer buyer);

    long countByBuyer(Buyer buyer);
}
