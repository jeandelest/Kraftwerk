package fr.insee.kraftwerk.core.utils;

import fr.insee.kraftwerk.core.Constants;
import fr.insee.kraftwerk.core.exceptions.KraftwerkException;
import fr.insee.kraftwerk.core.inputs.UserInputs;
import fr.insee.kraftwerk.core.inputs.UserInputsFile;

import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public interface FileUtilsInterface {
    static Path transformToOut(Path inDirectory) {
        return transformToOther(inDirectory, "out");
    }

    static Path transformToOut(Path inDirectory, LocalDateTime localDateTime) {
        return transformToOther(inDirectory, "out").resolve(localDateTime.format(DateTimeFormatter.ofPattern(Constants.OUTPUT_FOLDER_DATETIME_PATTERN)));
    }

    /**
     * Change /some/path/in/campaign-name to /some/path/temp/campaign-name
     */
    static Path transformToTemp(Path inDirectory) {
        return transformToOther(inDirectory, "temp");
    }

    /**
     * Change /some/path/in/campaign-name to /some/path/__other__/campaign-name
     */
    private static Path transformToOther(Path inDirectory, String other) {
        return "in".equals(inDirectory.getFileName().toString()) ? inDirectory.getParent().resolve(other)
                : transformToOther(inDirectory.getParent(), other).resolve(inDirectory.getFileName());
    }

    void renameInputFile(Path inDirectory);

    void archiveInputFiles(UserInputsFile userInputsFile) throws KraftwerkException;

    void deleteDirectory(Path directoryPath) throws KraftwerkException;

    List<String> listFiles(String dir);

    Path getTempVtlFilePath(UserInputs userInputs, String step, String dataset);

    Path convertToPath(String userField, Path inputDirectory) throws KraftwerkException;

    URL convertToUrl(String userField, Path inputDirectory);
}
