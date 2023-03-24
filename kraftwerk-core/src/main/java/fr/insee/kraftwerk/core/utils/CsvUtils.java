package fr.insee.kraftwerk.core.utils;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import com.opencsv.exceptions.CsvException;

import fr.insee.kraftwerk.core.Constants;
import fr.insee.vtl.model.Dataset;
import fr.insee.vtl.model.InMemoryDataset;
import fr.insee.vtl.model.Structured.Component;
import fr.insee.vtl.model.Structured.DataPoint;
import fr.insee.vtl.model.Structured.DataStructure;

/** Encapsulate org.opencsv features that we use in Kraftwerk. */
public class CsvUtils {
	
	private CsvUtils() {
		//Utility class
	}

    public static CSVReader getReader(Path filePath) throws IOException {
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

    public static CSVReader getReaderWithSeparator(String filePath, char separator) throws IOException {
        CSVParser csvParser= new CSVParserBuilder()
                .withSeparator(separator)
                .build();
        return new CSVReaderBuilder(new FileReader(filePath, StandardCharsets.UTF_8))
                .withCSVParser(csvParser)
                .build();
    }

    public static CSVWriter getWriter(String filePath) throws IOException {
        return new CSVWriter(new FileWriter(filePath, StandardCharsets.UTF_8),
                Constants.CSV_OUTPUTS_SEPARATOR,
                Constants.getCsvOutputQuoteChar(),
                ICSVWriter.DEFAULT_ESCAPE_CHARACTER,
                ICSVWriter.DEFAULT_LINE_END);
    }
    
	public static Dataset getDatasetFromCsv(Path path, int nbIdentifier) throws IOException, CsvException {
		//Read CSV
		List<String[]> lines; 
		try (CSVReader csvReader = CsvUtils.getReader(path)) {
			 lines =  csvReader.readAll();
		 }
		 
		 //Components from headers
		 String[] headers = lines.get(0);
		 List<Component> components = new ArrayList<>();
		 for (int nbCol=0;nbCol<nbIdentifier;nbCol++) {
			 Component component = new Component(headers[nbCol],String.class,Dataset.Role.IDENTIFIER);
			 components.add(component);
		 }
		 for (int nbCol=nbIdentifier;nbCol<headers.length;nbCol++) {
			 Component component = new Component(headers[nbCol],String.class,Dataset.Role.ATTRIBUTE);
			 components.add(component);
		 }

		 //DataStructure
		 DataStructure ds = new DataStructure(components);
		 
		 //DataPoints from other lines
		 List<DataPoint> dataPoints = new ArrayList<>();
		 
		 for (int nbLine=1;nbLine<lines.size();nbLine++) {
			 String[] currentLine = lines.get(nbLine);
			 Map<String, Object> values = new HashMap<>();

			 for (int i=0;i<currentLine.length;i++) {
				values.putIfAbsent(headers[i], currentLine[i]);
			 }
			 DataPoint dp = new DataPoint(ds, values);
			 dataPoints.add(dp);
		 }
		 
		 //InMemoryDataset
		return new InMemoryDataset(dataPoints, ds);
		
	}
}
