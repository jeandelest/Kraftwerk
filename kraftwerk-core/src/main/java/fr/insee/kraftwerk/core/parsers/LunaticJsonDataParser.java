package fr.insee.kraftwerk.core.parsers;

import java.nio.file.Path;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import fr.insee.kraftwerk.core.Constants;
import fr.insee.kraftwerk.core.exceptions.NullException;
import fr.insee.kraftwerk.core.metadata.VariablesMap;
import fr.insee.kraftwerk.core.rawdata.GroupData;
import fr.insee.kraftwerk.core.rawdata.GroupInstance;
import fr.insee.kraftwerk.core.rawdata.QuestionnaireData;
import fr.insee.kraftwerk.core.rawdata.SurveyRawData;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class LunaticJsonDataParser extends DataParser {

	/**
	 * Parser constructor.
	 * 
	 * @param data The SurveyRawData to be filled by the parseSurveyData method. The
	 *             variables must have been previously set.
	 */
	public LunaticJsonDataParser(SurveyRawData data) {
		super(data);
	}

	@Override
	void parseDataFile(Path filePath) throws NullException {
		log.warn("Lunatic data parser being implemented!");

		// Read data in json file
		JSONObject jsonObject = null;
		try {
			jsonObject = (JSONObject) Constants.readJsonSimple(filePath);
		} catch (Exception e) {
			throw new NullException("Can't read JSON file - " + e.getClass() + " " + e.getMessage());
		}
		JSONObject jsonData = (JSONObject) jsonObject.get("data");

		// Init the questionnaire data object
		QuestionnaireData questionnaireData = new QuestionnaireData();

		// Root identifier
		String identifier = (String) jsonObject.get("id");
		questionnaireData.setIdentifier(identifier);
		data.getIdSurveyUnits().add(identifier);

		// Survey answers
		readCollected(jsonData, questionnaireData);
		readExternal(jsonData,  questionnaireData);
		//Calculated variables are not read, they are calculated throw the metadata description
		data.addQuestionnaire(questionnaireData);

		log.info("Successfully parsed Lunatic JSON answers file: {}", filePath);

	}


	private void readCollected(JSONObject jsonData, QuestionnaireData questionnaireData) {
		readVariables(Constants.COLLECTED,jsonData, questionnaireData, data.getVariablesMap());
	}
	

	private void readExternal(JSONObject jsonData, QuestionnaireData questionnaireData) {
		readVariables(Constants.EXTERNAL,jsonData, questionnaireData, data.getVariablesMap());
		
	}
	
	private void readVariables(String type, JSONObject jsonData, QuestionnaireData questionnaireData, VariablesMap variables) {
		JSONObject collectedVariables = (JSONObject) jsonData.get(type);

		// Data object
		GroupInstance answers = questionnaireData.getAnswers();

		for (Object variable : collectedVariables.keySet()) {
			String variableName = (String) variable;
			JSONObject variableData = (JSONObject) collectedVariables.get(variableName);
			if (variables.hasVariable(variableName)) {// Read only variables described in metadata
				if (variableData.get(type) != null) {
					Object value = variableData.get(type);
					if (value instanceof JSONArray valueArray) {// Group variables
						// Get the selected subgroup
						String groupName = variables.getVariable(variableName).getGroupName();
						GroupData groupData = answers.getSubGroup(groupName);
						// Add all children values to the variable in the subgroup
						addAllVariablesInGroup(variables, variableName, valueArray, groupData);
					}else { // Root																			// Variables
						setMaxLength(variables, variableName, value.toString());
						answers.putValue(variableName, value.toString());
					} // else JSONObject ?
				}
			} else {
				log.warn(String.format("WARNING: Variable %s not expected!", variableName));
			}

		}
	}

	private void addAllVariablesInGroup(VariablesMap variables, String variableName, JSONArray valueArray,
			GroupData groupData) {
		for (int j = 0; j < valueArray.size(); j++) {
			String currentVal = valueArray.get(j).toString();
			setMaxLength(variables, variableName, currentVal);
			groupData.putValue(currentVal, variableName, j);
		}
	}


}
