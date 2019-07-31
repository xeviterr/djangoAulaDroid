package cat.institutmontilivi.djau;

import org.json.JSONObject;

import java.util.Date;
import java.text.SimpleDateFormat;

import cat.institutmontilivi.djau.Exceptions.UserNotFoundException;

public class PresenciaWebService {

    public static final String CALLER_doLogin = "doLogin";
    public static final String CALLER_getImpartirPerData = "getImpartirPerData";
    public static final String CALLER_getControlAssistencia = "getControlAssistencia";
    public static final String CALLER_putControlAssistencia = "putControlAssistencia";
    public static final String CALLER_getEstatsControlAssistencia = "getEstatControlAssistencia";
    public static final String CALLER_getProfes = "getProfes";
    public static final String CALLER_getFrangesHoraries = "getFrangesHoraries";
    public static final String CALLER_putGuardia = "postGuardia";
    public static final String CALLER_getAPILevel = "getAPILevel";

    public interface ICallBackaActivityGetString{
        public void returnData(String callerID, String data, boolean error, HttpError detallsError);
        public void runOnUiThread(Runnable action);
    }

    HttpConnection con = null;
    String url = "";
    String username = "";

    public PresenciaWebService(HttpConnection con, String url, String username)
    {
        this.con = con;
        this.url = url;
        this.username = username;
    }

    private void existeixUsuari() throws UserNotFoundException {
        if (this.username.equals(""))
            throw new UserNotFoundException("Ens cal configurar l'usuari per fer una petició web");
    }

    public void doLogin(final ICallBackaActivityGetString activitatQueCrida, final String password) throws UserNotFoundException {
        existeixUsuari();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StringBuilder result = new StringBuilder();
                    result.append("username=" + username);
                    result.append("&");
                    result.append("password=" + password);

                    final String data = con.sendData(result.toString(),url + "/login/", "POST");
                    JSONObject tokenJson = new JSONObject(data);
                    //Assigna el token d'autenticació.
                    con.setToken(tokenJson.getString("token"));

                    activitatQueCrida.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activitatQueCrida.returnData(CALLER_doLogin, data, false, null );
                        }
                    });
                } catch (final HttpErrorException e) {
                    e.printStackTrace();
                    activitatQueCrida.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activitatQueCrida.returnData(CALLER_doLogin,"", true, new HttpError(e.getErrorCode(), e.getMessage()));
                        }
                    });
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
    public void getImpartirPerData(final ICallBackaActivityGetString activitatQueCrida, final String dataImpartirYYYYMMDD) throws UserNotFoundException {
        existeixUsuari();
        String urlCrida = url + "/getImpartirPerData/" + dataImpartirYYYYMMDD +"/" + username;
        executaRebreEnBackground(activitatQueCrida, CALLER_getImpartirPerData, urlCrida);
    }

    public void getControlAssistencia(final ICallBackaActivityGetString activitatQueCrida, final String idImpartir) throws UserNotFoundException {
        existeixUsuari();
        String urlCrida = url + "/getControlAssistencia/" + idImpartir +"/" + username;
        executaRebreEnBackground(activitatQueCrida, CALLER_getControlAssistencia, urlCrida);
    }

    public void getEstatControlAssistencia(final ICallBackaActivityGetString activitatQueCrida) throws UserNotFoundException {
        existeixUsuari();
        String urlCrida = url + "/getEstatsControlAssistencia/";
        executaRebreEnBackground(activitatQueCrida, CALLER_getEstatsControlAssistencia, urlCrida);
    }

    public void getProfes(final ICallBackaActivityGetString activitatQueCrida) throws UserNotFoundException {
        existeixUsuari();
        String urlCrida = url + "/getProfes/";
        executaRebreEnBackground(activitatQueCrida, CALLER_getProfes, urlCrida);
    }

    public void getFrangesHoraries(final ICallBackaActivityGetString activitatQueCrida) throws UserNotFoundException {
        existeixUsuari();
        String urlCrida = url + "/getFrangesHoraries/";
        executaRebreEnBackground(activitatQueCrida, CALLER_getFrangesHoraries, urlCrida);
    }

    public void getAPILevel(final ICallBackaActivityGetString activitatQueCrida) throws UserNotFoundException {
        existeixUsuari();
        String urlCrida = url + "/getAPILevel/";
        executaRebreEnBackground(activitatQueCrida, CALLER_getAPILevel, urlCrida);
    }

    public void putGuardia(final ICallBackaActivityGetString activitatQueCrida, final String idUsuariASubstituir, final String idUsuari, final String idFranja, final Date diaAImpartir) throws UserNotFoundException {
        existeixUsuari();
        String urlCrida = url + "/putGuardia/";
        String dadesJSON = "{\"idUsuariASubstituir\":\"%s\", " +
            "\"idUsuari\":\"%s\"," +
            "\"idFranja\":\"%s\"," +
            "\"diaAImpartir\":\"%s\"}";
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        executaEnviarEnBackground(activitatQueCrida, CALLER_putGuardia, urlCrida, String.format(dadesJSON, idUsuariASubstituir, idUsuari, idFranja, format.format(diaAImpartir)));

    }

    public void putControlAssistencia(final ICallBackaActivityGetString activitatQueCrida, final String idImpartir, final String dadesJSON) throws UserNotFoundException {
        existeixUsuari();
        String urlCrida = url + "/putControlAssistencia/" + idImpartir + "/" + username + "/";
        executaEnviarEnBackground(activitatQueCrida, CALLER_putControlAssistencia, urlCrida, dadesJSON);
    }

    private void executaEnviarEnBackground(final ICallBackaActivityGetString activitatQueCrida, final String callerID, final String url, final String dadesJSON)
    {
        new Thread(new Runnable() {
            String data="";
            boolean error = false;
            HttpError errorObj = null;
            @Override
            public void run() {
                try {
                    data = con.sendData(dadesJSON, url, "PUT");
                }
                catch (HttpErrorException e)
                {
                    error=true;
                    errorObj = new HttpError(e.getMessage(), e.getErrorCode());
                    e.printStackTrace();
                }
                catch (Exception e) {
                    error = true;
                    errorObj = new HttpError(e.getMessage(), "0");
                    e.printStackTrace();
                }

                //Crida el callback amb el thread principal.
                activitatQueCrida.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activitatQueCrida.returnData(CALLER_putControlAssistencia, data, error, errorObj);
                    }
                });
            }
        }).start();
    }


    private void executaRebreEnBackground(final ICallBackaActivityGetString activitatQueCrida, final String callerID, final String url)
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

