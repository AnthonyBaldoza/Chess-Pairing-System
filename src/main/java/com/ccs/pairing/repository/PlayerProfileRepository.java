package com.ccs.pairing.repository;

import com.ccs.pairing.model.PlayerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, Long> {
    Optional<PlayerProfile> findByNameIgnoreCase(String name);
    List<PlayerProfile> findByNameContainingIgnoreCaseOrderByNameAsc(String query);
    List<PlayerProfile> findAllByOrderByNameAsc();
}
