package com.scopic.auction.repository;

import com.scopic.auction.domain.Bidder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Bidder, String> {
}
