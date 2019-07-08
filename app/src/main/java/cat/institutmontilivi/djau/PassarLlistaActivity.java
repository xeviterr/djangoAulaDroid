package cat.institutmontilivi.djau;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PassarLlistaActivity extends Activity implements PresenciaWebService.ICallBackaActivityGetString, AssistenciaView.OnStateChangeListener, View.OnClickListener
{
    String pkImpartir = "";
    ArrayList<AssistenciaView> vistesAssistencia = new ArrayList<AssistenciaView>();
    Map<String, Integer> estatsControlAssistencia = new HashMap<String, Integer>();

    PresenciaWebService pws = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_passar_llista);
            HttpPersistentConnection conn = (HttpPersistentConnection) getIntent().getSerializableExtra("CONN");
            this.pkImpartir = getIntent().getStringExtra("PKIMPARTIR");
            if (conn == null)
                throw new Exception("Error no han passat la connexió, és necessària. (CONN)");
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            pws = new PresenciaWebService(conn, prefs.getString("server_url", ""),prefs.getString("username", ""));
            pws.getEstatControlAssistencia(this);
            pws.getControlAssistencia(this, this.pkImpartir);

            Button button = (Button) findViewById(R.id.btnEnviar);
            button.setOnClickListener(this);

            Button botoTotFalta = (Button) findViewById(R.id.btn_tf);
            botoTotFalta.setOnClickListener(this);

            Button botoTotPresent = (Button) findViewById(R.id.btn_tp);
            botoTotPresent.setOnClickListener(this);

            Button botoEstatHoraAnterior = (Button) findViewById(R.id.btn_ha);
            botoEstatHoraAnterior.setOnClickListener(this);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void returnData(String callerID, String data, boolean error, HttpError errorData)  {
        //Retorna un conjunt de registres ("ca", "estatHoraAnterior")
        try {
            if (error)
            {
                Toast toast = Toast.makeText(getApplicationContext(),
                        errorData.toString(),
                        Toast.LENGTH_SHORT);
                toast.show();
            }
            else {
                if (callerID == PresenciaWebService.CALLER_getControlAssistencia) {
                    if (this.estatsControlAssistencia == null)
                        throw new Exception("Cal obtenir els Estats dels controls d'assistència per passar llista.");
                    JSONArray assistencies = new JSONArray(data);
                    for (int i = 0; i < assistencies.length(); i++) {
                        JSONObject camps = assistencies.getJSONObject(i).getJSONObject("ca").getJSONObject("fields");
                        JSONObject campsAlumne = assistencies.getJSONObject(i).getJSONObject("alumne").getJSONObject("fields");
                        String alumne = campsAlumne.getString("nom") + " " + campsAlumne.getString("cognoms");
                        int estatAnterior = -1;
                        if (!assistencies.getJSONObject(i).isNull("estatHoraAnterior"))
                        {
                            estatAnterior = assistencies.getJSONObject(i).getInt("estatHoraAnterior");
                        }
                        Log.e("DEBUG , hora anterior:", String.valueOf(estatAnterior));
                        int estat = -1;
                        if (!camps.isNull("estat")) {
                            estat = camps.getInt("estat");
                        }

                        TextView titleView = new TextView(this);
                        titleView.setText(alumne);

                        ScrollView sv = this.findViewById(R.id.scroll);
                        LinearLayout ll = sv.findViewById(R.id.mainLayout);
                        AssistenciaView assistenciaView = new AssistenciaView(this, this, this.estatsControlAssistencia);
                        assistenciaView.setTag(assistencies.getJSONObject(i).getJSONObject("ca").getString("pk"));
                        assistenciaView.setEstatActual(estat);
                        assistenciaView.setEstatHoraAnterior(estatAnterior);
                        vistesAssistencia.add(assistenciaView);

                        View linia = new View(this);
                        linia.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
                        linia.setBackgroundColor(Color.parseColor("#000000"));

                        ll.addView(titleView);
                        ll.addView(assistenciaView);
                        ll.addView(linia);
                    }
                } else if (callerID == PresenciaWebService.CALLER_getEstatControlAssistencia) {
                    JSONArray estats = new JSONArray(data);
                    for (int i = 0; i < estats.length(); i++) {
                        JSONObject camps = estats.getJSONObject(i).getJSONObject("fields");
                        estatsControlAssistencia.put(camps.getString("codi_estat"), estats.getJSONObject(i).getInt("pk"));
                    }
                } else if (callerID == PresenciaWebService.CALLER_putControlAssistencia)
                {
                    finish();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void OnStateChange(AssistenciaView source)
    {
        ;
    }

    @Override
    public void onClick(View v) {
        try {
            Button b = (Button )v;
            if (b.getId()==R.id.btnEnviar) {
                //Caldrà comprovar que totes les dades estiguin marcades.
                String json = "{" +
                        "                    \"pk\": \"%s\"," +
                        "                    \"estat\": \"%s\"\n" +
                        "                    }";
                String jSonAEnviar = "";
                for (AssistenciaView assistencia : vistesAssistencia) {
                    //Enviar les dades fent un post al servidor.
                    if (jSonAEnviar != "")
                        jSonAEnviar += ",";
                    jSonAEnviar += String.format(json, assistencia.getTag(), assistencia.getEstatActual());
                }
                pws.putControlAssistencia(this, this.pkImpartir, "[" + jSonAEnviar + "]");
            }
            else if(b.getId()==R.id.btn_tp)
            {
                for (AssistenciaView assistencia : vistesAssistencia) {
                    assistencia.setEstatActual(estatsControlAssistencia.get("P"));
                }
            }
            else if(b.getId()==R.id.btn_tf)
            {
                for (AssistenciaView assistencia : vistesAssistencia) {
                    assistencia.setEstatActual(estatsControlAssistencia.get("F"));
                }
            }
            else if (b.getId()==R.id.btn_ha)
            {
                for (AssistenciaView assistencia : vistesAssistencia) {
                    assistencia.setEstatActual(assistencia.getEstatAnterior());
                }
            }
        }
        catch (Exception e)
        {
            Toast toast = Toast.makeText(getApplicationContext(),
                    e.getMessage(),
                    Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}
