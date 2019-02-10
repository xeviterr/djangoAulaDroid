package com.example.djau;

import android.widget.Toast;

import java.util.Date;
import java.text.SimpleDateFormat;

public class PresenciaWebService {

    public static final String CALLER_doLogin = "doLogin";
    public static final String CALLER_getImpartirPerData = "getImpartirPerData";
    public static final String CALLER_getControlAssistencia = "getControlAssistencia";
    public static final String CALLER_putControlAssistencia = "putControlAssistencia";
    public static final String CALLER_getEstatControlAssistencia = "getEstatControlAssistencia";
    public static final String CALLER_getProfes = "getProfes";
    public static final String CALLER_getFrangesHoraries = "getFrangesHoraries";
    public static final String CALLER_putGuardia = "putGuardia";

    public interface ICallBackaActivityGetString{
        public void returnData(String callerID, String data, boolean error, HttpError detallsError);
        public void runOnUiThread(Runnable action);
    }

    HttpPersistentConnection con = null;
    String url = "";
    String username = "";

    public PresenciaWebService(HttpPersistentConnection con, String url, String username)
    {
        this.con = con;
        this.url = url;
        this.username = username;
    }

    public void doLogin(final ICallBackaActivityGetString activitatQueCrida, final String password) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String data = con.requestData(url + "/login/" + username);
                    activitatQueCrida.returnData(CALLER_doLogin, data, false, null );
                } catch (HttpErrorException e) {
                    e.printStackTrace();
                    activitatQueCrida.returnData(CALLER_doLogin,"", true, new HttpError(e.getErrorCode(), e.getMessage()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Retorna les classes a impartir d'un usuari en una data donada.
     * @param activitatQueCrida
     * @param dataImpartirYYYYMMDD Crida la data en aquest format: AAAA-MM-DD
     */
    public void getImpartirPerData(final ICallBackaActivityGetString activitatQueCrida, final String dataImpartirYYYYMMDD)
    {
        String urlCrida = url + "/getImpartirPerData/" + dataImpartirYYYYMMDD +"/" + username;
        executaEnBackground(activitatQueCrida, CALLER_getImpartirPerData, urlCrida);
    }

    public void getControlAssistencia(final ICallBackaActivityGetString activitatQueCrida, final String idImpartir)
    {
        String urlCrida = url + "/getControlAssistencia/" + idImpartir +"/" + username;
        executaEnBackground(activitatQueCrida, CALLER_getControlAssistencia, urlCrida);
    }

    public void getEstatControlAssistencia(final ICallBackaActivityGetString activitatQueCrida)
    {
        String urlCrida = url + "/getEstatControlAssistencia/" + username;
        executaEnBackground(activitatQueCrida, CALLER_getEstatControlAssistencia, urlCrida);
    }

    public void getProfes(final ICallBackaActivityGetString activitatQueCrida)
    {
        String urlCrida = url + "/getProfes/" + username;
        executaEnBackground(activitatQueCrida, CALLER_getProfes, urlCrida);
    }

    public void getFrangesHoraries(final ICallBackaActivityGetString activitatQueCrida)
    {
        String urlCrida = url + "/getFrangesHoraries/" + username;
        executaEnBackground(activitatQueCrida, CALLER_getFrangesHoraries, urlCrida);
    }

    public void putGuardia(final ICallBackaActivityGetString activitatQueCrida, final String idUsuariASubstituir, final String idUsuari, final String idFranja, final Date diaAImpartir)
    {
        String urlCrida = url + "/putGuardia/" + username + "/";
        String dadesJSON = "{\"idUsuariASubstituir\":\"%s\", " +
            "\"idUsuari\":\"%s\"," +
            "\"idFranja\":\"%s\"," +
            "\"diaAImpartir\":\"%s\"}";
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        executaEnviarEnBackground(activitatQueCrida, CALLER_putGuardia, urlCrida, String.format(dadesJSON, idUsuariASubstituir, idUsuari, idFranja, format.format(diaAImpartir)));

    }

    public void putControlAssistencia(final ICallBackaActivityGetString activitatQueCrida, final String idImpartir, final String dadesJSON)
    {
        String urlCrida = url + "/putControlAssistencia/" + idImpartir + "/" + username + "/";
        executaEnviarEnBackground(activitatQueCrida, CALLER_putControlAssistencia, urlCrida, dadesJSON);
    }

    private void executaEnviarEnBackground(final ICallBackaActivityGetString activitatQueCrida, final String callerID, final String url, final String dadesJSON)
    {
        new Thread(new Runnable() {
            String data="";
            boolean error = false;
            String errorMsg = "";
            String errorCode = "0";
            @Override
            public void run() {
                try {
                    data = con.sendJSONData(dadesJSON, url, "PUT");
                }
                catch (HttpErrorException e)
                {
                    error=true;
                    errorMsg=e.getMessage();
                    errorCode=e.getErrorCode();
                    e.printStackTrace();
                }
                catch (Exception e) {
                    error = true;
                    errorMsg = e.getMessage();
                    e.printStackTrace();
                }

                //Crida el callback amb el thread principal.
                activitatQueCrida.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activitatQueCrida.returnData(CALLER_putControlAssistencia, data, error, new HttpError(errorCode, errorMsg));
                    }
                });
            }
        }).start();
    }


    private void executaEnBackground(final ICallBackaActivityGetString activitatQueCrida, final String callerID, final String url)
    {
        new Thread(new Runnable() {
            String data="";
            boolean error = false;
            HttpError err = null;
            @Override
            public void run() {
                try
                {
                    data = con.requestData(url);
                }
                catch (HttpErrorException e)
                {

                    error = true;
                    err = new HttpError(e.getMessage(), e.getErrorCode());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    error = true;
                    err = new HttpError(e.getMessage(), "0");
                }


                //Crida el callback amb el thread principal.
                activitatQueCrida.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activitatQueCrida.returnData(callerID, data, error, err );
                    }
                });
            }
        }).start();
    }

}

