package fr.insee.kraftwerk.core.metadata;

import fr.insee.kraftwerk.core.Constants;
import fr.insee.kraftwerk.core.exceptions.KraftwerkException;
import fr.insee.kraftwerk.core.inputs.ModeInputs;
import lombok.extern.log4j.Log4j2;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class MetadataUtilsGenesis {

	private MetadataUtilsGenesis() {
		throw new IllegalStateException("Utility class");
	}

	public static Map<String, VariablesMap> getMetadata(Map<String, ModeInputs> modeInputsMap) throws KraftwerkException {
		Map<String, VariablesMap> metadataVariables = new LinkedHashMap<>();
		for (Map.Entry<String, ModeInputs> entry : modeInputsMap.entrySet()) {
			String k = entry.getKey();
			ModeInputs v = entry.getValue();
			putToMetadataVariable(k, v, metadataVariables);
		}
		return metadataVariables;
	}

	private static void putToMetadataVariable(String dataMode, ModeInputs modeInputsGenesis, Map<String, VariablesMap> metadataVariables ) throws KraftwerkException {
		// Step 1 : we add the variables read in the DDI
		VariablesMap variables= new VariablesMap();
		variables = DDIReader.getVariablesFromDDI(modeInputsGenesis.getDdiUrl());

		// Step 2 : we add the variables that are only present in the Lunatic file
		if (modeInputsGenesis.getLunaticFile() != null) {
			// First we add the collected _MISSING variables
			List<String> missingVars = LunaticReader.getMissingVariablesFromLunatic(modeInputsGenesis.getLunaticFile());
			for (String missingVar : missingVars) {
				addLunaticVariable(variables, missingVar, Constants.MISSING_SUFFIX, VariableType.STRING);
			}
			// Then we add calculated FILTER_RESULT_ variables
			List<String> filterResults = LunaticReader.getFilterResultFromLunatic(modeInputsGenesis.getLunaticFile());
			for (String filterResult : filterResults) {
				addLunaticVariable(variables, filterResult, Constants.FILTER_RESULT_PREFIX, VariableType.BOOLEAN);
			}
		}
		metadataVariables.put(dataMode, variables);
	}

	public static void addLunaticVariable(VariablesMap variables, String missingVar, String prefixOrSuffix, VariableType varType) {
		String correspondingVariableName = missingVar.replace(prefixOrSuffix, "");
		Group group;
		if (variables.hasVariable(correspondingVariableName)) { // the variable is directly found
			group = variables.getVariable(correspondingVariableName).getGroup();
		} else if (variables.isInQuestionGrid(correspondingVariableName)) { // otherwise, it should be from a question grid
			group = variables.getQuestionGridGroup(correspondingVariableName);
		} else {
			group = variables.getGroup(variables.getGroupNames().get(0));
			log.warn(String.format(
					"No information from the DDI about question named \"%s\".",
					correspondingVariableName));
			log.warn(String.format(
					"\"%s\" has been arbitrarily associated with group \"%s\".",
					missingVar, group.getName()));
		}
		variables.putVariable(new Variable(missingVar, group, varType));
	}

	public static Map<String, VariablesMap> getMetadataFromLunatic(Map<String, ModeInputs> modeInputsMap) {
		Map<String, VariablesMap> metadataVariables = new LinkedHashMap<>();
		modeInputsMap.forEach((k, v) -> putToMetadataVariableFromLunatic(k,v,metadataVariables));
		return metadataVariables;
	}

	private static void putToMetadataVariableFromLunatic(String dataMode, ModeInputs modeInputs, Map<String, VariablesMap> metadataVariables ) {
		VariablesMap variables = LunaticReader.getVariablesFromLunatic(modeInputs.getLunaticFile());
		metadataVariables.put(dataMode, variables);
	}
}
