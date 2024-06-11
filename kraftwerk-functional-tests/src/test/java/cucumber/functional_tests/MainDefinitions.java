package cucumber.functional_tests;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import fr.insee.kraftwerk.api.process.MainProcessing;
import fr.insee.kraftwerk.core.Constants;
import fr.insee.kraftwerk.core.KraftwerkError;
import fr.insee.kraftwerk.core.exceptions.KraftwerkException;
import fr.insee.kraftwerk.core.inputs.UserInputsFile;
import fr.insee.kraftwerk.core.metadata.MetadataModel;
import fr.insee.kraftwerk.core.metadata.MetadataUtils;
import fr.insee.kraftwerk.core.outputs.OutputFiles;
import fr.insee.kraftwerk.core.outputs.csv.CsvOutputFiles;
import fr.insee.kraftwerk.core.sequence.*;
import fr.insee.kraftwerk.core.utils.FileUtils;
import fr.insee.kraftwerk.core.utils.SqlUtils;
import fr.insee.kraftwerk.core.vtl.VtlBindings;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static cucumber.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

// Main example
public class MainDefinitions {

	Path inDirectory = Paths.get(FUNCTIONAL_TESTS_INPUT_DIRECTORY);
	static Path outDirectory = Paths.get(FUNCTIONAL_TESTS_OUTPUT_DIRECTORY);
	Path tempDirectory = Paths.get(FUNCTIONAL_TESTS_TEMP_DIRECTORY);
	UserInputsFile userInputs;
	String campaignName = "";
	VtlBindings vtlBindings = new VtlBindings();
	OutputFiles outputFiles;
	Map<String, MetadataModel> metadataModelMap;

	private ControlInputSequence controlInputSequence;
	List<KraftwerkError> errors = new ArrayList<>();
	static Connection database;

	@BeforeAll
	public static void clean() throws KraftwerkException, SQLException {
		FileUtils.deleteDirectory(outDirectory);
		database = SqlUtils.openConnection();
	}

	@Given("Step 0 : We have some survey in directory {string}")
	public void launch_all_steps(String campaignDirectoryName) {
		outDirectory = Paths.get(FUNCTIONAL_TESTS_OUTPUT_DIRECTORY);

		this.campaignName = campaignDirectoryName;
		inDirectory = inDirectory.resolve(campaignName);
		outDirectory = outDirectory.resolve(campaignName);
		controlInputSequence = new ControlInputSequence(inDirectory.toString());
		// We clean the output and the temp directory
		deleteDirectory(outDirectory.toFile());
		deleteDirectory(tempDirectory.toFile());
	}

	@Given("We have a test VTL script named {string} creating a variable {string} in dataset {string}")
	public void init_VTL_script(String vtlScriptName, String variableToCreate, String datasetName) throws IOException {
		String generatedVTL = datasetName + " := " + datasetName + " [calc " + variableToCreate + " := \"TEST\"];";

		Path vtlFolderPath = Files.createDirectories(Path.of(Constants.VTL_FOLDER_PATH).resolve("tcm"));
		Path vtlPath = vtlFolderPath.resolve(vtlScriptName + ".vtl");

		if(Files.exists(vtlPath)) //If file already exist (legitimate script) backup
			Files.copy(vtlPath,vtlFolderPath.resolve(vtlScriptName + ".bkp"));
		else
			Files.createFile(vtlPath);
		Files.write(vtlPath,generatedVTL.getBytes());
	}

	@When("Step 1 : We initialize the input files")
	public void initialize_input_files() throws KraftwerkException {
		System.out.println("InDirectory value : " + inDirectory);
		userInputs = controlInputSequence.getUserInputs(inDirectory);
		vtlBindings = new VtlBindings();
	}

	@When("Step 1 : We initialize with input file {string}")
	public void initialize_with_specific_input(String inputFileName) throws KraftwerkException {
		userInputs = new UserInputsFile(inDirectory.resolve(inputFileName), inDirectory);
		vtlBindings = new VtlBindings();
	}

	@When("Step 1 : We launch main service")
	public void launch_main() throws KraftwerkException {
		// We clean the output and the temp directory
		deleteDirectory(outDirectory.toFile());
		deleteDirectory(tempDirectory.toFile());
		MainProcessing mp = new MainProcessing(inDirectory.toString(), false, "defaultDirectory", 419430400L);
		mp.runMain();
	}

	@When("We launch main service 2 times")
	public void launch_main_2() throws KraftwerkException, InterruptedException {
		// We clean the output and the temp directory
		deleteDirectory(outDirectory.toFile());
		deleteDirectory(tempDirectory.toFile());
		MainProcessing mp = new MainProcessing(inDirectory.toString(), false, "defaultDirectory", 419430400L);
		mp.runMain();
		await().atMost(2, TimeUnit.SECONDS);
		mp = new MainProcessing(inDirectory.toString(), false, "defaultDirectory", 419430400L);
		mp.runMain();
	}

	@When("Step 1 : We launch main service file by file")
	public void launch_main_filebyfile() throws KraftwerkException {
		// We clean the output and the temp directory
		deleteDirectory(outDirectory.toFile());
		deleteDirectory(tempDirectory.toFile());
		MainProcessing mp = new MainProcessing(inDirectory.toString(), true,
				Paths.get(FUNCTIONAL_TESTS_INPUT_DIRECTORY).resolve(campaignName).toString(), 419430400L);
		mp.runMain();
	}

	@When("Step 2 : We get each unimodal dataset")
	public void unimodal_treatments() throws KraftwerkException, SQLException {
		try (Statement statement = database.createStatement()) {
			metadataModelMap = MetadataUtils.getMetadata(userInputs.getModeInputsMap());
			BuildBindingsSequence buildBindingsSequence = new BuildBindingsSequence(true);
			for (String dataMode : userInputs.getModeInputsMap().keySet()) {
				boolean withDDI = true;
				buildBindingsSequence.buildVtlBindings(userInputs, dataMode, vtlBindings, metadataModelMap.get(dataMode), withDDI, null, statement);
				UnimodalSequence unimodal = new UnimodalSequence();
				unimodal.applyUnimodalSequence(userInputs, dataMode, vtlBindings, errors, metadataModelMap);
			}
		}
	}

	@When("Step 3 : We aggregate each unimodal dataset into a multimodal dataset")
	public void aggregate_datasets() throws SQLException {
		try (Statement statement = database.createStatement()) {
			MultimodalSequence multimodalSequence = new MultimodalSequence();
			multimodalSequence.multimodalProcessing(userInputs, vtlBindings, errors, metadataModelMap, statement);
		}
	}

	@When("Step 4 : We export the final version")
	public void export_results() throws KraftwerkException, SQLException {
		try (Statement statement = database.createStatement()) {
			WriterSequence writerSequence = new WriterSequence();
			LocalDateTime localDateTime = LocalDateTime.now();
			writerSequence.writeOutputFiles(inDirectory, localDateTime, vtlBindings, userInputs.getModeInputsMap(), metadataModelMap, errors, statement);
			writeErrorsFile(inDirectory, localDateTime, errors);
			outputFiles = new CsvOutputFiles(outDirectory, vtlBindings, userInputs.getModes(), statement);
		}
	}

	@Then("Step 5 : We check if we have {int} lines")
	public void count_lines_in_root_tables(int expectedLineCount) throws CsvValidationException, IOException {
		// Go to first datetime folder
		Path executionOutDirectory = outDirectory.resolve(Objects.requireNonNull(new File(outDirectory.toString()).listFiles(File::isDirectory))[0].getName());

		Path filePath = outputFiles == null ?
				executionOutDirectory.resolve(inDirectory.getFileName() + "_" + Constants.ROOT_GROUP_NAME + ".csv")
				: executionOutDirectory.resolve(outputFiles.outputFileName(Constants.ROOT_GROUP_NAME));

		// Get reader to read the root table written in outputs
		System.out.println("Check output file path : " + filePath);
		CSVReader csvReader = getCSVReader(filePath);
		// Count
		int lineCount = 0;
		while ((csvReader.readNext()) != null) {
			lineCount++;
		}
		// Close reader
		csvReader.close();
		// Test
		assertEquals(expectedLineCount, lineCount);
	}

	@Then("Step 2 : We check root output file has {int} lines and {int} variables")
	public void check_csv_output_root_table(int expectedLineCount, int expectedVariablesCount) throws IOException, CsvValidationException {
		Path executionOutDirectory = outDirectory.resolve(Objects.requireNonNull(new File(outDirectory.toString()).listFiles(File::isDirectory))[0].getName());
		CSVReader csvReader = getCSVReader(
				executionOutDirectory.resolve(outDirectory.getFileName() + "_" + Constants.ROOT_GROUP_NAME + ".csv"));
		// get header
		String[] header = csvReader.readNext();
		// Count
		int variableCount = header.length;

		// Count
		int lineCount = 1;
		while ((csvReader.readNext()) != null) {
			lineCount++;
		}

		// Close reader
		csvReader.close();
		// Test
		assertEquals(expectedVariablesCount, variableCount);
		assertEquals(expectedLineCount, lineCount);

	}

	@Then("Step 2 : We check {string} output file has {int} lines and {int} variables")
	public void check_csv_output_loop_table(String loopName, int expectedLineCount, int expectedVariablesCount) throws IOException, CsvValidationException {
		Path executionOutDirectory = outDirectory.resolve(Objects.requireNonNull(new File(outDirectory.toString()).listFiles(File::isDirectory))[0].getName());
		CSVReader csvReader = getCSVReader(
				executionOutDirectory.resolve(outDirectory.getFileName() + "_" + loopName + ".csv"));
		// get header
		String[] header = csvReader.readNext();
		// Count
		int variableCount = header.length;

		// Count
		int lineCount = 1;
		while ((csvReader.readNext()) != null) {
			lineCount++;
		}

		// Close reader
		csvReader.close();
		// Test
		assertEquals(expectedVariablesCount, variableCount);
		assertEquals(expectedLineCount, lineCount);

	}

	@Then("Step 2 : We check root parquet output file has {int} lines and {int} variables")
	public void check_parquet_output_root_table(int expectedLineCount, int expectedVariablesCount) throws IOException, CsvValidationException, SQLException {
		Path executionOutDirectory = outDirectory.resolve(Objects.requireNonNull(new File(outDirectory.toString()).listFiles(File::isDirectory))[0].getName());
		Path filePath = executionOutDirectory.resolve(outDirectory.getFileName() + "_" + Constants.ROOT_GROUP_NAME + ".parquet");
		try (Statement statement = database.createStatement()) {
			SqlUtils.readParquetFile(statement, filePath);

			String tableName = filePath.getFileName().toString().split("\\.")[0];

			// Count number of variables
			Assertions.assertThat(SqlUtils.getColumnNames(statement,tableName)).hasSize(expectedVariablesCount);

			// Count lines
			ResultSet resultSet = statement.executeQuery(String.format("SELECT COUNT(*) FROM \"%s\"",tableName));
			Assertions.assertThat(resultSet.next()).isTrue();
			Assertions.assertThat(resultSet.getInt(1)).isEqualTo(expectedLineCount);
		}
	}

	@Then("We check {string} parquet output file has {int} lines and {int} variables")
	public void check_parquet_output_loop_table(String loopName, int expectedLineCount, int expectedVariablesCount) throws IOException, CsvValidationException, SQLException {
		Path executionOutDirectory = outDirectory.resolve(Objects.requireNonNull(new File(outDirectory.toString()).listFiles(File::isDirectory))[0].getName());
		Path filePath = executionOutDirectory.resolve(outDirectory.getFileName() + "_" + loopName + ".parquet");
		try (Statement statement = database.createStatement()) {
			SqlUtils.readParquetFile(statement, filePath);

			String tableName = filePath.getFileName().toString().split("\\.")[0];

			// Count number of variables
			Assertions.assertThat(SqlUtils.getColumnNames(statement,tableName)).hasSize(expectedVariablesCount);

			// Count lines
			ResultSet resultSet = statement.executeQuery(String.format("SELECT COUNT(*) FROM \"%s\"",tableName));
			Assertions.assertThat(resultSet.next()).isTrue();
			Assertions.assertThat(resultSet.getInt(1)).isEqualTo(expectedLineCount);
		}
	}

	@Then("Step 6 : We check if we have {int} variables")
	public void count_variables_in_root_tables(int expectedVariablesCount) throws CsvValidationException, IOException {
		// Go to first datetime folder
		Path executionOutDirectory = outDirectory.resolve(Objects.requireNonNull(new File(outDirectory.toString()).listFiles(File::isDirectory))[0].getName());

		Path filePath = outputFiles == null ?
				executionOutDirectory.resolve(inDirectory.getFileName() + "_" + Constants.ROOT_GROUP_NAME + ".csv")
				: executionOutDirectory.resolve(outputFiles.outputFileName(Constants.ROOT_GROUP_NAME));

		// Get reader to read the root table written in outputs
		System.out.println("Check output file path : " + filePath);
		CSVReader csvReader = getCSVReader(filePath);
		// get header
		String[] header = csvReader.readNext();
		// Count
		int variableCount = header.length;

		// Close reader
		csvReader.close();
		// Test
		assertEquals(expectedVariablesCount, variableCount);
	}

	@Then("Step 7 : We check that id {string} has value {string} for variable {string} in table {string}")
	public void checkVariableValue(String idUE, String expectedValue, String variable, String tableName)
			throws IOException, CsvValidationException {
		if (tableName == null || tableName.isEmpty())
			tableName = Constants.ROOT_GROUP_NAME;

		// Get reader to read the root table written in outputs
		CSVReader csvReader = getCSVReader(outputFiles.getOutputFolder().resolve(outputFiles.outputFileName(tableName)));
		// get header
		String[] header = csvReader.readNext();
		int idUEPosition = Arrays.asList(header).indexOf(Constants.ROOT_IDENTIFIER_NAME);
		int varPosition = Arrays.asList(header).indexOf(variable);

		// get the line corresponding to idUE
		String[] line;
		String value = null;
		while ((line = csvReader.readNext()) != null) {
			if (line[idUEPosition].equals(idUE))
				value = line[varPosition];
		}

		// Close reader
		csvReader.close();
		// Test
		assertEquals(expectedValue, value);
	}

	void deleteDirectory(File directoryToBeDeleted) {
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		directoryToBeDeleted.delete();
	}

	private void writeErrorsFile(Path inDirectory,LocalDateTime localDateTime, List<KraftwerkError> errors) {
		Path tempOutputPath = FileUtils.transformToOut(inDirectory,localDateTime).resolve(Constants.ERRORS_FILE_NAME);
		FileUtils.createDirectoryIfNotExist(tempOutputPath.getParent());

		// Write errors file
		if (!errors.isEmpty()) {
			try (FileWriter myWriter = new FileWriter(tempOutputPath.toFile(), true)) {
				for (KraftwerkError error : errors) {
					myWriter.write(error.toString());
				}
				System.out.println((String.format("Text file: %s successfully written", tempOutputPath)));
			} catch (IOException e) {
				System.out
						.println((String.format("Error occurred when trying to write text file: %s", tempOutputPath)));
			}
		} else {
			System.out.println("No error found during VTL transformations");
		}
	}

	@Then("In a file named {string} there should be a {string} field")
	public void check_field_existence(String fileName, String fieldName) throws IOException, CsvValidationException {
		// Go to first datetime folder
		Path executionOutDirectory = outDirectory.resolve(Objects.requireNonNull(new File(outDirectory.toString()).listFiles(File::isDirectory))[0].getName());

		File outputReportingDataFile = new File(executionOutDirectory + "/" + fileName);

		// File existence assertion
		assertThat(outputReportingDataFile).exists().isFile().canRead();

		CSVReader csvReader = getCSVReader(
				outputReportingDataFile.toPath()
		);


		// Get header
		String[] header = csvReader.readNext();
		csvReader.close();

		assertThat(header).isNotEmpty().contains(fieldName);
	}

	@When("We clean the test VTL script named {string}")
	public void clean_vtl(String vtlScriptName) throws IOException {
		Path vtlPath = Path.of(Constants.VTL_FOLDER_PATH).resolve("tcm").resolve(vtlScriptName + ".vtl");
		Path bkpPath = vtlPath.getParent().resolve(vtlScriptName + ".bkp");

		Files.deleteIfExists(vtlPath);

		if(Files.exists(bkpPath)) // Put backup back
			Files.copy(bkpPath,bkpPath.getParent().resolve(vtlScriptName + ".vtl"));

		Files.deleteIfExists(bkpPath);
	}

	//CSV Utilities
	public CSVReader getCSVReader(Path filePath) throws IOException {
		CSVParser parser = new CSVParserBuilder()
				.withSeparator(Constants.CSV_OUTPUTS_SEPARATOR)
				//.withQuoteChar(Constants.CSV_OUTPUTS_QUOTE_CHAR)
				//.withEscapeChar(CSVWriter.DEFAULT_ESCAPE_CHARACTER)
				.build();
		return new CSVReaderBuilder(new FileReader(filePath.toFile(), StandardCharsets.UTF_8))
				//.withSkipLines(1) // (uncomment to ignore header)
				.withCSVParser(parser)
				.build();
	}

	@Then("We check if there is only one header")
	public void uniqueHeaderCheck() throws IOException, CsvValidationException {
		// Go to first datetime folder
		Path executionOutDirectory = outDirectory.resolve(Objects.requireNonNull(new File(outDirectory.toString()).listFiles(File::isDirectory))[0].getName());

		Path filePath = outputFiles == null ?
				executionOutDirectory.resolve(inDirectory.getFileName() + "_" + Constants.ROOT_GROUP_NAME + ".csv")
				: executionOutDirectory.resolve(outputFiles.outputFileName(Constants.ROOT_GROUP_NAME));

		// Get reader to read the root table written in outputs
		System.out.println("Check output file path : " + filePath);
		CSVReader csvReader = getCSVReader(filePath);
		// get header
		String[] header = csvReader.readNext();
		String[] line;
		while((line = csvReader.readNext()) != null){
			Assertions.assertThat(line).isNotEqualTo(header);
		}
	}

    @Then("We check if the CSV format is correct")
    public void checkCSV() throws IOException {
		//The CSV format rules is the following :
		//for booleans : 0 for false, 1 for true
		//ALL data must be between double quotes

		// Go to first datetime folder
		Path executionOutDirectory = outDirectory.resolve(Objects.requireNonNull(new File(outDirectory.toString()).listFiles(File::isDirectory))[0].getName());

		Path filePath = outputFiles == null ?
				executionOutDirectory.resolve(inDirectory.getFileName() + "_" + Constants.ROOT_GROUP_NAME + ".csv")
				: executionOutDirectory.resolve(outputFiles.outputFileName(Constants.ROOT_GROUP_NAME));

		try(BufferedReader bufferedReader = Files.newBufferedReader(filePath)){
			String line = bufferedReader.readLine();
			//Check header
			String[] header = line.split(String.valueOf(Constants.CSV_OUTPUTS_SEPARATOR));
			//Check if line is split correctly
			Assertions.assertThat(header).hasSizeGreaterThan(1);
			for(String headerElement : header){
				//Check if element is between double quotes
				Assertions.assertThat(headerElement.charAt(0)).isEqualTo('"');
				Assertions.assertThat(headerElement.charAt(headerElement.length()-1)).isEqualTo('"');
			}

			//Check content
			while(line != null){
				String[] lineelements = line.split(String.valueOf(Constants.CSV_OUTPUTS_SEPARATOR));
				//Check if line is split correctly
				Assertions.assertThat(lineelements).hasSizeGreaterThan(1);
				//Check if no "true" or "false"
				Assertions.assertThat(lineelements).doesNotContain("\"false\"","\"true\"");
				for(String lineelement : lineelements){
					//Check if value is between double quotes
					//If value is NULL, must still be between double quotes
					Assertions.assertThat(lineelement).isNotEmpty();
					Assertions.assertThat(lineelement.charAt(0)).isEqualTo('"');
					Assertions.assertThat(lineelement.charAt(lineelement.length()-1)).isEqualTo('"');
				}
				line = bufferedReader.readLine();
			}
		}
    }


	@AfterAll
	public static void closeConnection() throws SQLException {
		database.close();
	}
}