package fr.insee.kraftwerk.core.metadata;

import fr.insee.kraftwerk.core.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetadataUtilsGenesisTest {

	@Test
	void addLunaticVariableTest() {

		VariablesMap variables = new VariablesMap();
		variables.putGroup(new Group("LOOP1"));
		McqVariable mcqVar = new McqVariable("QUESTION_GRID_VAR_11",variables.getGroup("LOOP1"),"QUESTION_GRID1","Label 1 question grid");
		mcqVar.setInQuestionGrid(true);
		Variable varRoot = new Variable("TYPE_QUEST", variables.getRootGroup(), VariableType.STRING);
		variables.putVariable(varRoot);
		variables.putVariable(mcqVar);

		MetadataUtilsGenesis.addLunaticVariable(variables,"FILTER_RESULT_QUESTION_GRID1", Constants.FILTER_RESULT_PREFIX, VariableType.BOOLEAN);
		MetadataUtilsGenesis.addLunaticVariable(variables,"FILTER_RESULT_TYPE_QUEST", Constants.FILTER_RESULT_PREFIX, VariableType.BOOLEAN);
		MetadataUtilsGenesis.addLunaticVariable(variables,"QUESTION_GRID1_MISSING", Constants.MISSING_SUFFIX, VariableType.STRING);
		MetadataUtilsGenesis.addLunaticVariable(variables,"TYPE_QUEST_MISSING", Constants.MISSING_SUFFIX, VariableType.STRING);
		MetadataUtilsGenesis.addLunaticVariable(variables,"COMMENT_QE", Constants.MISSING_SUFFIX, VariableType.STRING);

		assertEquals("LOOP1",variables.getVariable("FILTER_RESULT_QUESTION_GRID1").getGroup().getName());
		assertEquals(Constants.ROOT_GROUP_NAME,variables.getVariable("FILTER_RESULT_TYPE_QUEST").getGroup().getName());

		assertEquals("LOOP1",variables.getVariable("QUESTION_GRID1_MISSING").getGroup().getName());
		assertEquals(Constants.ROOT_GROUP_NAME,variables.getVariable("TYPE_QUEST_MISSING").getGroup().getName());

		assertEquals(Constants.ROOT_GROUP_NAME,variables.getVariable("COMMENT_QE").getGroup().getName());
	}


}
