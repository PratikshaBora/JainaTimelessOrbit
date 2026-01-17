package com.timelessOrbit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.timelessOrbit.gamestate.Player;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Integer>{

	Player findByUsernameAndMobileNumber(String username,String password);
}
