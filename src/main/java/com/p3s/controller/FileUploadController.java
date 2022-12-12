package com.p3s.controller;

import com.p3s.service.FileConvert;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.*;

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
	public void uploadFileHandler(HttpServletResponse response, @RequestParam("file") MultipartFile inputFile) throws Exception {

		if(inputFile == null || inputFile.isEmpty()) {
			throw new RuntimeException("Input file is empty.");
		}

		// Creating the directory to store file
		String rootPath = System.getProperty("catalina.home");
		File dir = new File(rootPath + File.separator + "tmpFiles");
		if (!dir.exists()) {
			dir.mkdirs();
		}

		String inputFileName = inputFile.getOriginalFilename();

		File serverFile = new File(dir.getAbsolutePath() + File.separator + inputFileName);
		FileCopyUtils.copy(inputFile.getBytes(), serverFile);

		String fileNameWithoutExtension = FilenameUtils.removeExtension(inputFileName);
		String outputFileName = fileNameWithoutExtension + ".jpeg";
		String outputFullPath = dir.getAbsolutePath() + File.separator + outputFileName;

		List<String> outputFileNames;
		try {
			outputFileNames = convertTiffForGroup4Compression(serverFile, dir.getAbsolutePath(), outputFileName);
		} catch (Exception e) {
			outputFileNames = convertTiffForJpeg2000Compression(serverFile, outputFullPath);
		}

		response.setContentType("application/octet-stream");
		response.setStatus(HttpServletResponse.SC_OK);

		if(outputFileNames.size() == 1) {
			// Download single image file.
			response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + outputFileName);
			InputStream inputStream = Files.newInputStream(Paths.get(outputFileNames.get(0)));
			StreamUtils.copy(inputStream, response.getOutputStream());
		}
		else {
			// Download multiple image files in a zip file.
			String zipFileName = fileNameWithoutExtension + ".zip";
			response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + zipFileName);
			try (ZipOutputStream zippedOut = new ZipOutputStream(response.getOutputStream())) {
				for (String file : outputFileNames) {
					FileSystemResource resource = new FileSystemResource(file);
					ZipEntry e = new ZipEntry(resource.getFilename());
					e.setSize(resource.contentLength());
					e.setTime(System.currentTimeMillis());
					zippedOut.putNextEntry(e);
					// And the content of the resource:
					StreamUtils.copy(resource.getInputStream(), zippedOut);
					zippedOut.closeEntry();
				}
				zippedOut.finish();
			}
		}

		response.flushBuffer();
		FileUtils.cleanDirectory(dir);
	}

	private List<String> convertTiffForGroup4Compression(File inputFile, String folderPath, String filePath) throws Exception {
		// Below two lines work for single page tiff image but not for multi page.
		/* BufferedImage inputTiffImage = ImageIO.read(inputFile);
		   ImageIO.write(inputTiffImage, "jpeg", new File(outputFullPath)); */

		List<String> outputFileNames = new ArrayList<>();
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
					outputFileNames.add(finalPath);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
		return outputFileNames;
	}

	private List<String> convertTiffForJpeg2000Compression(File inputFile, String outputFullPath) throws Exception {
		String inputPath = inputFile.getAbsolutePath();
		FileConvert converter = new FileConvert(inputPath, outputFullPath);
		converter.convert();
		return Collections.singletonList(outputFullPath);
	}
}
