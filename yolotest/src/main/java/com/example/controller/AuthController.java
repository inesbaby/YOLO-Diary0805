package com.example.controller;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.mail.MailException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.entity.Album;
import com.example.entity.Role;
import com.example.entity.RoleName;
import com.example.entity.User;
import com.example.entity.UserFriend;
import com.example.entity.UserFriendId;
import com.example.exception.AppException;
import com.example.exception.ResourceNotFoundException;
import com.example.payload.ApiResponse;
import com.example.payload.JwtAuthenticationResponse;
import com.example.payload.LoginRequest;
import com.example.payload.SignUpRequest;
import com.example.repository.RoleRepository;
import com.example.repository.UserRepository;
import com.example.security.CurrentUser;
import com.example.security.JwtTokenProvider;
import com.example.security.UserPrincipal;
import com.example.service.NotificationService;

//https://www.callicoder.com/spring-boot-spring-security-jwt-mysql-react-app-part-2/
@RestController
@RequestMapping("/api/auth")

public class AuthController {

	private static final Logger logger = LoggerFactory.getLogger(DiaryController.class);

	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserRepository userRepository;

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	JwtTokenProvider tokenProvider;
	
	@Autowired
	UserFriendRepository userfriendrepository;

	@Autowired
	NotificationService notificationService;
	


	@PreAuthorize("hasRole('USER')")
	@GetMapping("/private")
	public String privateArea() {
		System.out.println("privateArea");
		return "bojour";

	}

	// 登入
	@PostMapping("/login")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);
		System.out.println(authentication.getAuthorities());
		String jwt = tokenProvider.generateToken(authentication);
		return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
	}

	// 註冊
	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
		if (userRepository.existsByUsername(signUpRequest.getUsername())) {
			return new ResponseEntity(new ApiResponse(false, "Username is already taken!"), HttpStatus.BAD_REQUEST);
		}

		if (userRepository.existsByEmail(signUpRequest.getEmail())) {
			return new ResponseEntity(new ApiResponse(false, "Email Address already in use!"), HttpStatus.BAD_REQUEST);
		}

		User user = new User(signUpRequest.getUsername(), signUpRequest.getEmail(), signUpRequest.getPassword());

		user.setPassword(passwordEncoder.encode(user.getPassword()));

		Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
				.orElseThrow(() -> new AppException("User Role not set."));
		System.out.println(userRole);

		user.setRoles(Collections.singleton(userRole));
		System.out.println(Collections.singleton(userRole));// [com.example.entity.Role@3eb19954]

		User result = userRepository.save(user);

		URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/user/{username}")
				.buildAndExpand(result.getUsername()).toUri();
		System.out.println(location);// http://localhost:8080/api/user/testin221111
		
		try {
			notificationService.sendNotification(user);
		}catch(MailException e) {
			logger.info("Error sending email"+e.getMessage());
		}

		return ResponseEntity.created(location).body(new ApiResponse(true, "User registered successfully"));
	}

	// 更改使用者密碼
	@PutMapping("user/{username}")
	public User update(@PathVariable String username, @RequestBody User user) {
		return userRepository.findByUsername(username).map(users -> {
			users.setPassword(passwordEncoder.encode(user.getPassword()));
			return userRepository.save(users);
		}).orElseThrow(()->new ResourceNotFoundException("Username" + username + "not found", null, user));
	    
				
			
	}

	// 刪除
	@RequestMapping(value = "user/{username}", method = RequestMethod.DELETE)
	public void delete(@PathVariable String username) {
		userRepository.deleteById(username);

	}
	//新增好友 一個人加，對方確認，雙方互為好友
			@PostMapping("/addfriend/{username}")
			public UserFriend addFriend(@PathVariable String username , @CurrentUser UserPrincipal currentUser){
			//	if (userfriendrepository.existsById(username))
			//	if (userRepository.existsByUsername(signUpRequest.getUsername())) {
			//		return new ResponseEntity(new ApiResponse(false, "Username is already taken!"), HttpStatus.BAD_REQUEST);
			//	}
				String current = currentUser.getUsername();
				User userdata = new User(current);
				User friend = new User(username);
//				userfriendrepository.findByUser(userdata).map(thedata ->{
//					if(thedata.getFriend().getUsername().equals(current)&& thedata.getUser().getUsername().equals(username)) {
//						return thedata;
//					} 
					if(username.equals(currentUser.getUsername()) ) {
						System.out.println("can not add yourself");
						return null;
					}
					else {
						System.out.println("currentUser = "+currentUser.getUsername());
						System.out.println("findUser = "+username);
						return userRepository.findByUsername(username).map(frienddata ->{
						UserFriend findfriend = new UserFriend();
						findfriend.setFriend(friend);
						findfriend.setUser(userdata);
						findfriend.setConfirmed(true);
					    return userfriendrepository.save(findfriend);
						}).orElseThrow(()->new ResourceNotFoundException("Fail!!!!!!", username, currentUser));	
						}
					
					
//				});
//				return null;
				
				
				
}
	//刪除好友 一個人刪除，雙方名單都會除名
			@Transactional
			@RequestMapping(value = "/deleteFriend/{username}", method = RequestMethod.DELETE)
			public void deletefriend(@PathVariable String username, @CurrentUser UserPrincipal currentUser) {
				System.out.println("DELETE currentUser = "+ currentUser.getUsername());
				System.out.println("DELETE username = "+ username);
				User user = new User(currentUser.getUsername());
				User friend = new User(username);
				
				if (username.equals(currentUser.getUsername())) {
					System.out.println("Don't Delete Yourself");
				}else {
				UserFriendId userfriendId = new UserFriendId();
				UserFriendId frienduserId = new UserFriendId();
				userfriendId.setUser(user);
				userfriendId.setFriend(friend);
				frienduserId.setUser(friend);
				frienduserId.setFriend(user);
				userfriendrepository.deleteById(userfriendId);
				userfriendrepository.deleteById(frienduserId);
				
				
				
					
					
					
					
					
				
				}
				
//					else {
//				userRepository.findByUsername(username).map(users ->{
//					System.out.println("User List:");
//					System.out.println("User name:"+ users.getUsername());
//					System.out.println("User email:"+ users.getEmail());
					 // System.out.println("Enter");
//					System.out.println("FindaUser = "+users.getUsername());
//					if(users.getUsername().equals(username)) {
//						System.out.println("DELETE SUCCESSFULLY!");
//						return userfriendrepository.deleteByUser(users);
//						
//						
//					}
//						else {
//						System.out.println("DELETE Failed!");
//						  return null;
//							}).orElseThrow(()->new ResourceNotFoundException("Faill!!!!!!", currentUser.getUsername(), null));
//					}
//					return null;
//					    
//				
//				}).orElseThrow(()->new ResourceNotFoundException("Faill!!!!!!", currentUser.getUsername(), null));
			}

			
			}

