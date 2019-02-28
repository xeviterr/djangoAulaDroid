package cat.institutmontilivi.djau;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.Map;

/**
 * Created by administrador on 18/07/16.
 * Per crear un control ho he fet a partir d'aquest codi. (sample descarregable)
 * https://developer.android.com/training/custom-views/create-view.html
 */
public class AssistenciaView extends LinearLayout implements View.OnClickListener {

    //Intefície que indica quin mètode han d'implementar els observers
    //que escoltin al control d'assistència per saber quan ha canviat d'estat.

    //Map entre el codi alfanumèric d'estat i el seu codi numèric. P-->0, F-->1
    public Map<String, Integer> estatsDisponibles;

    //Soc molt vagu.... set i get...
    private OnStateChangeListener stateChangeListener = null;
    private int estatActual = -1;
    private int estatAnterior = -1;

    private Button bPresent = null;
    private Button bFalta = null;
    private Button bRetard = null;
    private Button bJustificada = null;

    public AssistenciaView(Context context, OnStateChangeListener scl, Map<String, Integer> estats) {
        super(context);

        this.estatsDisponibles = estats;

        this.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);

        lp.weight = 1;
        Button bPresent = new Button(context);
        bPresent.setText("P");
        bPresent.setTag(estatsDisponibles.get("P").toString());
        bPresent.setOnClickListener(this);
        this.addView(bPresent,lp);
        this.bPresent = bPresent;

        Button bFalta = new Button(context);
        bFalta.setText("F");
        bFalta.setTag(estatsDisponibles.get("F").toString());
        bFalta.setOnClickListener(this);
        this.addView(bFalta,lp);
        this.bFalta = bFalta;

        Button bRetard = new Button(context);
        bRetard.setText("R");
        bRetard.setTag(estatsDisponibles.get("R").toString());
        bRetard.setOnClickListener(this);
        this.addView(bRetard, lp);
        this.bRetard = bRetard;

        Button bJustificada = new Button(context);
        bJustificada.setText("J");
        bJustificada.setTag(estatsDisponibles.get("J").toString());
        bJustificada.setOnClickListener(this);
        this.addView(bJustificada, lp);
        this.bJustificada = bJustificada;

        //Assignem la classe que escoltarà en cas de canvi d'estat del botó.
        this.setStateChangeListener(scl);
    }

    @Override
    public void onClick(View v) {
        Log.e("DEBUG", "Click sobre la view" + v.toString());

        Button b = (Button) v;
        int estatBotoOnHemFetClick = Integer.parseInt((String) b.getTag());
        setEstatActual(estatBotoOnHemFetClick);
        if (this.getStateChangeListener() != null)
            //Informa als observadors que han fet click sobre el botó i han canviat d'estat.
            this.getStateChangeListener().OnStateChange(this);

        Log.e("DEBUG", "Click sobre canvi d'estat");
    }

    /**
     * Canvia el color segons l'estat del botó.
     */
    private void canviarColorBoto()
    {
        //Deixar tots els botons sense color.
        this.bPresent.setBackgroundColor(Color.TRANSPARENT);
        this.bFalta.setBackgroundColor(Color.TRANSPARENT);
        this.bRetard.setBackgroundColor(Color.TRANSPARENT);
        this.bJustificada.setBackgroundColor(Color.TRANSPARENT);

        if (this.estatActual == estatsDisponibles.get("P")) bPresent.setBackgroundColor(Color.GREEN);
        else if (this.estatActual == estatsDisponibles.get("F")) bFalta.setBackgroundColor(Color.RED);
        else if (this.estatActual == estatsDisponibles.get("J")) bJustificada.setBackgroundColor(Color.BLUE);
        else if (this.estatActual == estatsDisponibles.get("R")) bRetard.setBackgroundColor(Color.YELLOW);
    }

    public OnStateChangeListener getStateChangeListener() {
        return stateChangeListener;
    }

    public void setStateChangeListener(OnStateChangeListener stateChangeListener) {
        this.stateChangeListener = stateChangeListener;
    }

    public int getEstatActual() {
        return estatActual;
    }

    public void setEstatActual(int estatActual) {
        this.estatActual = estatActual;
        canviarColorBoto();
    }

    public void setEstatHoraAnterior(int estatAnterior) {
        this.estatAnterior= estatAnterior;
    }

    public int getEstatAnterior() { return estatAnterior; }

    public interface OnStateChangeListener
    {
        void OnStateChange(AssistenciaView source);
    }
}