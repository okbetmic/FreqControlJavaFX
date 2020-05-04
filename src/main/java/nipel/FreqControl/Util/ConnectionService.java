package nipel.FreqControl.Util;

import com.fazecast.jSerialComm.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static nipel.FreqControl.Util.Commands.controllerActions.*;
import static nipel.FreqControl.Util.Commands.deviceStates;
import static nipel.FreqControl.Util.Commands.deviceActions;
import static nipel.FreqControl.Util.Commands.log;

public class ConnectionService extends Service<String> {

    private String portDescriptor;
    private SerialPort serialPort;

    //  enough to describe all operating modes

    public Commands.controllerActions controllerAction;
    public Commands.deviceStates deviceState;
    private Commands.deviceSettings deviceSettings;

    private BooleanProperty sendDisableProperty;
    public StringProperty deviceStateProperty = new SimpleStringProperty();
    public StringProperty connectionStateProperty = new SimpleStringProperty();


    byte[] buffer;

    public ConnectionService() {
        Commands.setVariables(); // init states and commands
        buffer  = new byte[19]; // serial port read-write buffer
        portDescriptor = "";

        sendDisableProperty = new SimpleBooleanProperty(true);
        deviceState = deviceStates.NC;
        serviceEnd();
    }

    public BooleanProperty getSendDisableProperty() {
        return sendDisableProperty;
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

        buffer[2] = (byte) ((deviceSettings.freq >>> 0) & 0xFF);
        buffer[3] = (byte) ((deviceSettings.freq >>> 8) & 0xFF);
        buffer[4] = (byte) ((deviceSettings.freq >>> 16) & 0xFF);
        buffer[5] = (byte) ((deviceSettings.freq >>> 24) & 0xFF);

        buffer[6] = deviceActions.get("PL");

        serialPort.writeBytes(buffer, 7);
        serialPort.readBytes(buffer, 1);
        if (buffer[0] == deviceActions.get("READY_B")) {
            System.out.println("java");
            for (int i = 0; i < 4; i++) {
                System.out.print(String.format("%8s", Integer.toBinaryString(buffer[5 - i] & 0xFF)).replace(' ', '0')  + "\t");
            }
            Arrays.fill(buffer, (byte) 0);
            serialPort.readBytes(buffer, 4);
            System.out.println("\nardu received");
            for (int i = 0; i < 4; i++) {
                System.out.print(String.format("%8s", Integer.toBinaryString(buffer[3 - i] & 0xFF)).replace(' ', '0') + "\t");
            }
            System.out.println("\n");
            return true;
        }
        return false;
    }

    private void sendSweep() throws InterruptedException {
        buffer[0] = deviceActions.get("PF");
        buffer[1] = deviceActions.get("SW");

        buffer[2] = (byte) ((deviceSettings.minF >>> 0) & 0xFF);
        buffer[3] = (byte) ((deviceSettings.minF >>> 8) & 0xFF);
        buffer[4] = (byte) ((deviceSettings.minF >>> 16) & 0xFF);
        buffer[5] = (byte) ((deviceSettings.minF >>> 24) & 0xFF);

        buffer[6] = (byte) ((deviceSettings.maxF >>> 0) & 0xFF);
        buffer[7] = (byte) ((deviceSettings.maxF >>> 8) & 0xFF);
        buffer[8] = (byte) ((deviceSettings.maxF >>> 16) & 0xFF);
        buffer[9] = (byte) ((deviceSettings.maxF >>> 24) & 0xFF);

        buffer[10] = (byte) ((deviceSettings.timeStep >>> 0) & 0xFF);
        buffer[11] = (byte) ((deviceSettings.timeStep >>> 8) & 0xFF);
        buffer[12] = (byte) ((deviceSettings.timeStep >>> 16) & 0xFF);
        buffer[13] = (byte) ((deviceSettings.timeStep >>> 24) & 0xFF);

        buffer[14] = (byte) ((deviceSettings.freqStep >>> 0) & 0xFF);
        buffer[15] = (byte) ((deviceSettings.freqStep >>> 8) & 0xFF);
        buffer[16] = (byte) ((deviceSettings.freqStep >>> 16) & 0xFF);
        buffer[17] = (byte) ((deviceSettings.freqStep >>> 24) & 0xFF);

        buffer[18] = deviceActions.get("PL");

        serialPort.writeBytes(buffer, 19);
        serialPort.readBytes(buffer, 1);
        if (buffer[0] == deviceActions.get("READY_B")) {
            double totalTime = (deviceSettings.maxF - deviceSettings.minF) / deviceSettings.freqStep * deviceSettings.timeStep;
            log.info("Timeout " + String.valueOf(totalTime) + " ms");
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, (int) (totalTime * 1.2), 100);
            int i = (deviceSettings.maxF - deviceSettings.minF) / deviceSettings.freqStep;
            final long sweepStartTime = System.currentTimeMillis();
            serialPort.addDataListener(new SerialPortPacketListener() {
                @Override
                public int getPacketSize() {
                    return i + 1;
                }

                @Override
                public int getListeningEvents() {
                    return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
                }

                @Override
                public void serialEvent(SerialPortEvent serialPortEvent) {
                    byte[] newData = serialPortEvent.getReceivedData();
                    System.out.println("Received data of size: " + newData.length + "\n");
                    if (newData[newData.length - 1] == deviceActions.get("SWEEP_END_B")) {
                        System.out.println("sweep end " + (System.currentTimeMillis() - sweepStartTime));
                        controllerAction = PING;
                        serialPort.removeDataListener();
                    }
                }
            });
        }
    }

    @Override
    protected Task<String> createTask() {
        if (deviceState == deviceStates.NC && controllerAction == controllerAction.BEGIN_COMM) {// connect to arduino
            return new Task<String>() {
                @Override
                protected String call() {
                    log.info("created connection task (deviceState=" + deviceState + " and controllerAction=" + controllerAction + ")");
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
                            deviceState = deviceStates.SB;
                        }
                    } catch (TimeoutException e) {
                        log.warning(e.getLocalizedMessage());
                    } catch (Exception e) {
                        log.warning(e.getLocalizedMessage());
                    }

                    if (deviceState == deviceStates.SB) {
                        log.info("connection with " + portDescriptor + " successful");
                        return "ok";
                    }
                    else {
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
                    while (deviceState != deviceStates.NC) {
                        if (ping()) {
                            switch (controllerAction) {
                                case PING:
                                    break;
                                case SEND_SF:
                                    log.info("sending frequency (freq=" + deviceSettings.freq + ")");
                                    if (sendFrequency()) {
                                        log.info("data transfer successful");
                                        if (deviceSettings.freq <=0)
                                            deviceState = deviceState.SB;
                                        else
                                            deviceState = deviceState.SF;
                                        Platform.runLater(() -> serviceEnd());
                                    } else {
                                        log.warning("data send error");
                                    }
                                    controllerAction = controllerAction.PING;
                                    break;
                                case SEND_SW:
                                    log.info(String.format("sending sweep \n\tminF=%d Hz\n\tmaxF=%d Hz\n\ttimeStep=%d ms\n\tfreqStep=%dHz", deviceSettings.minF, deviceSettings.maxF, deviceSettings.timeStep, deviceSettings.freqStep));
                                    sendSweep();
                                    final long sweepStartTime = System.currentTimeMillis();
                                    double totalTime = (deviceSettings.maxF - deviceSettings.minF) / deviceSettings.freqStep * deviceSettings.timeStep;
                                    while ((controllerAction == SEND_SW) && ((System.currentTimeMillis() - sweepStartTime) <= totalTime * 2))
                                    {
                                        updateProgress((System.currentTimeMillis() - sweepStartTime), totalTime);
                                    }
                                    break;
                            }
                        } else {
                            log.warning("connection lost");
                            deviceState = deviceStates.NC; // exiting task
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
        if (deviceState == deviceStates.NC)
            connectionStateProperty.setValue("Connecting");
        super.start();
    }

    @Override
    public void succeeded() {
        log.info("task succeeded (deviceState=" + deviceState + " and controllerAction=" + controllerAction + ")");
        serviceEnd();
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
            deviceState = (deviceStates.NC);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void serviceEnd() {
        switch (deviceState)
        {
            case NC -> {
                connectionStateProperty.setValue(deviceState.toString());
                deviceStateProperty.setValue("");
                sendDisableProperty.setValue(true);
                break;
            }
            default -> {
                connectionStateProperty.setValue("Connected");
                deviceStateProperty.setValue(deviceState.toString());
                sendDisableProperty.setValue(false);
            }
        }
    }
}
