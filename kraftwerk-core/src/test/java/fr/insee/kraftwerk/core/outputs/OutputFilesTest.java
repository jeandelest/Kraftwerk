package fr.insee.kraftwerk.core.outputs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import fr.insee.kraftwerk.core.Constants;
import fr.insee.kraftwerk.core.TestConstants;
import fr.insee.kraftwerk.core.exceptions.KraftwerkException;
import fr.insee.kraftwerk.core.inputs.ModeInputs;
import fr.insee.kraftwerk.core.inputs.UserInputs;
import fr.insee.kraftwerk.core.utils.FileUtils;
import fr.insee.kraftwerk.core.vtl.VtlBindings;
import fr.insee.vtl.model.Dataset;
import fr.insee.vtl.model.InMemoryDataset;
import fr.insee.vtl.model.Structured;

@TestMethodOrder(OrderAnnotation.class)
class OutputFilesTest {

	private static UserInputs testUserInputs;
	private static OutputFiles outputFiles;

	Dataset fooDataset = new InMemoryDataset(List.of(),
			List.of(new Structured.Component("FOO", String.class, Dataset.Role.IDENTIFIER)));

	@Test
	@Order(1)
	void createInstance() {
		assertDoesNotThrow(() -> {
			//
			testUserInputs = new UserInputs(
					Path.of(TestConstants.UNIT_TESTS_DIRECTORY, "user_inputs/inputs_valid_several_modes.json"),
					Path.of(TestConstants.UNIT_TESTS_DIRECTORY));
			//
			VtlBindings vtlBindings = new VtlBindings();
			for (String mode : testUserInputs.getModes()) {
				vtlBindings.put(mode, fooDataset);
			}
			vtlBindings.put(testUserInputs.getMultimodeDatasetName(), fooDataset);
			vtlBindings.put(Constants.ROOT_GROUP_NAME, fooDataset);
			vtlBindings.put("LOOP", fooDataset);
			vtlBindings.put("FROM_USER", fooDataset);
			//
			outputFiles = new OutputFiles(Paths.get(TestConstants.UNIT_TESTS_DUMP), vtlBindings, testUserInputs);
		});
	}

	@Test
	@Order(2)
	void testGetDatasetOutputNames() {
		//
		Set<String> outputDatasetNames = outputFiles.getDatasetToCreate();

		//
		for (String mode : testUserInputs.getModes()) {
			assertFalse(outputDatasetNames.contains(mode));
		}
		assertFalse(outputDatasetNames.contains(testUserInputs.getMultimodeDatasetName()));
		assertTrue(outputDatasetNames.containsAll(Set.of(Constants.ROOT_GROUP_NAME, "LOOP", "FROM_USER")));
	}

	@Test
	@Order(3)
	@Disabled
	void moveFiles() throws KraftwerkException {
		String campaignName = "move_files";
		//
		testUserInputs = new UserInputs(
				Path.of(TestConstants.UNIT_TESTS_DIRECTORY, campaignName, "move_files.json"),
				Path.of(TestConstants.UNIT_TESTS_DIRECTORY, campaignName));
		Path inputFolder = testUserInputs.getInputDirectory();

		Map<String, ModeInputs> modeInputsMap = testUserInputs.getModeInputsMap();

		for (String mode : modeInputsMap.keySet()) {
			// We create the mode files
			ModeInputs modeInputs = testUserInputs.getModeInputs(mode);

			String nameNewFile = modeInputs.getDataFile().toString();
			try {
				new File(nameNewFile).createNewFile();
				// Now the paradata
				if (modeInputs.getParadataFolder() != null && !modeInputs.getParadataFolder().toString().contentEquals("")) {
					Files.createDirectories(Paths.get(inputFolder + "/paradata"));
					new File(Constants.getResourceAbsolutePath(inputFolder + "/paradata/L0000003.json")).createNewFile();
					new File(Constants.getResourceAbsolutePath(inputFolder + "/paradata/L0000004.json")).createNewFile();
					new File(Constants.getResourceAbsolutePath(inputFolder + "/paradata/L0000009.json")).createNewFile();
					new File(Constants.getResourceAbsolutePath(inputFolder + "/paradata/L0000010.json")).createNewFile();
				}
				if (modeInputs.getReportingDataFile() != null && !modeInputs.getReportingDataFile().toString().contentEquals("")) {
					Files.createDirectories(Paths.get(inputFolder + "/suivi"));
					new File(Constants.getResourceAbsolutePath(inputFolder + "/suivi/reportingdata.xml")).createNewFile();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		FileUtils.moveInputFiles(testUserInputs);
		assertTrue(new File(TestConstants.UNIT_TESTS_DIRECTORY + "/" + campaignName + "/Archive/papier").exists());
		assertTrue(new File(TestConstants.UNIT_TESTS_DIRECTORY + "/" + campaignName + "/Archive/web").exists());
		assertTrue(
				new File(TestConstants.UNIT_TESTS_DIRECTORY + "/" + campaignName + "/Archive/paradata/L0000010.json").exists());
		assertTrue(
				new File(TestConstants.UNIT_TESTS_DIRECTORY + "/" + campaignName + "/Archive/suivi/reportingdata.xml").exists());

		deleteDirectory(new File(TestConstants.UNIT_TESTS_DIRECTORY + "/" + campaignName + "/Archive"));
		deleteDirectory(new File(TestConstants.UNIT_TESTS_DIRECTORY + "/" + campaignName + "/paradata"));
		deleteDirectory(new File(TestConstants.UNIT_TESTS_DIRECTORY + "/" + campaignName + "/suivi"));
	}
	
	
	boolean deleteDirectory(File directoryToBeDeleted) {
	    File[] allContents = directoryToBeDeleted.listFiles();
	    if (allContents != null) {
	        for (File file : allContents) {
	            deleteDirectory(file);
	        }
	    }
	    return directoryToBeDeleted.delete();
	}
}
