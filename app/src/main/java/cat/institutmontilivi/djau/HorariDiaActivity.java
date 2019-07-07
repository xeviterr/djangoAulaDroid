package cat.institutmontilivi.djau;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;

import cat.institutmontilivi.djau.Exceptions.UserNotFoundException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class HorariDiaActivity extends Activity implements View.OnClickListener, PresenciaWebService.ICallBackaActivityGetString {
    private static final int CODI_ACTIVITAT_GUARDIA = 1;
    private static final int CODI_ACTIVITAT_PASSAR_LLISTA = 2;

    HttpPersistentConnection conn = new HttpPersistentConnection();
    PresenciaWebService pws = null;
    //Date dataAVisualitzar = new GregorianCalendar(2019, Calendar.MAY, 27).getTime();
    Date dataAVisualitzar =  new GregorianCalendar().getTime();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_horari_dia);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        setTitle(sdf.format(dataAVisualitzar));
    }

    @Override
    protected void onStart() {
        super.onStart();
        doLogin();
    }

    protected void doLogin()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        pws = new PresenciaWebService(
                conn, prefs.getString("server_url", ""), prefs.getString("username", ""));

        try {
            pws.getAPILevel(this);
        } catch (UserNotFoundException e) {
            e.printStackTrace();
            Utils.mostraMissatgeToast(getApplicationContext(),
                    "Error, no hi ha usuari configurat, accedeix a configuració i indica el teu usuari." + e.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.horari_dia_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        switch (item.getItemId()) {
            case R.id.configuracio:
                Log.e("DEBUG", "click a configuració");
                startActivity(new Intent(this, SettingsActivity.class ));
                return true;
            case R.id.reconnectar:
                Log.e("DEBUG", "click a reconnectar");
                doLogin();
                return true;
            case R.id.anteriorDia:
                Log.e("DEBUG", "click a dia anterior");
                dataAVisualitzar = Utils.sumaORestaDiesAData(dataAVisualitzar, -1);
                setTitle(sdf.format(dataAVisualitzar));
                try {
                    pws.getImpartirPerData(this, sdf.format(dataAVisualitzar));
                } catch (Exception e) {
                    e.printStackTrace();
                    Utils.mostraMissatgeToast(getApplicationContext(), e.getMessage());
                }
                return true;
            case R.id.seguentDia:
                Log.e("DEBUG", "click a dia següent");
                dataAVisualitzar = Utils.sumaORestaDiesAData(dataAVisualitzar, 1);
                setTitle(sdf.format(dataAVisualitzar));
                try {
                    pws.getImpartirPerData(this, sdf.format(dataAVisualitzar));
                } catch (UserNotFoundException e) {
                    e.printStackTrace();
                    Utils.mostraMissatgeToast(getApplicationContext(), e.getMessage());
                }
                return true;
            case R.id.saltarADia:
                Log.e("DEBUG", "click a saltarADia");
                Calendar cal = Calendar.getInstance();
                cal.setTime(dataAVisualitzar);

                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        this, new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                Calendar calr = Calendar.getInstance();
                                calr.set(year, monthOfYear, dayOfMonth);
                                Log.e("DEBUG" ," data seleccionada:" + calr.getTime());
                                dataAVisualitzar = calr.getTime();
                                String dAImprimir = sdf.format(dataAVisualitzar);
                                setTitle(dAImprimir);
                                try {
                                    pws.getImpartirPerData(HorariDiaActivity.this, dAImprimir);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Utils.mostraMissatgeToast(getApplicationContext(), e.getMessage());
                                }
                            }
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

                datePickerDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                    }
                });
                //datePickerDialog.updateDate(dataAVisualitzar.getYear(), dataAVisualitzar.getMonth(), dataAVisualitzar.getDay());
                datePickerDialog.show();
                return true;
             default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadControls(JSONArray dades) throws JSONException {
        //Consultar l'horari d'avui i recuperar totes les classes a impartir.
        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.mainLayout);
        mainLayout.removeAllViews();

        for (int i = 0; i < dades.length(); i++) {
            JSONObject impartir = dades.getJSONObject(i);
            Button b = new Button(this);
            b.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            b.setOnClickListener(this);
            b.setTag(impartir.getJSONObject("impartir").getString("pk"));
            //Treure els dos últims zeros dels segons..
            String horaInici = impartir.getJSONObject("horari").getJSONObject("fields").getString("hora_inici");
            if (horaInici.length() > 5)
                horaInici = horaInici.substring(0, 5);

            b.setText(impartir.getString("assignatura") + "\n" +
                    horaInici);
            JSONObject campsImpartir = impartir.getJSONObject("impartir").getJSONObject("fields");
            if (campsImpartir.getString("dia_passa_llista") == "null") {
                Drawable drw = ResourcesCompat.getDrawable(getResources(), R.drawable.boto_gris, null);
                b.setBackgroundDrawable(drw);
            }
            else {
                if (campsImpartir.getString("professor_guardia") != "null") {
                    Drawable drw = ResourcesCompat.getDrawable(getResources(), R.drawable.boto_blau, null);
                    b.setBackgroundDrawable(drw);
                    b.setText("G:" + b.getText());
                } else {
                    Drawable drw = ResourcesCompat.getDrawable(getResources(), R.drawable.boto_verd, null);
                    b.setBackgroundDrawable(drw);
                }
            }
            b.setTextColor(Color.BLACK);
            mainLayout.addView(b);
        }

        //Afegir botó de guàrdia.
        Button b = new Button(this);
        b.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        b.setTag("novaGuardia");
        b.setOnClickListener(this);
        b.setText("Passar guàrdia");
        mainLayout.addView(b);
    }

    @Override
    public void onClick(View v) {
        //Han fet click sobre un botó
        Button boto = (Button)v;
        if (v.getTag()=="novaGuardia")
        {
            //Botó nova guàrdia. Seleccionarem usuari i una hora.
            Intent intent = new Intent(this, GuardiaActivity.class);
            intent.putExtra("CONN", this.conn);
            intent.putExtra("DATA_A_VISUALITZAR", this.dataAVisualitzar);
            startActivityForResult(intent, CODI_ACTIVITAT_GUARDIA);
        }
        else
        {
            //Botó convencional click sobre una classe a impartir.
            Log.e("ERROR", "Log error." + boto.getTag());

            //Connexió
            Intent intent = new Intent(this, PassarLlistaActivity.class);
            intent.putExtra("CONN", this.conn);
            intent.putExtra("PKIMPARTIR", (String) boto.getTag());
            startActivityForResult(intent, CODI_ACTIVITAT_PASSAR_LLISTA);
        }
    }

    @Override
    public void returnData(final String callerID, final String data, boolean error, HttpError errorMsg) {
        //Retorna les dades del thread.
        try {
            if (error)
            {
                String msg = "";
                if (callerID == PresenciaWebService.CALLER_getAPILevel)
                    msg = "No s'ha pogut accedir al servei Web, comprova la configuració.";
                if (callerID == PresenciaWebService.CALLER_doLogin)
                    msg = "No s'ha pogut fer login, canvia el nom d'usuari i el password.";
                else
                    msg = errorMsg.toString();
                Utils.mostraMissatgeToast(getApplicationContext(), msg);
            }
            else {
                if (callerID == PresenciaWebService.CALLER_getAPILevel) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    if (!data.equals(Configuration.getInstance().APILevel))
                        throw new Exception("La versió de la API no coincideix, actualitza la teva aplicació Android a la última versió.");

                    pws.doLogin(this, prefs.getString("password", ""));
                }
                if (callerID == PresenciaWebService.CALLER_doLogin) {

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    //pws.getImpartirPerData(this, sdf.format(dataAVisualitzar));
                    pws.getImpartirPerData(this, sdf.format(dataAVisualitzar));
                }
                if (callerID == PresenciaWebService.CALLER_getImpartirPerData) {
                    Log.e("OBTINGUT EL PK", new JSONArray(data).getJSONObject(0)
                            .getJSONObject("impartir").getString("pk"));

                    loadControls(new JSONArray(data));
                }
            }
            Log.e("RETURN DATA", data);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ERR", data);
            try {
                loadControls(new JSONArray());
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        //Recarrega les dades.
        try {
            pws.getImpartirPerData(this, new SimpleDateFormat("yyyy-MM-dd").format(dataAVisualitzar));
        } catch (UserNotFoundException e) {
            e.printStackTrace();
            Utils.mostraMissatgeToast(getApplicationContext(), e.getMessage());
        }
    }
}
