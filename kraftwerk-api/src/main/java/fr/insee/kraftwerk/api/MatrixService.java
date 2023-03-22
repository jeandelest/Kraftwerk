package fr.insee.kraftwerk.api;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import fr.insee.kraftwerk.core.utils.CsvUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController("/matrix")
@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Success"),
		@ApiResponse(responseCode = "400", description = "Bad Request"),
		@ApiResponse(responseCode = "500", description = "Internal server error") })
public class MatrixService {

	private static final String MATRIX_NAME_EXAMPLE = "PCS2020";

	private static final String CSV = ".csv";
	
	@Value("${fr.insee.postcollecte.files}")
	private String defaultDirectory;
	
	private String matrixDirectory;
	

	@PostConstruct
	public void initializeWithProperties() {
		matrixDirectory = defaultDirectory.concat("/matrix");
	}


	@PutMapping(value = "/flat")
	@Operation(operationId = "flatMatrix", summary = "${summary.flatMatrix}", description = "${description.flatMatrix}")
	public ResponseEntity<String> buildVtlBindings(
			@Parameter(description = "${param.matrixName}", required = true, example = MATRIX_NAME_EXAMPLE) @RequestBody String matrixNameParam
			) throws CsvValidationException, IOException   {
		//Read data files
		Path matrixPath = Path.of(matrixDirectory, matrixNameParam+CSV);
		Path flatMatrixPath = Path.of(matrixDirectory, "flat"+matrixNameParam+CSV);

		
		//Process
		List<String[]> flatMatrix = readLineByLine(matrixPath);
		writeAllLines(flatMatrix, flatMatrixPath);
		return ResponseEntity.ok(matrixNameParam);


	}
	
	public List<String[]> readLineByLine(Path filePath) throws IOException, CsvValidationException   {
	    List<String[]> list = new ArrayList<>();
	    String[] variables = null;
	        try (CSVReader csvReader = CsvUtils.getReader(filePath)) {
	            String[] line;
	            while ((line = csvReader.readNext()) != null) {
	            	if (csvReader.getLinesRead() ==1) { //HEADER
	            		log.info(line[0]);
	            		variables = Arrays.copyOfRange(line, 1, line.length);
	            		String[] newHeaders = {"libelle","statut","code"};
	            		list.add(newHeaders);
	            	}else {
	            		if (variables != null) {
	            		for (int i=0;i<variables.length;i++) {
							String[] newLine = {line[0],variables[i],line[i+1]};
							list.add(newLine);
						}
	            		}
	            	}
	            }
	        
	    }
	    return list;
	}

	public void writeAllLines(List<String[]> lines, Path path) throws IOException {
	    try (CSVWriter writer = new CSVWriter(new FileWriter(path.toString()))) {
	        writer.writeAll(lines);
	    }
	}

}