package nipel.FreqControl.Util;

import java.io.Serializable;

public class Settings implements Serializable {

    // maybe it isn't the best choice..
    public Commands.controllerActions userAction;
    public Commands.controllerActions activeAction;

    // single freq
    public double freq;
    // sweep
    public double minF, maxF; // Hz
    public double totalTime;
    public double timeStep; // MILLIS
    public double freqStep; // Hz
}
