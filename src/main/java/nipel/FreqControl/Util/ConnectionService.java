package nipel.FreqControl.Util;

import com.fazecast.jSerialComm.SerialPort;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static nipel.FreqControl.Util.Commands.controllerActions.*;
import static nipel.FreqControl.Util.Commands.deviceStates.*;
import static nipel.FreqControl.Util.Commands.deviceActions;
import static nipel.FreqControl.Util.Commands.log;

public class ConnectionService extends Service<String> {

    private String portDescriptor;
    private SerialPort serialPort;

    //  enough to describe all operating modes

    public Commands.controllerActions controllerAction;
    public Commands.deviceStates deviceState;
    private Commands.deviceSettings deviceSettings;

    byte[] buffer;

    public ConnectionService() {
        Commands.setVariables(); // init states and commands
        buffer  = new byte[15]; // serial port read-write buffer
        deviceState = NC; // by default not connected
        portDescriptor = "";
    }

    public String[] getPorts() {
        return Stream.of(SerialPort.getCommPorts()).map(a -> a.getSystemPortName()).toArray(String[]::new);
    }

    // replace with set settings
    public void setPortDescriptor(String portDescriptor) {
        this.portDescriptor = portDescriptor;
    }

    public void setControllerAction(Commands.controllerActions controllerAction) {
        this.controllerAction = controllerAction;
    }

    public void setSettings(Commands.deviceSettings deviceSettings) {
        this.deviceSettings = deviceSettings;
    }

    private boolean ping() { // call with certainty that port is opened
        buffer[0] = deviceActions.get("PF");
        buffer[1] = deviceActions.get("HANDSHAKE_B");
        buffer[2] = deviceActions.get("PL");
        serialPort.writeBytes(buffer, 3);
        serialPort.readBytes(buffer, 1);
        return (buffer[0] == deviceActions.get("HANDSHAKE_B"));
    }

    private boolean sendFrequency() {
        buffer[0] = deviceActions.get("PF");
        buffer[1] = deviceActions.get("SF");

        buffer[2] = (byte) ((deviceSettings.freq) & 0xFF);
        buffer[3] = (byte) ((deviceSettings.freq >> 8) & 0xFF);
        buffer[4] = (byte) ((deviceSettings.freq >> 16) & 0xFF);
        buffer[5] = (byte) ((deviceSettings.freq >> 24) & 0xFF);

        buffer[6] = deviceActions.get("PL");

        serialPort.writeBytes(buffer, 7);
        serialPort.readBytes(buffer, 1);
        if (buffer[0] == deviceActions.get("READY_B"))
            return true;
        return false;
    }



    @Override
    protected Task<String> createTask() {
        if (deviceState == deviceState.NC && controllerAction == controllerAction.BEGIN_COMM) {// connect to arduino
            return new Task<String>() {
                @Override
                protected String call() {
                    log.info("created connection task (deviceState=" + deviceState + " and controllerAction=" + controllerAction + ")");
                    updateMessage("connecting");
                    try {
                        serialPort.closePort();
                    } catch (Exception ex) {
                    }
                    if (portDescriptor.length() < 4) {
                        log.warning("incorrect port descriptor. connection interrupted");
                        return null;
                    }
                    serialPort = SerialPort.getCommPort(portDescriptor);
                    if (!serialPort.openPort()) {
                        log.warning("port opening error");
                    }

                    serialPort.setBaudRate(9600);
                    serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 3000, 100);

                    try {
                        Arrays.fill(buffer, (byte) 0);

                        serialPort.readBytes(buffer, 1);
                        if (buffer[0] != deviceActions.get("HANDSHAKE_B"))
                            throw new Exception("bad response from arduino");

                        buffer[0] = deviceActions.get("HANDSHAKE_B");
                        serialPort.writeBytes(buffer, 1);

                        serialPort.readBytes(buffer, 1);
                        if (buffer[0] == deviceActions.get("HANDSHAKE_B")) {    // connection successful
                            log.info("connection with " + portDescriptor + " successful");
                            deviceState = deviceState.SB;
                        }
                    } catch (TimeoutException e) {
                        log.warning(e.getLocalizedMessage());
                    } catch (Exception e) {
                        log.warning(e.getLocalizedMessage());
                    }

                    if (deviceState == deviceState.SB) {
                        updateMessage("connected");
                        return "ok";
                    }
                    else {
                        updateMessage("not connected");
                        return null;
                    }
                }
            };
        }

        if (controllerAction == PING && deviceState == deviceState.SB) {
            return new Task() {
                @Override
                protected String call() throws Exception {
                    log.info("created ping task (deviceState=" + deviceState + " and controllerAction=" + controllerAction + ")");
                    serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 100, 100);
                    updateMessage("connected");
                    while (deviceState != NC) {
                        if (ping()) {
                            switch (controllerAction) {
                                case PING:
                                    System.out.println("ping");
                                    break;
                                case SEND_SF:
                                    log.info("sending frequency " + deviceSettings.freq);
                                    if (sendFrequency()) {
                                        log.info("data transfer successful");
                                        deviceState = deviceState.SB;
                                    } else {
                                        log.warning("data send error");
                                    }
                                    controllerAction = controllerAction.PING;
                                    break;
                                case SEND_SW:
                                    break;
                            }
                        } else {
                            log.warning("connection lost");
                            deviceState = NC;
                            updateMessage("not connected");
                        }
                        Thread.sleep(1000);
                    }
                    return "end";
                }
            };
        }


        return new Task<String>() {
            @Override
            protected String call() throws Exception {
                while (true) {
                    System.out.println("you shouldn't have gotten here");
                    Thread.sleep(100);
                }
            }
        };
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void succeeded() {
        log.info("task succeeded (deviceState=" + deviceState + " and controllerAction=" + controllerAction + ")");
        super.succeeded();
    }

    @Override
    public void restart() {
        super.restart();
    }

    @Override
    public void reset() {
        super.reset();
    }

    public void down() {
        try {
            if (serialPort != null) {
                serialPort.closePort();
            }
            deviceState = NC;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
