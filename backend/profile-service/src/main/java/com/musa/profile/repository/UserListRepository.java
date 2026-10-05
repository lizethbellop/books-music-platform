package com.musa.profile.repository;

import com.musa.profile.entity.UserList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserListRepository extends JpaRepository<UserList, UUID> {

    List<UserList> findByProfileIdOrderByCreatedAtDesc(UUID profileId);
}