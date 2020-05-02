package nipel.FreqControl.Util;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.*;

public class Commands {

    public static HashMap<String, Byte> deviceActions;
    public enum deviceStates {NC, SB, SF, SW};
    public enum controllerActions {BEGIN_COMM, PING, SB, SEND_SF, SEND_SW};


    public static Logger log = Logger.getLogger(Commands.class.getName());

    public static class deviceSettings {
        // single frequency
        public int freq;
        // sweep
        public int beginFreq, endFreq;
        public double step;
    }

    public static void setVariables()
    {
        log.setUseParentHandlers(false);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        FileHandler fileHandler = null;
        try {
            fileHandler = new FileHandler("log.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        SimpleFormatter simpleFormatter = new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT] [%2$-1s] %3$s %n";
            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(format,
                        new Date(lr.getMillis()),
                        lr.getLevel().getLocalizedName(),
                        lr.getMessage()
                );
            }
        };

        consoleHandler.setFormatter(simpleFormatter);
        fileHandler.setFormatter(simpleFormatter);

        log.addHandler(consoleHandler);
        log.addHandler(fileHandler);
        log.setLevel(Level.INFO);

        log.info("logger started");

        deviceActions = new HashMap<>();
        deviceActions.put("READY_B",        (byte) 002);    //
        deviceActions.put("HANDSHAKE_B",    (byte) 012);    //
        deviceActions.put("ERROR_B",        (byte) 022);    //

        deviceActions.put("PF",             (byte) 033);
        deviceActions.put("PL",             (byte) 066);

        deviceActions.put("SF",             (byte) 001);
        deviceActions.put("SW",             (byte) 002);
        deviceActions.put("SB",             (byte) 003);
    }
}
