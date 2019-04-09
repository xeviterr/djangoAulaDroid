package cat.institutmontilivi.djau;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.LinearLayout;
import android.widget.Toast;

import cat.institutmontilivi.djau.R;

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
    //Date dataAVisualitzar = new GregorianCalendar(2018, Calendar.DECEMBER, 31).getTime();
    Date dataAVisualitzar = new GregorianCalendar().getTime();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_horari_dia);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        setTitle(sdf.format(dataAVisualitzar));
        doLogin();
    }

    protected void doLogin()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        pws = new PresenciaWebService(
                conn, prefs.getString("server_url", ""),prefs.getString("username", ""));

        pws.getAPILevel(this);
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
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
                pws.getImpartirPerData(this, sdf.format(dataAVisualitzar));
                return true;
            case R.id.seguentDia:
                Log.e("DEBUG", "click a dia següent");
                dataAVisualitzar = Utils.sumaORestaDiesAData(dataAVisualitzar, 1);
                pws.getImpartirPerData(this, sdf.format(dataAVisualitzar));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
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
                    Drawable drw = ResourcesCompat.getDrawable(getResources(), R.drawable.boto_vermell, null);
                    b.setBackgroundDrawable(drw);
                    b.setText("G:" + b.getText());
                } else {
                    Drawable drw = ResourcesCompat.getDrawable(getResources(), R.drawable.boto_verd, null);
                    b.setBackgroundDrawable(drw);
                }
            }
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
                if (callerID == PresenciaWebService.CALLER_doLogin)
                    msg = "No s'ha pogut fer login, canvia la configuració i intenta reconnectar";
                else
                    msg = errorMsg.toString();
                Toast toast = Toast.makeText(getApplicationContext(),
                        msg,
                        Toast.LENGTH_LONG);
                toast.show();
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
        pws.getImpartirPerData(this, new SimpleDateFormat("yyyy-MM-dd").format(dataAVisualitzar));
    }

}
