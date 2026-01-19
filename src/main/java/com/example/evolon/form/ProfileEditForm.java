package com.example.evolon.form;

import lombok.Data;

@Data
public class ProfileEditForm {

	private String nickname;
	private String profileImageUrl;

	private String lastName;
	private String firstName;

	private String postalCode;
	private String address;
	private String bio;

}
