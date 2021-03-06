package com.example.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.engine.controller.EngineFunc;
import com.example.engine.entity.RetrieveFace;
import com.example.engine.entity.TrainFace;
import com.example.engine.util.CmdUtil;
import com.example.engine.util.Textfile;
import com.example.engine.util.TxtUtil;
import com.example.entity.Selfie;
import com.example.entity.User;
import com.example.exception.BadRequestException;
import com.example.payload.AlbumResponse;
import com.example.payload.DataRequest;
import com.example.payload.PhotoResponse;
import com.example.payload.UploadSelfieResponse;
import com.example.repository.SelfieRepository;
import com.example.repository.UserRepository;
import com.example.security.CurrentUser;
import com.example.security.UserPrincipal;
import com.example.service.SelfieStorageService;
import com.mysql.fabric.xmlrpc.base.Array;

@RestController
@RequestMapping("/api/selfie")
//MultipartException: Current request is not a multipart request
//https://stackoverflow.com/questions/42013087/multipartexception-current-request-is-not-a-multipart-request
public class UploadSelfieController {

	private static final Logger logger = LoggerFactory.getLogger(UploadSelfieController.class);

	static String SelfieFILEPATH = "C:\\engine\\selfie\\";
	static String FILEPATH = "C:\\engine\\list.txt";
	// /Users/ines/Desktop/photo --> ines mac's path
	// /Users/ines/Desktop/engine/selfie/ --> ines mac's path
	// C:\\engine\\selfie\\ --> laboratory's path
	// C:\\engine\\list.txt --> laboratory's path

	@Autowired
	SelfieStorageService selfieStorageService;

	@Autowired
	SelfieRepository selfieRepository;
	@Autowired
	UserRepository userRepository;

	@Autowired
	Textfile txt;

	@Autowired
	EngineFunc engine;

//將檔案blob轉成絕對路徑
	public static void blob(byte[] imageByte, String name) { // 改成username

		BufferedImage image = null;
		try {

			// imageByte = DatatypeConverter.parseBase64Binary(imageString);
			ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
			image = ImageIO.read(new ByteArrayInputStream(imageByte));
			bis.close();

			File outputfile = new File(SelfieFILEPATH + name + ".jpg");

			ImageIO.write(image, "jpg", outputfile);

		} catch (IOException e) {

			e.printStackTrace();
		}
	}

//上傳頭貼

	private UploadSelfieResponse uploadSelfie(@RequestParam("file") MultipartFile file, @CurrentUser String current) {// @PathVariable(value
																														// =
																														// "username")
		Selfie selfie = selfieStorageService.storeSelfie(file, current);

		String selfieDownloadURI = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/selfie/downloadSelfie/").path(selfie.getId()).toUriString();
		System.out.println(selfieDownloadURI);
		selfie.setSelfieUri(selfieDownloadURI);
		selfieRepository.save(selfie);
		blob(selfie.getSelfiedata(), current);
		String selfieId = selfie.getId();
		selfieRepository.findById(selfieId).map(set -> {
			// 改成使用使用者帳號(唯一值)只會用在大頭照的部分
			set.setSelfiePath(SelfieFILEPATH + current + ".jpg");

			return selfieRepository.save(set);
		}).orElseThrow(() -> new BadRequestException("SelfieId " + selfieId + "not found"));

		return new UploadSelfieResponse(selfie.getSelfieName(), file.getContentType(), selfieDownloadURI,
				file.getSize());

	}

//上傳頭貼
	@RequestMapping(value = "/uploadmany", headers = "content-type=multipart/*", method = RequestMethod.POST)
	public List<UploadSelfieResponse> uploadSelfies(@RequestParam(value = "file", required = true) MultipartFile[] file,
			@CurrentUser UserPrincipal currentUser) {
		System.out.println("upload selfie!!!!!!!!!!!!!!(" + file.length + ")");
		String username = currentUser.getUsername();
		if (file != null && file.length > 0) {

			for (int i = 0; i < file.length; i++) {
				System.out.println(i + ":" + "第" + i + "張照片");
				System.out.println("共" + (i + 1) + "張照片");
				MultipartFile savefile = file[i];
				uploadSelfie(savefile, username);
			}
			try {
				System.out.println("START　Write!");
				txt.getSelfiepath(SelfieFILEPATH, currentUser.getUsername());

				System.out.println("TRAIN!");

				engine.trainEngine();
				System.out.println("OVER!!!!!!!");

				File selfiefile = new File(SelfieFILEPATH + username + ".jpg");
				selfiefile.delete();
				System.out.println("DELETE SELFIE!");
				txt.deleteTxt(FILEPATH);
				System.out.println("DELeTE TXT");

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		return null;
	}

	// 訓練人臉圖
	@RequestMapping(value = "/trainFacePhoto", method = RequestMethod.POST)
	public UploadSelfieResponse trainFacePhoto(@RequestParam(value = "username") String username,@Valid @RequestBody DataRequest dataRequest) {
		System.out.println("!!!!!!!!!!!!!!");
		userRepository.findByUsername(username).map(user -> {
			return username;
		}).orElseThrow(() -> new BadRequestException("Username " + username + " not found"));

		if (dataRequest.getData() != null) {
			blob(dataRequest.getData(), username);

			try {
				System.out.println("START　Write!");
				txt.getFacepath(SelfieFILEPATH, username);
				System.out.println("TRAIN!");
				engine.trainEngine();
				System.out.println("OVER!!!!!!!");

				File selfiefile = new File(SelfieFILEPATH + username + ".jpg");
				selfiefile.delete();
				System.out.println("DELETE SELFIE!");
				txt.deleteTxt(FILEPATH);
				System.out.println("DELETE TXT");

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		return null;
	}

//修改頭貼

//透過使用者讀取頭貼
	@RequestMapping(value = "/myDownloadSelfie", method = RequestMethod.GET)
	public ResponseEntity<Resource> downloadSelfieByUsername(@CurrentUser UserPrincipal currentUser, Selfie selfie) {
		String username = currentUser.getUsername();
//		selfie = selfieRepository.findByUser(user);
		Optional<User> user = userRepository.findByUsername(username);
		selfie = user.get().getSelfie();
		System.out.println(username);
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(selfie.getSelfieType()))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; selfiename = \"" + selfie.getSelfieName() + "\"")
				.body(new ByteArrayResource(selfie.getSelfiedata()));
	}

	// 讀取頭貼photoData
	@GetMapping("/downloadMySelfie")
	public PhotoResponse getSelfie(@CurrentUser UserPrincipal currentUser, Selfie selfie) {
		Optional<User> user = userRepository.findByUsername(currentUser.getUsername());
		selfie = user.get().getSelfie();
		PhotoResponse photoResponse = new PhotoResponse(selfie.getId(), selfie.getSelfiedata());
		return photoResponse;
	}

}