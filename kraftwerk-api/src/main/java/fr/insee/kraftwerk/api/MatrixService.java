package fr.insee.kraftwerk.api;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;

import fr.insee.kraftwerk.core.outputs.OutputFiles;
import fr.insee.kraftwerk.core.utils.CsvUtils;
import fr.insee.kraftwerk.core.vtl.ErrorVtlTransformation;
import fr.insee.kraftwerk.core.vtl.VtlBindings;
import fr.insee.kraftwerk.core.vtl.VtlExecute;
import fr.insee.vtl.model.Dataset;
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
	private static final String JSON = ".json";
	
	@Value("${fr.insee.postcollecte.files}")
	private String defaultDirectory;
	
	private String matrixDirectory;
	
	VtlExecute vtlExecute;

	@PostConstruct
	public void initializeWithProperties() {
		matrixDirectory = defaultDirectory.concat("/matrix");
		vtlExecute = new VtlExecute();
	}


	@PutMapping(value = "/flat")
	@Operation(operationId = "flatMatrix", summary = "${summary.flatMatrix}", description = "${description.flatMatrix}")
	public ResponseEntity<String> flatMatrix(
			@Parameter(description = "${param.matrixName}", required = true, example = MATRIX_NAME_EXAMPLE) @RequestBody String matrixNameParam
			) throws IOException, CsvException   {
		//Read data files
		Path matrixPath = Path.of(matrixDirectory, matrixNameParam+CSV);
		Path flatMatrixPath = Path.of(matrixDirectory, "flat"+matrixNameParam+CSV);

		
		//Process
		List<String[]> flatMatrix = getFlatCsv(matrixPath);
		writeAllLinesInCsv(flatMatrix, flatMatrixPath);
		log.info("Start to create dataset");
		Dataset matrixDataset = CsvUtils.getDatasetFromCsv(flatMatrixPath,2);
		log.info("End to create dataset {}", matrixDataset);
		Path matrixDatasetPath = Path.of(matrixDirectory, "flat"+matrixNameParam+JSON);
		log.info("Write dataset in", matrixDatasetPath);

		vtlExecute.writeJsonDataset(matrixDatasetPath, matrixDataset);
		log.info("End to write dataset ");
		
		VtlBindings vtlBindings = new VtlBindings();
		vtlExecute.putVtlDataset(matrixDatasetPath.toString(), matrixNameParam, vtlBindings);
		List<ErrorVtlTransformation> errors = new ArrayList<>();
		vtlExecute.evalVtlScript("", vtlBindings, errors );
		errors.forEach(e -> log.error(e.toString()));
		OutputFiles of = new OutputFiles(Path.of(matrixDirectory), vtlBindings, matrixNameParam);
		of.writeOutputCsvTables();
		return ResponseEntity.ok(matrixNameParam);


	}
	
	@PutMapping(value = "/2")
	@Operation(operationId = "2", summary = "${summary.2}", description = "${description.2}")
	public ResponseEntity<String> methodToRename(
			@Parameter(description = "${param.matrixName}", required = true, example = MATRIX_NAME_EXAMPLE) @RequestBody String matrixNameParam
			)    {
		//Read data files
		Path matrixDatasetPath = Path.of(matrixDirectory, "flat"+matrixNameParam+JSON);
		
		//Process
		VtlBindings vtlBindings = new VtlBindings();
		vtlExecute.putVtlDataset(matrixDatasetPath.toString(), matrixNameParam, vtlBindings);
		log.debug("Size of dataset before VTL script: {}", vtlBindings.getDataset(matrixNameParam).getDataPoints().size());
		List<ErrorVtlTransformation> errors = new ArrayList<>();
		vtlExecute.evalVtlScript("", vtlBindings, errors );
		errors.forEach(e -> log.error(e.toString()));
		log.debug("Size of dataset after VTL script: {}", vtlBindings.getDataset(matrixNameParam).getDataPoints().size());
		OutputFiles of = new OutputFiles(Path.of(matrixDirectory), vtlBindings, matrixNameParam);
		of.writeOutputCsvTables();
		return ResponseEntity.ok(matrixNameParam);


	}
	
	private List<String[]> getFlatCsv(Path filePath) throws IOException, CsvValidationException   {
	    List<String[]> list = new ArrayList<>();
	    Map<String[], String> map = new HashMap<>();
	    String[] variables = null;
	        try (CSVReader csvReader = CsvUtils.getReader(filePath)) {
	            String[] line;
	            while ((line = csvReader.readNext()) != null) {
	            	if (csvReader.getLinesRead() ==1) { //HEADER
	            		variables = Arrays.copyOfRange(line, 1, line.length);
	            		String[] newHeaders = {"libelle","statut","code"};
	            		list.add(newHeaders);
	            	}else if (variables != null) {
		            	for (int i=0;i<variables.length;i++) {
							String[] key = {line[0],variables[i]};
							map.putIfAbsent(key, line[i+1]);
						}
	            	}
	            }
	    }
	    map.forEach((k,v)-> {
	    	String[] element = {k[0],k[1],v};
	    	list.add(element);
	    });
	    return list;
	}

	private void writeAllLinesInCsv(List<String[]> lines, Path path) throws IOException {
	    try (CSVWriter writer = new CSVWriter(new FileWriter(path.toString()),';',ICSVWriter.NO_QUOTE_CHARACTER,
                ICSVWriter.DEFAULT_ESCAPE_CHARACTER,
                ICSVWriter.DEFAULT_LINE_END)) {
	        writer.writeAll(lines);
	    }
	}
	


}