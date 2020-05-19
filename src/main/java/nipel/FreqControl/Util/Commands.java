package nipel.FreqControl.Util;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.*;

public class Commands {

    public static HashMap<String, Byte> deviceActions;
    public enum deviceStates {
        NotConnected("Not connected"),
        Connecting("Connecting"),
        StandBy("Stand by"),
        SingleFreq("Single Frequency"),
        Sweep("Sweep");
        private final String displayName;
        deviceStates(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    };
    public enum controllerActions {BEGIN_COMM, HEARTBEAT, SB, SEND_SF, SEND_SW};

    public static Logger log = Logger.getLogger(Commands.class.getName());

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
            private static final String format = "[%1$tF %1$tT][%2$-1s][%3$s] %4$s %n";
            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(format,
                        new Date(lr.getMillis()),
                        lr.getLevel().getLocalizedName(),
                        lr.getSourceClassName(),
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

        deviceActions.put("SWEEP_LOOP_B",   (byte) 055);
        deviceActions.put("SWEEP_END_B",    (byte) 065);

        deviceActions.put("PF",             (byte) 033);
        deviceActions.put("PL",             (byte) 066);

        deviceActions.put("SF",             (byte) 001);
        deviceActions.put("SW",             (byte) 002);
        deviceActions.put("SB",             (byte) 003);
    }
}
