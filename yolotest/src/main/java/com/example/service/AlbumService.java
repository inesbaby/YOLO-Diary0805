package com.example.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import com.example.entity.Diary;
import com.example.entity.User;
import com.example.exception.BadRequestException;
import com.example.exception.ResourceNotFoundException;
import com.example.payload.AlbumResponse;
import com.example.payload.DiaryResponse;
import com.example.payload.PagedResponse;
import com.example.repository.AlbumRepository;
import com.example.repository.DiaryRepository;
import com.example.repository.UserRepository;
import com.example.security.UserPrincipal;
import com.example.util.AppConstants;
import com.example.util.ModelMapper;

@Service
public class AlbumService {
	@Autowired
	private AlbumRepository albumRepository;
	@Autowired
	private UserRepository userRepository;

	private static final Logger logger = LoggerFactory.getLogger(AlbumService.class);

	public PagedResponse<AlbumResponse> getAlbumsCreatedByMe(UserPrincipal currentUser, int page, int size) {
		validatePageNumberAndSize(page, size);
		User user = userRepository.findByUsername(currentUser.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User", "username", currentUser.getUsername()));

		Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
		Page<Album> albums = albumRepository.findByCreatedBy(currentUser.getUsername(), pageable);
		if (albums.getNumberOfElements() == 0) {
			return new PagedResponse<>(Collections.emptyList(), albums.getNumber(), albums.getSize(),
					albums.getTotalElements(), albums.getTotalPages(), albums.isLast());
		}

		List<AlbumResponse> albumResponses = albums.map(album -> {
			return ModelMapper.mapAlbumToAlbumResponse(album, user);
		}).getContent();
		return new PagedResponse<>(albumResponses, albums.getNumber(), albums.getSize(), albums.getTotalElements(),
				albums.getTotalPages(), albums.isLast());

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
