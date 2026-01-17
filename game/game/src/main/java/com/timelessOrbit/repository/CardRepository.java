package com.timelessOrbit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.timelessOrbit.gamestate.Card;

@Repository
public interface CardRepository extends JpaRepository<Card, Long>{

}
