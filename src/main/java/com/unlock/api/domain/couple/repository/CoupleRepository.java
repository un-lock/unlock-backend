package com.unlock.api.domain.couple.repository;

import com.unlock.api.domain.couple.entity.Couple;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoupleRepository extends JpaRepository<Couple, Long> {
}
