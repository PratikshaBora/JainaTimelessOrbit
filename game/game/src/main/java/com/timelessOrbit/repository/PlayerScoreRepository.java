package com.timelessOrbit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.timelessOrbit.gamestate.PlayerScore;

@Repository
public interface PlayerScoreRepository extends JpaRepository<PlayerScore, Long>{

}
 