package nipel.FreqControl.Util;

import com.fazecast.jSerialComm.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.Arrays;
import java.util.stream.Stream;

import static nipel.FreqControl.Util.Commands.deviceStates;
import static nipel.FreqControl.Util.Commands.deviceActions;
import static nipel.FreqControl.Util.Commands.controllerActions;
import static nipel.FreqControl.Util.Commands.log;

public class ConnectionService extends Service<String> {

    private String portDescriptor;
    private SerialPort serialPort;

    public Commands.controllerActions controllerAction;
    private Commands.deviceSettings deviceSettings;

    private ObjectProperty<deviceStates> deviceStateProperty;


    byte[] buffer;



    public ConnectionService() {
        Commands.setVariables(); // init states and commands
        buffer  = new byte[19]; // serial port read-write buffer
        portDescriptor = "";

        deviceStateProperty =new SimpleObjectProperty<>(deviceStates.NotConnected);
    }

    public ObjectProperty getDeviceStateProperty() {return deviceStateProperty; }

    public String[] getPorts() { return Stream.of(SerialPort.getCommPorts()).map(a -> a.getSystemPortName()).toArray(String[]::new); }

    public void setPortDescriptor(String portDescriptor) {
        this.portDescriptor = portDescriptor;
    }

    public void setControllerAction(controllerActions controllerAction) {
        this.controllerAction = controllerAction;
    }

    public void setSettings(Commands.deviceSettings deviceSettings) {
        this.deviceSettings = deviceSettings;
    }

    private boolean heartbeat() { // call with certainty that port is opened
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

        return buffer[0] == deviceActions.get("READY_B");
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
            log.info("Timeout " + totalTime + " ms");
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
                        controllerAction = controllerActions.HEARTBEAT;
                        serialPort.removeDataListener();
                    }
                }
            });
        }
    }

    @Override
    protected Task<String> createTask() {
        switch (controllerAction) {
            case BEGIN_COMM -> { return new BeginCommTask(); }
            case HEARTBEAT -> { return new HeartbeatTask(); }
            case SEND_SF -> { return new SendFrequencyTask(); }
            case SEND_SW -> { return new SendSweepTask(); }
        }
        return null;
    }

    // task classes with device actions

    private class BeginCommTask extends Task<String> {

        @Override
        protected String call() throws Exception {
            log.info("created connection task");
            Platform.runLater(() -> deviceStateProperty.setValue(deviceStates.Connecting));
            if (portDescriptor.length() < 4) {
                throw new Exception("incorrect port descriptor");
            }
            serialPort = SerialPort.getCommPort(portDescriptor);
            if (!serialPort.openPort()) {
                throw new Exception(String.format("can't open port <%s>", portDescriptor));
            }
            serialPort.setBaudRate(9600);
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 3000, 100);
            Arrays.fill(buffer, (byte) 0);
            serialPort.readBytes(buffer, 1);
            if (buffer[0] != deviceActions.get("HANDSHAKE_B"))
                throw new Exception("bad response");
            buffer[0] = deviceActions.get("HANDSHAKE_B");
            serialPort.writeBytes(buffer, 1);
            serialPort.readBytes(buffer, 1);
            if (buffer[0] == deviceActions.get("HANDSHAKE_B")) {
                Platform.runLater(() -> deviceStateProperty.setValue(deviceStates.StandBy));
                controllerAction = controllerActions.HEARTBEAT;
                log.info("connection with " + portDescriptor + " successful");
                return "successful connection";
            }
            return "null";
        }
    }

    private class HeartbeatTask extends Task<String> {
        @Override
        protected String call() throws Exception {
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 100, 100);
            int heartbeatDelay = 1000;
            Thread.sleep(heartbeatDelay);
            if (heartbeat()) {
                return "heartbeat ok";
            }
            else
                throw new Exception("lost heartbeat");
        }
    }

    private class SendFrequencyTask extends Task<String> {
        @Override
        protected String call() throws Exception {
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 100, 100);
            log.info("sending frequency (freq=" + deviceSettings.freq + ")");
            if (sendFrequency()) {
                log.info("data transfer successful");
                if (deviceSettings.freq <=0 )
                    Platform.runLater(() -> deviceStateProperty.setValue(deviceStates.StandBy));
                else
                    Platform.runLater(() -> deviceStateProperty.setValue(deviceStates.SingleFreq));
            } else {
                throw new Exception("sending error");
            }
            controllerAction = controllerActions.HEARTBEAT;
            return null;
        }
    }

    private class SendSweepTask extends Task<String> {
        @Override
        protected String call() throws Exception {
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 100, 100);
            Platform.runLater(() -> deviceStateProperty.setValue(deviceStates.Sweep));
            log.info(String.format("sending sweep \n\tminF=%d Hz\n\tmaxF=%d Hz\n\ttimeStep=%d ms\n\tfreqStep=%dHz", deviceSettings.minF, deviceSettings.maxF, deviceSettings.timeStep, deviceSettings.freqStep));
            sendSweep();
            final long sweepStartTime = System.currentTimeMillis();
            double totalTime = 1.0 * (deviceSettings.maxF - deviceSettings.minF) / deviceSettings.freqStep * deviceSettings.timeStep;
            while ((controllerAction == controllerActions.SEND_SW) && ((System.currentTimeMillis() - sweepStartTime) <= totalTime))
                updateProgress((System.currentTimeMillis() - sweepStartTime), totalTime);
            Platform.runLater(() -> deviceStateProperty.setValue(deviceStates.StandBy));
            return null;
        }
    }

    private void closeConnection() {
        deviceStateProperty.setValue(deviceStates.NotConnected);
        if (serialPort != null)
            serialPort.closePort();
    }
    @Override
    public boolean cancel() {
        closeConnection();
        return super.cancel();
    }
    @Override
    public void start() {
        super.start();
    }
    @Override
    protected void failed() {
        closeConnection();
        super.failed();
    }


}