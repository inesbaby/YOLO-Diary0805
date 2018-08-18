package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.entity.Diary;
import com.example.entity.Photo;
import com.example.exception.BadRequestException;
import com.example.exception.MySelfieNotFoundException;
import com.example.repository.DiaryRepository;
import com.example.repository.PhotoRepository;

import java.io.IOException;

@Service
public class PhotoStorageService {

	@Autowired
	private PhotoRepository photoRepository;
	@Autowired
	private DiaryRepository diaryRepository;


	public Photo storePhoto(MultipartFile photo, Long diaryId) {
		// Normalize file name
		String photoName = StringUtils.cleanPath(photo.getOriginalFilename());
		Diary diary = new Diary(diaryId);

		try {
			// Check if the file's name contains invalid characters
			if (photoName.contains("..")) {
				throw new BadRequestException("Sorry! Filename contains invalid path sequence " + photoName);
			}
			

			Photo photos = new Photo(photoName, photo.getContentType(), photo.getBytes(),diary);
			
			return diaryRepository.findById(diaryId).map(diaryy -> {
				return photoRepository.save(photos);
			}).orElseThrow(() -> new BadRequestException("DiaryId " + diaryId + "not found"));
			
		} catch (IOException ex) {
			throw new BadRequestException("Could not store file " + photoName + ". Please try again!", ex);
		}
	}
	
	
	

	public Photo getPhoto(String photoId) {
		return photoRepository.findById(photoId)
				.orElseThrow(() -> new MySelfieNotFoundException("File not found with id " + photoId));
	}
	
	

}
