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
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
		questionnaireData.setIdentifier((String) jsonObject.get("id"));

		// Survey answers
		readCollected(jsonData, questionnaireData, data.getVariablesMap());
		readExternal(jsonData,  questionnaireData, data.getVariablesMap());
		readCalculated();

		data.addQuestionnaire(questionnaireData);

		log.info("Successfully parsed Lunatic JSON answers file: {}", filePath);

	}


	private void readCollected(JSONObject jsonData, QuestionnaireData questionnaireData, VariablesMap variables) {
		readVariables(Constants.COLLECTED,jsonData, questionnaireData, data.getVariablesMap());
	}
	

	private void readExternal(JSONObject jsonData, QuestionnaireData questionnaireData, VariablesMap variablesMap) {
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
					if (value instanceof String || value instanceof Number || value instanceof Boolean) { // Root
																											// Variables
						setMaxLength(variables, variableName, value.toString());
						answers.putValue(variableName, value.toString());
					} else if (value instanceof JSONArray valueArray) {// Group variables
						// Get the selected subgroup
						String groupName = variables.getVariable(variableName).getGroupName();
						GroupData groupData = answers.getSubGroup(groupName);
						// Add all children values to the variable in the subgroup
						for (int j = 0; j < valueArray.size(); j++) {
							String currentVal = valueArray.get(j).toString();
							setMaxLength(variables, variableName, currentVal);
							groupData.putValue(currentVal, variableName, j);
						}
					} // else JSONObject ?

				}
			} else {
				log.warn(String.format("WARNING: Variable %s not expected!", variableName));
			}

		}
	}


	private void readCalculated() {
		// TODO Auto-generated method stub

	}

}
