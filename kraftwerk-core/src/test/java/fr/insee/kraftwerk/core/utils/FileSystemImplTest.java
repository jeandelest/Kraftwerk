package fr.insee.kraftwerk.core.utils;

import fr.insee.kraftwerk.core.Constants;
import fr.insee.kraftwerk.core.TestConstants;
import fr.insee.kraftwerk.core.exceptions.KraftwerkException;
import fr.insee.kraftwerk.core.inputs.UserInputsFile;
import fr.insee.kraftwerk.core.utils.files.FileSystemImpl;
import fr.insee.kraftwerk.core.utils.files.FileUtilsInterface;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FileSystemImplTest {
	private static FileUtilsInterface fileUtilsInterface;

	@BeforeAll
    static void init(){
		fileUtilsInterface = new FileSystemImpl();
	}

    
    @Test
    void testTransformToOut() {
        assertEquals(Paths.get("C://Users/in/Kraftwerk/src/test/resources/functional_tests/out/VQS"),
				FileUtilsInterface.transformToOut(Paths.get("C://Users/in/Kraftwerk/src/test/resources/functional_tests/in/VQS")));
    }

	@Test
	void testTransformToOut2() {
		Path path = Paths.get("C://Users/in/Kraftwerk/src/test/resources/functional_tests/out/VQS");
		assertEquals(path, FileUtilsInterface.transformToOut(Paths.get("C://Users/in/Kraftwerk/src/test/resources/functional_tests/in/VQS"), LocalDateTime.now()).getParent());
	}
    
    @Test
    void archiveInputFiles_failWhenNull() {
		assertThrows(NullPointerException.class, () -> fileUtilsInterface.archiveInputFiles(null));
    }  
    
    
	@Test
	void archiveInputFiles_ok() throws IOException, KraftwerkException{
			
		//GIVEN
		String campaignName = "move_files";
		Path inputDirectory = Path.of(TestConstants.UNIT_TESTS_DIRECTORY, campaignName, "execute");
		org.springframework.util.FileSystemUtils.deleteRecursively(inputDirectory);
		Files.createDirectories(inputDirectory);
		Files.copy(Path.of(TestConstants.UNIT_TESTS_DIRECTORY, campaignName,"move_files.json"), 
				Path.of(inputDirectory.toString(),"move_files.json"));
		
		//WEB
		Path webDirectory = Paths.get(inputDirectory.toString(), "web");
		Files.createDirectories(webDirectory);
		new File(webDirectory+"/web.xml").createNewFile();
		new File(webDirectory+"/vqs-2021-x00-xforms-ddi.xml").createNewFile();
		new File(webDirectory+"/WEB.vtl").createNewFile();

		//PAPER
		Path paperDirectory =  Paths.get(inputDirectory.toString(), "papier");
		Files.createDirectories(paperDirectory);
		new File(paperDirectory+"/paper.txt").createNewFile();
		new File(paperDirectory+"/vqs-2021-x00-fo-ddi.xml").createNewFile();
		
		//Reporting
		Path reportingDirectory =  Paths.get(inputDirectory.toString(), "suivi");
		Files.createDirectories(reportingDirectory);
		new File(reportingDirectory+"/reportingdata.xml").createNewFile();
		
		//Paradata
		Path paradataDirectory =  Paths.get(inputDirectory.toString(), "paradata");
		Files.createDirectories(paradataDirectory);
		new File(Constants.getResourceAbsolutePath(paradataDirectory +"/L0000003.json")).createNewFile();
		new File(Constants.getResourceAbsolutePath(paradataDirectory +"/L0000004.json")).createNewFile();
		new File(Constants.getResourceAbsolutePath(paradataDirectory +"/L0000009.json")).createNewFile();
		new File(Constants.getResourceAbsolutePath(paradataDirectory +"/L0000010.json")).createNewFile();

		UserInputsFile testUserInputsFile = new UserInputsFile(Path.of(inputDirectory.toString(), "move_files.json"),inputDirectory, fileUtilsInterface);


		//WHEN
		fileUtilsInterface.archiveInputFiles(testUserInputsFile);
		
		//THEN
		Assertions.assertTrue(new File(inputDirectory.toString() + "/Archive/papier").exists());
		Assertions.assertTrue(new File(inputDirectory.toString() + "/Archive/web").exists());
		Assertions.assertTrue(new File(inputDirectory.toString() + "/Archive/paradata/L0000010.json").exists());
		Assertions.assertTrue(new File(inputDirectory.toString() + "/Archive/suivi/reportingdata.xml").exists());

		//CLEAN
		org.springframework.util.FileSystemUtils.deleteRecursively(inputDirectory);
	}


}
