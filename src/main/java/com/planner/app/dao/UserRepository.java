package com.planner.app.dao;

import com.planner.app.dto.UserDTO;
import com.planner.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    @Query("SELECT new com.planner.app.dto.UserDTO(u.lastName, u.firstName, u.username, u.mail, u.phoneNumber, u.birthday)" +
            " FROM User u WHERE u.username = :username OR u.mail = :username")
    Optional<UserDTO> findByUsernameOrEmailDTO(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.username = :username OR u.mail = :username")
    Optional<User> findByUsernameOrEmail(@Param("username") String username);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username")
    boolean existsByUsername(@Param("username") String username);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.mail = :mail")
    boolean existsByMail(@Param("mail") String mail);

}
