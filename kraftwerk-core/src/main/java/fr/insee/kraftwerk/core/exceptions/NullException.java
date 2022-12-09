package fr.insee.kraftwerk.core.exceptions;

import java.io.File;
import java.nio.file.Path;

public class NullException extends KraftwerkException {

    private static final long serialVersionUID = 6677487610288558193L;

    
    public NullException() {
        this( "Denominator cannot be zero" );
    }

    public NullException( String message ) {
        super(500, message );
    }
	
}