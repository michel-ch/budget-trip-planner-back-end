package com.planner.app.dto;

import com.planner.app.entity.Image;
import com.planner.app.entity.Location;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String lastName;
    private String firstName;
    private String username;
    private String mail;
    private String phoneNumber;
    private Date birthday;
    private Image image;
    private Location location;

    public UserDTO(String lastName, String firstName, String username, String mail, String phoneNumber, Date birthday) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.username = username;
        this.mail = mail;
        this.phoneNumber = phoneNumber;
        this.birthday = birthday;
    }
}
