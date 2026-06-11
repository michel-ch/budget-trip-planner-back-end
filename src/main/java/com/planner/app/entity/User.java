package com.planner.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name= "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "last_name", length = 250)
    private String lastName;

    @Column(name = "first_name", length = 250)
    private String firstName;

    @Column(name = "username", length = 250, nullable = false,unique = true)
    private String username;

    @Column(name = "password", length = 250, nullable = false)
    private String password;

    @Column(name = "mail", length = 250, nullable = false,unique = true)
    private String mail;

    @Column(name = "phone_numb", length = 20)
    private String phoneNumber;

    @Column(name = "birthday")
    private Date birthday;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_image_id")
    private Image profile_image_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location_id;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "friends",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "friend_id")
    )
    private Set<User> friends = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "group_memberships",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<TravelGroup> travelGroups = new HashSet<>();

    public User(String username, String password, String mail) {
        this.username = username;
        this.password = password;
        this.mail = mail;
    }

}
