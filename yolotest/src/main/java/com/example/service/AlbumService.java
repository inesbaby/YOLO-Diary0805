package com.example.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.entity.Album;
import com.example.entity.AlbumUser;
import com.example.entity.Diary;
import com.example.entity.User;
import com.example.exception.BadRequestException;
import com.example.exception.ResourceNotFoundException;
import com.example.payload.AlbumResponse;
import com.example.payload.DiaryResponse;
import com.example.payload.PagedResponse;
import com.example.payload.UserSummary;
import com.example.repository.AlbumRepository;
import com.example.repository.AlbumUserRepository;
import com.example.repository.DiaryRepository;
import com.example.repository.UserRepository;
import com.example.security.UserPrincipal;
import com.example.util.AppConstants;
import com.example.util.ModelMapper;

//https://docs.oracle.com/javase/tutorial/datetime/iso/instant.html(時間參照網址）
//https://lw900925.github.io/java/java8-newtime-api.html（時間參照網址）
@Service
public class AlbumService {
	@Autowired
	private AlbumRepository albumRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private AlbumUserRepository albumUserRepository;
	@Autowired
	private DiaryRepository diaryRepository;

	private static final Logger logger = LoggerFactory.getLogger(AlbumService.class);

	public List<AlbumResponse> getAlbumsCreatedByMe(UserPrincipal currentUser, int page, int size) {
		validatePageNumberAndSize(page, size);
		User user = userRepository.findByUsername(currentUser.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User", "username", currentUser.getUsername()));

		Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
		Page<Album> albums = albumRepository.findByCreatedBy(currentUser.getUsername(), pageable);
		if (albums.getNumberOfElements() == 0) {
//			return new PagedResponse<>(Collections.emptyList(), albums.getNumber(), albums.getSize(),
//					albums.getTotalElements(), albums.getTotalPages(), albums.isLast());
		}

		List<AlbumResponse> albumResponses = albums.map(album -> {
			return ModelMapper.mapAlbumToAlbumResponse(album, user);
		}).getContent();
//		return new PagedResponse<>(albumResponses, albums.getNumber(), albums.getSize(), albums.getTotalElements(),
//				albums.getTotalPages(), albums.isLast());
		return albumResponses;

	}

	public List<AlbumResponse> getAlbumsAboutMe(UserPrincipal currentUser, int page, int size) {
		validatePageNumberAndSize(page, size);
		User user = userRepository.findByUsername(currentUser.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User", "username", currentUser.getUsername()));

		Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
		Page<AlbumUser> albumUser = albumUserRepository.findByUser(user, pageable);

		List<AlbumResponse> albumResponse = albumUser.map(albumUsers -> {
			return ModelMapper.mapAlbumUserToAlbumUserResponseByUsername(albumUsers);
		}).getContent();

		return albumResponse;
	}

	public List<AlbumResponse> getAlbumsAboutMeByYear(UserPrincipal currentUser, int page, int size) {
		validatePageNumberAndSize(page, size);
		User user = userRepository.findByUsername(currentUser.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User", "username", currentUser.getUsername()));

		Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
		Page<AlbumUser> albumUser = albumUserRepository.findByUser(user, pageable);

		Instant nowTime = Instant.now();
		LocalDateTime nowTimes = LocalDateTime.ofInstant(nowTime, ZoneId.systemDefault());
		System.out.println("size : " + albumUser.getContent().size());
		List<AlbumResponse> albumResponse = new ArrayList<>();
		AlbumResponse albumResponses = new AlbumResponse();
		for (int i = 0; i < albumUser.getContent().size(); i++) {
			LocalDateTime createdTime = LocalDateTime.ofInstant(albumUser.getContent().get(i).getAlbum().getCreatedAt(),
					ZoneId.systemDefault());
			System.out.println("createdTime.minusYears(1) : " + createdTime.minusYears(1));

			if (createdTime.isAfter(nowTimes.minusYears(1))) {
				System.out.println(createdTime.getDayOfYear() - nowTimes.getYear() - 1);
				albumResponses = ModelMapper.mapAlbumUserToAlbumUserResponseByUsername(albumUser.getContent().get(i));
				System.out.println("name: " + albumResponses.getName());
				albumResponse.add(albumResponses);
			}
		}

		return albumResponse;
	}

	public List<AlbumResponse> getAlbumsAboutMeByMonth(UserPrincipal currentUser, int page, int size) {
		validatePageNumberAndSize(page, size);
		User user = userRepository.findByUsername(currentUser.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User", "username", currentUser.getUsername()));

		Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
		Page<AlbumUser> albumUser = albumUserRepository.findByUser(user, pageable);

		Instant nowTime = Instant.now();
		LocalDateTime nowTimes = LocalDateTime.ofInstant(nowTime, ZoneId.systemDefault());
		System.out.println("size : " + albumUser.getContent().size());
		List<AlbumResponse> albumResponse = new ArrayList<>();
		AlbumResponse albumResponses = new AlbumResponse();
		for (int i = 0; i < albumUser.getContent().size(); i++) {
			LocalDateTime createdTime = LocalDateTime.ofInstant(albumUser.getContent().get(i).getAlbum().getCreatedAt(),
					ZoneId.systemDefault());
			System.out.println(createdTime.getDayOfMonth());
			System.out.println(createdTime.getDayOfYear());
			System.out.println("nowTimes.minusMonths(1) : " + nowTimes.minusMonths(1));
			System.out.println("createdTime : " + createdTime);

			boolean orNot =createdTime.isAfter(nowTimes.minusWeeks(1))==false;
			System.out.println("must be ture or false : "+ orNot);
			if (createdTime.isAfter(nowTimes.minusMonths(1))&& orNot) {
				// nowTimes.getDayOfYear() -createdTime.getDayOfYear() <= 30
				albumResponses = ModelMapper.mapAlbumUserToAlbumUserResponseByUsername(albumUser.getContent().get(i));
				albumResponse.add(albumResponses);
			}
		}

		return albumResponse;
	}

	public List<AlbumResponse> getAlbumsAboutMeByWeek(UserPrincipal currentUser, int page, int size) {
		validatePageNumberAndSize(page, size);
		User user = userRepository.findByUsername(currentUser.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User", "username", currentUser.getUsername()));

		Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
		Page<AlbumUser> albumUser = albumUserRepository.findByUser(user, pageable);

		Instant nowTime = Instant.now();
		LocalDateTime nowTimes = LocalDateTime.ofInstant(nowTime, ZoneId.systemDefault());
		System.out.println("size : " + albumUser.getContent().size());
		List<AlbumResponse> albumResponse = new ArrayList<>();
		AlbumResponse albumResponses = new AlbumResponse();
		for (int i = 0; i < albumUser.getContent().size(); i++) {
			LocalDateTime createdTime = LocalDateTime.ofInstant(albumUser.getContent().get(i).getAlbum().getCreatedAt(),
					ZoneId.systemDefault());
			System.out.println("createdTime.getDayOfMonth()" + createdTime.getDayOfMonth());
			System.out.println("createdTime.getDayOfYear()" + createdTime.getDayOfYear());
			System.out.println("nowTimes.minusWeeks(1)" + nowTimes.minusWeeks(1));
			if (createdTime.isAfter(nowTimes.minusWeeks(1))) {
				// nowTimes.getDayOfYear() -createdTime.getDayOfYear() <= 7
				System.out.println("nowTimes.getDayOfYear() : " + nowTimes.getDayOfYear());
				System.out.println("nowTimes.getDayOfYear() : " + nowTimes.getDayOfYear());
				System.out.println("createdTime.getDayOfYear() : " + createdTime.getDayOfYear());

				albumResponses = ModelMapper.mapAlbumUserToAlbumUserResponseByUsername(albumUser.getContent().get(i));
				albumResponse.add(albumResponses);
			}
		}

		return albumResponse;
	}

	public List<UserSummary> getUsersAboutAlbum(String albumId, int page, int size) {
		validatePageNumberAndSize(page, size);
		Album album = albumRepository.findById(albumId)
				.orElseThrow(() -> new ResourceNotFoundException("Album", "Id", albumId));

		Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
		Page<AlbumUser> albumUser = albumUserRepository.findByAlbum(album, pageable);

		List<UserSummary> userSummary = albumUser.map(albumUsers -> {
			return ModelMapper.mapAlbumUserToAlbumUserResponseByAlbumId(albumUsers);
		}).getContent();

		return userSummary;
	}

	/* 以下為上方有使用到的方法，validatePageNumberAndSize、getDiaryCreatorMap */

	private void validatePageNumberAndSize(int page, int size) {
		if (page < 0) {
			throw new BadRequestException("Page number cannot be less than zero.");
		}

		if (size > AppConstants.MAX_PAGE_SIZE) {
			throw new BadRequestException("Page size must not be greater than " + AppConstants.MAX_PAGE_SIZE);
		}
	}

}
