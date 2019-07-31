package cat.institutmontilivi.djau;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import cat.institutmontilivi.djau.Exceptions.UserNotFoundException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

public class GuardiaActivity extends Activity implements PresenciaWebService.ICallBackaActivityGetString {

    PresenciaWebService pws = null;
    Professor profeSeleccionat = null;
    Franja franjaSeleccionada = null;

    class Franja
    {
        public String id;
        public String nom;

        @Override
        public String toString() {
            return nom;
        }
    }

    class Professor
    {
        public String username;
        public String nom;

        @Override
        public String toString() {
            return nom;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guardia);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        HttpConnection conn = (HttpConnection) getIntent().getSerializableExtra("CONN");
        final Date dataGuardia = (Date)getIntent().getSerializableExtra("DATA_A_VISUALITZAR");

        pws = new PresenciaWebService(conn, prefs.getString("server_url", ""),prefs.getString("username", ""));

        Button enviar = (Button) findViewById(R.id.button);
        final AutoCompleteTextView autoCompleteFranja = (AutoCompleteTextView) findViewById(R.id.AutocompleteFranja);
        enviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Inicio la petició de crear una nova hora de guàrida.
                ArrayAdapter<Franja> adaptador = (ArrayAdapter<Franja>)autoCompleteFranja.getAdapter();
                Log.e("DEBUG", "CREANT HORA GUARDIA" + franjaSeleccionada);
                Log.e("DEBUG", "CREANT HORA GUARDIA" + profeSeleccionat);
                String username = prefs.getString("username", "");
                //putGuardia(final ICallBackaActivityGetString activitatQueCrida, final String idUsuariASubstituir, final String idUsuari, final String idFranja, final Date diaAImpartir)
                try {
                    pws.putGuardia((PresenciaWebService.ICallBackaActivityGetString)v.getContext(),
                            profeSeleccionat.username,
                            prefs.getString("username", ""),
                            franjaSeleccionada.id,
                            dataGuardia);
                } catch (UserNotFoundException e) {
                    e.printStackTrace();

                }

            }
        });

        try {
            pws.getProfes(this);
            pws.getFrangesHoraries(this);
        } catch (UserNotFoundException e) {
            e.printStackTrace();
            Utils.mostraMissatgeToast(getApplicationContext(), e.getMessage());
        }

    }

    @Override
    public void returnData(String callerID, String data, boolean error, HttpError errorData) {
        try
        {
            if (callerID==PresenciaWebService.CALLER_getFrangesHoraries)
            {
                JSONArray franges = new JSONArray(data);
                ArrayList<Franja> listdata = new ArrayList<Franja>();
                for (int i=0;i<franges.length();i++){
                    JSONObject json = franges.getJSONObject(i);
                    Franja f = new Franja();
                    f.id = json.getString("id");
                    f.nom = json.getString("hora_inici") + " - " + json.getString("hora_fi");
                    listdata.add(f);
                    Log.e("DEBUG", listdata.toString());
                }
                ArrayAdapter<Franja> adapter = new ArrayAdapter<Franja>(this,
                        android.R.layout.simple_dropdown_item_1line, listdata);
                AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.AutocompleteFranja);
                textView.setAdapter(adapter);
                textView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        franjaSeleccionada = (Franja) parent.getItemAtPosition(position);
                    }
                });
            }
            else if(callerID==PresenciaWebService.CALLER_getProfes)
            {
                JSONArray profes = new JSONArray(data);
                ArrayList<Professor> listdata = new ArrayList<Professor>();
                for (int i=0;i<profes.length();i++){
                    JSONObject json = profes.getJSONObject(i);
                    Professor p = new Professor();
                    p.username = json.getString("username");
                    p.nom = json.getString("username");
                    listdata.add(p);
                    Log.e("DEBUG", listdata.toString());
                }
                ArrayAdapter<Professor> adapter = new ArrayAdapter<Professor>(this,
                        android.R.layout.simple_dropdown_item_1line, listdata);

                AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.AutocompleteProfe);
                textView.setAdapter(adapter);
                textView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        profeSeleccionat = (Professor) parent.getItemAtPosition(position);
                    }
                });
            }
            else if (callerID == PresenciaWebService.CALLER_putControlAssistencia)
            {
                if (error) {
                    Toast toast = Toast.makeText(getApplicationContext(), errorData.getMsg(), Toast.LENGTH_SHORT);
                    toast.show();
                }
                else {
                    this.finish();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
