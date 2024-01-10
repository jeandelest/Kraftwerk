package fr.insee.kraftwerk.core.sequence;

import fr.insee.kraftwerk.core.TestConstants;
import fr.insee.kraftwerk.core.data.model.Mode;
import fr.insee.kraftwerk.core.data.model.SurveyUnitUpdateLatest;
import fr.insee.kraftwerk.core.exceptions.KraftwerkException;
import fr.insee.kraftwerk.core.inputs.UserInputsFile;
import fr.insee.kraftwerk.core.inputs.UserInputsGenesis;
import fr.insee.kraftwerk.core.metadata.VariablesMap;
import fr.insee.kraftwerk.core.vtl.VtlBindings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertThrows;

class BuildBindingSequenceGenesisTest {

	private static final Path inputSamplesDirectory = Path.of(TestConstants.UNIT_TESTS_DIRECTORY, "user_inputs_genesis");

	@Test
	@DisplayName("Attempting building bindings without metadata should throw an exception")
	void buildVtlBindingsGenesis_errorWithoutMetadata() throws KraftwerkException, IOException {
		//GIVEN
		List<Mode> modes = new ArrayList<>();
		modes.add(Mode.WEB);
		VtlBindings vtlBindings = new VtlBindings();
		BuildBindingsSequenceGenesis bbs = new BuildBindingsSequenceGenesis();
		List<SurveyUnitUpdateLatest> surveyUnits = List.of(new SurveyUnitUpdateLatest());
		//WHEN
		Map<String, VariablesMap> metadataVariables = new HashMap<>();
		//THEN
		assertThrows(NullPointerException.class, () -> bbs.buildVtlBindings("WEB", vtlBindings, metadataVariables,surveyUnits, inputSamplesDirectory));
	}



}
