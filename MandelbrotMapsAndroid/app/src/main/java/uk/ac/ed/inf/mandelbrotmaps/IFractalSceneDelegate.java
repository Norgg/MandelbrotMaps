package uk.ac.ed.inf.mandelbrotmaps;

import uk.ac.ed.inf.mandelbrotmaps.colouring.EnumColourStrategy;
import uk.ac.ed.inf.mandelbrotmaps.overlay.pin.PinColour;
import uk.ac.ed.inf.mandelbrotmaps.presenter.IFractalPresenter;
import uk.ac.ed.inf.mandelbrotmaps.settings.SceneLayoutEnum;

public interface IFractalSceneDelegate {
    public void setRenderingStatus(IFractalPresenter presenter, boolean rendering);

    public void onFractalLongClick(IFractalPresenter presenter, float x, float y);

    public void onFractalRecomputeScheduled(IFractalPresenter presenter);

    public void onFractalRecomputed(IFractalPresenter presenter, double timeTakenInSeconds);

    public void scheduleRecomputeBasedOnPreferences(IFractalPresenter presenter, boolean fullRefresh);

    public void onPinColourChanged(PinColour colour);

    public void onMandelbrotColourSchemeChanged(EnumColourStrategy colourStrategy, boolean reRender);

    public void onJuliaColourSchemeChanged(EnumColourStrategy colourStrategy, boolean reRender);

    public void onFractalViewReady(IFractalPresenter presenter);

    public void onSceneLayoutChanged(SceneLayoutEnum layoutType);
}
