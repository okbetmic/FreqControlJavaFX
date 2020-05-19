package nipel.FreqControl.Util;

import com.fazecast.jSerialComm.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.Arrays;
import java.util.stream.Stream;

import static nipel.FreqControl.Util.Commands.deviceStates;
import static nipel.FreqControl.Util.Commands.controllerActions;
import static nipel.FreqControl.Util.Commands.log;

public class ConnectionService extends Service<String> {

    private String portDescriptor;
    private SerialPort serialPort;

    private ObjectProperty<deviceStates> deviceStateProperty;

    private Settings settings;

    byte[] buffer;

    public ConnectionService(Settings settings) {
        Commands.setVariables(); // init states and commands
        buffer  = new byte[19]; // serial port read-write buffer
        portDescriptor = "";
        this.settings = settings;
        deviceStateProperty =new SimpleObjectProperty<>(deviceStates.NotConnected);
    }

    public ObjectProperty getDeviceStateProperty() {return deviceStateProperty; }

    public String[] getPorts() { return Stream.of(SerialPort.getCommPorts()).map(a -> a.getSystemPortName()).toArray(String[]::new); }

    public void setPortDescriptor(String portDescriptor) {
        this.portDescriptor = portDescriptor;
    }

    private boolean heartbeat() { // call with certainty that port is opened
        buffer[0] = Commands.deviceActions.get("PF");
        buffer[1] = Commands.deviceActions.get("HANDSHAKE_B");
        buffer[2] = Commands.deviceActions.get("PL");
        serialPort.writeBytes(buffer, 3);
        serialPort.readBytes(buffer, 1);
        return (buffer[0] == Commands.deviceActions.get("HANDSHAKE_B"));
    }

    private boolean sendFrequency() {
        buffer[0] = Commands.deviceActions.get("PF");
        buffer[1] = Commands.deviceActions.get("SF");

        buffer[2] = (byte) (((int) settings.freq) & 0xFF);
        buffer[3] = (byte) (((int)settings.freq >>> 8) & 0xFF);
        buffer[4] = (byte) (((int)settings.freq >>> 16) & 0xFF);
        buffer[5] = (byte) (((int)settings.freq >>> 24) & 0xFF);

        buffer[6] = Commands.deviceActions.get("PL");

        serialPort.writeBytes(buffer, 7);
        serialPort.readBytes(buffer, 1);

        return buffer[0] == Commands.deviceActions.get("READY_B");
    }

    private void sendSweep() {
        buffer[0] = Commands.deviceActions.get("PF");
        buffer[1] = Commands.deviceActions.get("SW");

        buffer[2] = (byte) (((int) settings.minF) & 0xFF);
        buffer[3] = (byte) (((int)settings.minF >>> 8) & 0xFF);
        buffer[4] = (byte) (((int)settings.minF >>> 16) & 0xFF);
        buffer[5] = (byte) (((int)settings.minF >>> 24) & 0xFF);

        buffer[6] = (byte) (((int) settings.maxF) & 0xFF);
        buffer[7] = (byte) (((int)settings.maxF >>> 8) & 0xFF);
        buffer[8] = (byte) (((int)settings.maxF >>> 16) & 0xFF);
        buffer[9] = (byte) (((int)settings.maxF >>> 24) & 0xFF);

        double ts = settings.timeStep * 1000;
        buffer[10] = (byte) (((int)ts) & 0xFF);
        buffer[11] = (byte) (((int)ts >>> 8) & 0xFF);
        buffer[12] = (byte) (((int)ts >>> 16) & 0xFF);
        buffer[13] = (byte) (((int)ts >>> 24) & 0xFF);

        buffer[14] = (byte) (((int) settings.freqStep) & 0xFF);
        buffer[15] = (byte) (((int)settings.freqStep >>> 8) & 0xFF);
        buffer[16] = (byte) (((int)settings.freqStep >>> 16) & 0xFF);
        buffer[17] = (byte) (((int)settings.freqStep >>> 24) & 0xFF);

        buffer[18] = Commands.deviceActions.get("PL");

        serialPort.writeBytes(buffer, 19);
        serialPort.readBytes(buffer, 1);
        if (buffer[0] == Commands.deviceActions.get("READY_B")) {
            double totalTime = ((int)settings.maxF - (int)settings.minF) / (int)settings.freqStep * (int)settings.timeStep;
            log.info("Timeout " + totalTime + " ms");
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, (int) (totalTime * 1.2), 100);
            int i = (int) ((settings.maxF - settings.minF) / settings.freqStep);
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
                    if (newData[newData.length - 1] == Commands.deviceActions.get("SWEEP_END_B")) {
                        System.out.println("sweep end " + (System.currentTimeMillis() - sweepStartTime));
                        settings.activeAction = controllerActions.HEARTBEAT;
                        serialPort.removeDataListener();
                    }
                }
            });
        }
    }

    @Override
    protected Task<String> createTask() {
        switch (settings.activeAction) {
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
            if (buffer[0] != Commands.deviceActions.get("HANDSHAKE_B"))
                throw new Exception("bad response");
            buffer[0] = Commands.deviceActions.get("HANDSHAKE_B");
            serialPort.writeBytes(buffer, 1);
            serialPort.readBytes(buffer, 1);
            if (buffer[0] == Commands.deviceActions.get("HANDSHAKE_B")) {
                Platform.runLater(() -> deviceStateProperty.setValue(deviceStates.StandBy));
                settings.activeAction = controllerActions.HEARTBEAT;
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
            log.info("sending frequency (freq=" + (int)settings.freq + ")");
            if (sendFrequency()) {
                log.info("data transfer successful");
                if ((int)settings.freq <=0 )
                    Platform.runLater(() -> deviceStateProperty.setValue(deviceStates.StandBy));
                else
                    Platform.runLater(() -> deviceStateProperty.setValue(deviceStates.SingleFreq));
            } else {
                throw new Exception("sending error");
            }
            settings.activeAction = controllerActions.HEARTBEAT;
            return null;
        }
    }

    private class SendSweepTask extends Task<String> {
        @Override
        protected String call() throws Exception {
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 100, 100);
            Platform.runLater(() -> deviceStateProperty.setValue(deviceStates.Sweep));
            log.info(String.format("sending sweep \n\tminF=%d Hz\n\tmaxF=%d Hz\n\ttimeStep=%d ms\n\tfreqStep=%dHz", (int)settings.minF, (int)settings.maxF, (int)(settings.timeStep*1000), (int)settings.freqStep));
            sendSweep();
            final long sweepStartTime = System.currentTimeMillis();
            double totalTime = (settings.maxF - settings.minF) / settings.freqStep * settings.timeStep;
            while ((settings.activeAction == controllerActions.SEND_SW) && ((System.currentTimeMillis() - sweepStartTime) <= totalTime * 1000))
                updateProgress((System.currentTimeMillis() - sweepStartTime), totalTime * 1000);

            settings.activeAction = controllerActions.HEARTBEAT;
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