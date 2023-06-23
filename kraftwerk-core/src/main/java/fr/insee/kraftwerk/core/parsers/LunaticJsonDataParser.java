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
		if (jsonData == null)
			jsonData = jsonObject;

		// Init the questionnaire data object
		QuestionnaireData questionnaireData = new QuestionnaireData();

		// Root identifier
		String identifier = (String) jsonObject.get("id");
		questionnaireData.setIdentifier(identifier);
		data.getIdSurveyUnits().add(identifier);

		// Survey answers
		readCollected(jsonData, questionnaireData);
		readExternal(jsonData, questionnaireData);
		// Calculated variables are not read, they are calculated throw the metadata
		// description
		data.addQuestionnaire(questionnaireData);

		log.info("Successfully parsed Lunatic JSON answers file: {}", filePath);

	}

	private void readCollected(JSONObject jsonData, QuestionnaireData questionnaireData) {
		JSONObject collectedVariables = (JSONObject) jsonData.get(Constants.COLLECTED);

		// Data object
		GroupInstance answers = questionnaireData.getAnswers();

		for (Object variable : collectedVariables.keySet()) {
			String variableName = (String) variable;
			JSONObject variableData = (JSONObject) collectedVariables.get(variableName);
			if (data.getVariablesMap().hasVariable(variableName)) {// Read only variables described in metadata
				if (variableData.get(Constants.COLLECTED) != null) {
					Object value = variableData.get(Constants.COLLECTED);
					if (value instanceof JSONArray valueArray) {// Group variables
						// Get the selected subgroup
						String groupName = data.getVariablesMap().getVariable(variableName).getGroupName();
						GroupData groupData = answers.getSubGroup(groupName);
						// Add all children values to the variable in the subgroup
						addAllVariablesInGroup(data.getVariablesMap(), variableName, valueArray, groupData);
					} else { // Root // Variables
						setMaxLength(data.getVariablesMap(), variableName, value.toString());
						answers.putValue(variableName, value.toString());
					} // else JSONObject ?
				}
			} else {
				log.warn(String.format("WARNING: Variable %s not expected!", variableName));
			}

		}
	}

	private void readExternal(JSONObject jsonData, QuestionnaireData questionnaireData) {
		JSONObject externalVariables = (JSONObject) jsonData.get(Constants.EXTERNAL);

		// Data object
		GroupInstance answers = questionnaireData.getAnswers();

		for (Object variable : externalVariables.keySet()) {
			String variableName = (String) variable;
			if (data.getVariablesMap().hasVariable(variableName)) {// Read only variables described in metadata
				if (externalVariables.get(variableName) instanceof JSONObject externalObj) {
					setMaxLength(data.getVariablesMap(), variableName, externalObj.toString());
					answers.putValue(variableName, externalObj.toString());
				}
				if (externalVariables.get(variableName) instanceof String externalStr) {
					setMaxLength(data.getVariablesMap(), variableName, externalStr);
					answers.putValue(variableName, externalStr);
				}
				if (externalVariables.get(variableName) instanceof JSONArray externalArray) {
					// Get the selected subgroup
					String groupName = data.getVariablesMap().getVariable(variableName).getGroupName();
					GroupData groupData = answers.getSubGroup(groupName);
					// Add all children values to the variable in the subgroup
					addAllVariablesInGroup(data.getVariablesMap(), variableName, externalArray, groupData);
				}
			} else {
				log.warn(String.format("WARNING: Variable %s not expected!", variableName));
			}

		}
	}

	private void addAllVariablesInGroup(VariablesMap variables, String variableName, JSONArray valueArray,
			GroupData groupData) {
		for (int j = 0; j < valueArray.size(); j++) {
			String currentVal = "";
			if (valueArray.get(j) != null)
				currentVal = valueArray.get(j).toString();
			setMaxLength(variables, variableName, currentVal);
			groupData.putValue(currentVal, variableName, j);
		}
	}

}
