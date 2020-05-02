package nipel.FreqControl.Controller;

import nipel.FreqControl.Util.Commands;

public class InjectableController {

    protected MainController mainController;
    protected Commands.deviceSettings deviceSettings;

    public InjectableController()
    {
        deviceSettings = new Commands.deviceSettings();
    }
    public void injectMainController(MainController mainController)
    {
        this.mainController = mainController;
    }

    public Commands.deviceSettings getSettings()
    {
        return deviceSettings;
    }
}
