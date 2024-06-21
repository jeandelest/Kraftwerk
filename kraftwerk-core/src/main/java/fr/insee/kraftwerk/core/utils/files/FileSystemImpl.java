package fr.insee.kraftwerk.core.utils.files;

import fr.insee.kraftwerk.core.exceptions.KraftwerkException;
import fr.insee.kraftwerk.core.inputs.ModeInputs;
import fr.insee.kraftwerk.core.inputs.UserInputs;
import fr.insee.kraftwerk.core.inputs.UserInputsFile;
import fr.insee.kraftwerk.core.utils.DateUtils;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Log4j2
@NoArgsConstructor
public class FileSystemImpl implements FileUtilsInterface{

	private static final String ARCHIVE = "Archive";
	

	
	/**
	 * Move the input file to another directory to archive it
	 */
	@Override
	public void renameInputFile(Path inDirectory) {
		File file = inDirectory.resolve("kraftwerk.json").toFile();
		String fileWithTime = "kraftwerk-" + DateUtils.getCurrentTimeStamp() + ".json";
		File file2 = inDirectory.resolve(fileWithTime).toFile();
		if (file2.exists()) {
			log.warn(String.format("Trying to rename '%s' to '%s', but second file already exists.", file, file2));
			log.warn("Timestamped input file will be over-written.");
			try {
				Files.delete(file2.toPath());
			} catch (IOException e) {
				log.error("Can't delete file {}, IOException : {}", fileWithTime, e.getMessage());
			}
		}
		if(!file.renameTo(file2)){
			log.error("Can't rename file {} to {}", file.getName(), file2.getName());
		}
	}

	@Override
	public void archiveInputFiles(UserInputsFile userInputsFile) throws KraftwerkException {
		//
		Path inputFolder = userInputsFile.getInputDirectory();
		String[] directories = inputFolder.toString().split(Pattern.quote(File.separator));
		String campaignName = directories[directories.length - 1];

		createArchiveDirectoryIfNotExists(inputFolder);

		//
		for (String mode : userInputsFile.getModes()) {
			ModeInputs modeInputs = userInputsFile.getModeInputs(mode);
			archiveData(inputFolder, campaignName, modeInputs);
			archiveParadata(inputFolder, campaignName, modeInputs);
			archiveReportingData(inputFolder, campaignName, modeInputs);
		}
	}

	/**
	 *  If reporting data, we move reporting data files
	 * @param inputFolder
	 * @param campaignName
	 * @param modeInputs
	 * @throws KraftwerkException
	 */
	private void archiveReportingData(Path inputFolder, String campaignName, ModeInputs modeInputs)
			throws KraftwerkException {
		if (modeInputs.getReportingDataFile() != null) {
			moveDirectory(modeInputs.getReportingDataFile().toFile(), inputFolder.resolve(ARCHIVE)
					.resolve(getRoot(modeInputs.getReportingDataFile(), campaignName)).toFile());
		}
	}

	/**
	 * If paradata, we move the paradata folder
	 * @param inputFolder
	 * @param campaignName
	 * @param modeInputs
	 * @throws KraftwerkException
	 */
	private void archiveParadata(Path inputFolder, String campaignName, ModeInputs modeInputs)
			throws KraftwerkException {
		if (modeInputs.getParadataFolder() != null) {
			moveDirectory(modeInputs.getParadataFolder().toFile(), inputFolder.resolve(ARCHIVE)
					.resolve(getRoot(modeInputs.getParadataFolder(), campaignName)).toFile());
		}
	}

	private void archiveData(Path inputFolder, String campaignName, ModeInputs modeInputs)
			throws KraftwerkException {
		Path dataPath = modeInputs.getDataFile();
		Path newDataPath = inputFolder.resolve(ARCHIVE).resolve(getRoot(dataPath, campaignName));

		if (!Files.exists(newDataPath)) {
			new File(newDataPath.getParent().toString()).mkdirs();
		}
		if (Files.isRegularFile(dataPath)) {
			moveFile(dataPath, newDataPath);
		} else if (Files.isDirectory(dataPath)) {
			moveDirectory(dataPath.toFile(),newDataPath.toFile());
		} else {
			log.debug(String.format("No file or directory at path: %s", dataPath));
		}
	}

	private void createArchiveDirectoryIfNotExists(Path inputFolder) {
		if (!Files.exists(inputFolder.resolve(ARCHIVE))) {
			inputFolder.resolve(ARCHIVE).toFile().mkdir();
		}
	}

	private void moveFile(Path dataPath, Path newDataPath) throws KraftwerkException {
		try {
			Files.move(dataPath, newDataPath);
		} catch (IOException e) {
			throw new KraftwerkException(500, "Can't move file " + dataPath + " to " + newDataPath);
		}
	}


	private void moveDirectory(File sourceFile, File destFile) throws KraftwerkException {
		if (sourceFile.isDirectory()) {
			File[] files = sourceFile.listFiles();
			assert files != null : "List of files in sourceFile is null";
			for (File file : files)
				moveDirectory(file, new File(destFile, file.getName()));
			try {
				Files.delete(sourceFile.toPath());
			} catch (IOException e) {
				throw new KraftwerkException(500, "Can't delete " + sourceFile + " - IOException : " + e.getMessage());
			}
		} else {
			if (!destFile.getParentFile().exists() && !destFile.getParentFile().mkdirs())
				throw new KraftwerkException(500, "Can't create directory to archive");
			if (!sourceFile.renameTo(destFile))
				throw new KraftwerkException(500, "Can't rename file " + sourceFile + " to " + destFile);
		}
	}
	
	private String getRoot(Path path, String campaignName) {
		String[] directories = path.toString().split(Pattern.quote(File.separator));
		int campaignIndex = Arrays.asList(directories).indexOf(campaignName);
		String[] newDirectories = Arrays.copyOfRange(directories, campaignIndex + 1, directories.length);
		StringBuilder result = new StringBuilder();
		String sep = "";
		for (String directory : newDirectories) {
			result.append(sep).append(directory);
			sep = File.separator;
		}
		return result.toString();
	}

	@Override
	public void deleteDirectory(Path directoryPath) throws KraftwerkException {
		try {
			org.springframework.util.FileSystemUtils.deleteRecursively(directoryPath);
		} catch (IOException e) {
			throw new KraftwerkException(500, "IOException when deleting temp folder : "+e.getMessage());
		}
	}

	/**
	 * List the files in the directory
	 * @param dir
	 * @return
	 */
	@Override
	public List<String> listFiles(String dir) {
		return Stream.of(new File(dir).listFiles())
				.filter(file -> !file.isDirectory())
				.map(File::getName)
				.toList();
	}

	@Override
	public Path getTempVtlFilePath(UserInputs userInputs, String step, String dataset) {
		createDirectoryIfNotExist(FileUtilsInterface.transformToTemp(userInputs.getInputDirectory()));
		return FileUtilsInterface.transformToTemp(userInputs.getInputDirectory()).resolve(step+ dataset+".vtl");
	}
	
	public static void createDirectoryIfNotExist(Path path) {
		try {
			Files.createDirectories(path);
			log.info(String.format("Created folder: %s", path.toFile().getAbsolutePath()));
		} catch (IOException e) {
			log.error("Permission refused to create folder: " + path.getParent(), e);
		}
	}

	@Override
	public Path convertToPath(String userField, Path inputDirectory) throws KraftwerkException {
		if (userField != null && !"null".equals(userField) && !userField.isEmpty()) {
			Path inputPath = inputDirectory.resolve(userField);
			if (!new File(inputPath.toUri()).exists()) {
				throw new KraftwerkException(400, String.format("The input folder \"%s\" does not exist in \"%s\".", userField, inputDirectory));
			}
			return inputPath;
		} else {
			return null;
		}
	}

	@Override
	public URL convertToUrl(String userField, Path inputDirectory) {
		if (userField == null) {
			log.debug("null value out of method that reads DDI field (should not happen).");
			return null;
		}
		try {
			if (userField.startsWith("http")) {
				return new URI(userField).toURL();
			}
			return inputDirectory.resolve(userField).toFile().toURI().toURL();
		} catch (MalformedURLException | URISyntaxException e) {
			log.error("Unable to convert URL from user input: " + userField);
			return null;
		} 
	}

	@Override
	@Nullable
	public Boolean isDirectory(String path) {
		if(!Files.isRegularFile(Path.of(path))){
			return null;
		}
		File file = new File(path);
		return file.isDirectory();
	}

	@Override
	public long getSizeOf(String path) {
		File file = new File(path);
		return FileUtils.sizeOf(file);
	}

	@Override
	public void writeFile(String path, String toWrite, boolean replace) {
		StandardOpenOption standardOpenOption = replace ? StandardOpenOption.TRUNCATE_EXISTING : StandardOpenOption.APPEND;
		try {
			Files.write(Path.of(path), toWrite.getBytes(), standardOpenOption);
		}catch (IOException e){
			log.error(e.toString());
		}
	}
}
