package com.p3s.controller;

import com.p3s.service.FileConvert;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class FileUploadController {

	@RequestMapping(value = "/upload", method = RequestMethod.GET)
	public String uploadFilePage() {
		return "upload";
	}

	/**
	 * Upload single file using Spring Controller
	 */
	@RequestMapping(value = "/uploadFile", method = RequestMethod.POST)
	public @ResponseBody
	ResponseEntity<ByteArrayResource> uploadFileHandler(@RequestParam("file") MultipartFile inputFile) throws Exception {

		if(inputFile == null || inputFile.isEmpty()) {
			throw new RuntimeException("Input file is empty.");
		}

		byte[] bytes = inputFile.getBytes();

		// Creating the directory to store file
		String rootPath = System.getProperty("catalina.home");
		File dir = new File(rootPath + File.separator + "tmpFiles");
		if (!dir.exists()) {
			dir.mkdirs();
		}

		String inputFileName = inputFile.getOriginalFilename();

		File serverFile = new File(dir.getAbsolutePath()
				+ File.separator + inputFileName);
		BufferedOutputStream stream = new BufferedOutputStream(
				new FileOutputStream(serverFile));
		stream.write(bytes);
		stream.close();

		String outputFileName = FilenameUtils.removeExtension(inputFileName) + ".jpeg";

		String outputFullPath = dir.getAbsolutePath() + File.separator + outputFileName;

		try {
			convertTiffForGroup4Compression(serverFile, outputFullPath);
		} catch (Exception e) {
			convertTiffForJpeg2000Compression(serverFile, outputFullPath);
		}

		Path path = Paths.get(outputFullPath);
		byte[] data = Files.readAllBytes(path);
		ByteArrayResource resource = new ByteArrayResource(data);

		FileUtils.cleanDirectory(dir);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment;filename="+outputFileName)
				.contentLength(data.length)
				.contentType(MediaType.parseMediaType("application/octet-stream"))
				.body(resource);
	}

	private void convertTiffForGroup4Compression(File inputFile, String outputFullPath) throws Exception {
		try {
			BufferedImage inputTiffImage = ImageIO.read(inputFile);
			ImageIO.write(inputTiffImage, "jpeg", new File(outputFullPath));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void convertTiffForJpeg2000Compression(File inputFile, String outputFullPath) throws Exception {
		String inputPath = inputFile.getAbsolutePath();
		FileConvert converter = new FileConvert(inputPath, outputFullPath);
		converter.convert();
	}
}
