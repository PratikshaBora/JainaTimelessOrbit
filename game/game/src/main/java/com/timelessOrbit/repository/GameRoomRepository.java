package com.timelessOrbit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.timelessOrbit.gamestate.GameRoom;

@Repository
public interface GameRoomRepository extends JpaRepository<GameRoom, Integer>{

}
