package nipel.FreqControl.Controller;

public abstract class InjectableController {
    protected MainController mainController;
    public void injectMainController(MainController mainController)
    {
        this.mainController = mainController;
    }
    abstract void update();
}
