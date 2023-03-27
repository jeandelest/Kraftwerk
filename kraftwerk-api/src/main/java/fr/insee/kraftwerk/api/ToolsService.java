package fr.insee.kraftwerk.api;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.opencsv.exceptions.CsvException;

import fr.insee.kraftwerk.core.utils.CsvUtils;
import fr.insee.kraftwerk.core.vtl.VtlExecute;
import fr.insee.vtl.model.Dataset;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController("/tools")
@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Success"),
		@ApiResponse(responseCode = "400", description = "Bad Request"),
		@ApiResponse(responseCode = "500", description = "Internal server error") })
public class ToolsService {

	private static final String CSV_PATH_EXAMPLE = "in/PCS2020/myfile.csv";
	private static final String JSON = ".json";
	
	@Value("${fr.insee.postcollecte.files}")
	private String defaultDirectory;
	
	
	VtlExecute vtlExecute;

	@PostConstruct
	public void initializeWithProperties() {
		vtlExecute = new VtlExecute();
	}


	@PutMapping(value = "/csvToDataset")
	@Operation(operationId = "csvToDataset", summary = "${summary.tools.csvToDataset}", description = "${description.tools.csvToDataset}")
	public ResponseEntity<String> csvToDataset(
			@Parameter(description = "${param.csvPath}", required = true, example = CSV_PATH_EXAMPLE) @RequestBody String csvPathParam,
			@Parameter(description = "${param.nbIdentifiers}", required = true, example = "2") @RequestParam int nbIdentifiersParam

			) throws IOException, CsvException   {
		//Read data files
		Path csvPath = Path.of(defaultDirectory, csvPathParam);
		
		//Process
		Dataset outputDataset = CsvUtils.getDatasetFromCsv(csvPath,nbIdentifiersParam);
	
		//Write the ouput
		Path jsonPath = Path.of(defaultDirectory, csvPathParam+ JSON);
		log.info("Write dataset in", jsonPath);

		vtlExecute.writeJsonDataset(jsonPath, outputDataset);
		log.info("End to write dataset ");
		
		return ResponseEntity.ok(csvPathParam);


	}
	

}