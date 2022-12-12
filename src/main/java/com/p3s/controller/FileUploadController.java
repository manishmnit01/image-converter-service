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
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.IntStream;

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



		try {
			convertTiffForGroup4Compression(serverFile, dir.getAbsolutePath(), outputFileName);
		} catch (Exception e) {
			String outputFullPath = dir.getAbsolutePath() + File.separator + outputFileName;
			convertTiffForJpeg2000Compression(serverFile, outputFullPath);
		}

		String outputFullPath = dir.getAbsolutePath() + File.separator + outputFileName;
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

	private void convertTiffForGroup4Compression(File inputFile, String folderPath, String filePath) throws Exception {
		// Below two lines work for single page tiff image but not for multi page.
		/* BufferedImage inputTiffImage = ImageIO.read(inputFile);
		   ImageIO.write(inputTiffImage, "jpeg", new File(outputFullPath)); */

		try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputFile)) {
			Iterator<ImageReader> iterator = ImageIO.getImageReaders(imageInputStream);
			if (iterator == null || !iterator.hasNext()) {
				throw new RuntimeException("Image file format not supported by ImageIO");
			}

			// We are just looking for the first reader compatible:
			ImageReader reader = iterator.next();
			reader.setInput(imageInputStream);
			int numPage = reader.getNumImages(true);

			IntStream.range(0, numPage).forEach(imageIndex -> {
				try {
					final BufferedImage inputTiffImage = reader.read(imageIndex);
					final String outputMultiPagePath = (imageIndex+1) + "_" + filePath;
					final String finalPath = folderPath + File.separator + outputMultiPagePath;

					ImageIO.write(inputTiffImage, "jpeg", new File(finalPath));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	private void convertTiffForJpeg2000Compression(File inputFile, String outputFullPath) throws Exception {
		String inputPath = inputFile.getAbsolutePath();
		FileConvert converter = new FileConvert(inputPath, outputFullPath);
		converter.convert();
	}
}
