package entity.Ue.register;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;

public class RegistrationManager implements RegistrationListener{
    private final String logFile;

    public RegistrationManager(String logFile) {
        this.logFile = logFile;
    }
    @Override
    public void onRegistrationResult(String supi, boolean success, int cause) {
        String line = String.format("%s,%b,%d,%s\n",supi,success,cause, Instant.now().toString());
        try(FileWriter writer = new FileWriter(logFile,true)) {
            writer.write(line);
            System.out.println("RegistrationManager: record ->" + line.trim());
        }catch (IOException e){
            System.out.println("RegistrationManager: Failed to write to log:  " + e.getMessage());
        }
    }
}
