package com.hospital.harmonia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alternative entry point, used only when the program is packaged as a "fat
 * jar" (via mvn package / maven-shade-plugin) and run with
 * "java -jar harmonia.jar". This works around a JavaFX limitation: when the
 * main class run by "java -jar" directly extends Application, the runtime
 * sometimes complains about missing modules. Having a separate class, with no
 * inheritance from Application, that just calls App.main(...), fixes this.
 *
 * During development in Eclipse, you can run App.java directly (Run As ->
 * Java Application) or use "mvn javafx:run".
 */
public class Launcher {

    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {
        log.debug("Launching Harmonia via the fat-jar entry point.");
        App.main(args);
    }
}
