package mashup.com.buslocation;

import android.os.AsyncTask;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import static mashup.com.buslocation.R.id.map;

public class Usuario implements Serializable {

    private String idSocket = "";
    private String idUser = "";
    private String userName = "";
    private String tipoUsuario = "";
    private double latitud = 0.0;
    private double longitud = 0.0;
    private Marker marcador;

    private double destinoLatitud;

    private double destinoLongitud;

    private String mac = "";

    private String ruta = "";

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public Usuario(String idUser, String userName) {
        this.idUser = idUser;
        this.userName = userName;
    }

    public String getIdSocket() {
        return idSocket;
    }

    public void setIdSocket(String idSocket) {
        this.idSocket = idSocket;
    }

    public String getIdUser() {
        return idUser;
    }

    public void setIdUser(String idUser) {
        this.idUser = idUser;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(String tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    public Marker getMarcador() {
        return marcador;
    }

    public void setMarcadorMapa(GoogleMap mMap) {
        this.marcador = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(this.latitud, this.longitud))
                .title(this.userName)
                .snippet("Mis Coordenadas: " + this.latitud + ", " + this.longitud)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.persona)));

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(this.latitud, this.longitud), 20);
        mMap.animateCamera(cameraUpdate);
    }

    public void actualizarPosition() {
        marcador.setPosition(new LatLng(this.latitud, this.longitud));
        marcador.setTitle(this.userName);
        marcador.setSnippet("Mis Coordenadas: " + this.latitud + ", " + this.longitud);
        if(marcador.isInfoWindowShown()) {
            marcador.hideInfoWindow();
            marcador.showInfoWindow();
        }
    }

    public void setDistancia(GoogleMap mMap, double latitud, double longitud) {
        this.destinoLatitud = latitud;
        this.destinoLongitud = longitud;
        this.marcador = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(this.latitud, this.longitud))
                .title("Autobus: " + this.ruta + " Chofer: " + this.userName)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.autobus)));
        mostrarDatos();
    }

    public void actualizarPositionDistancia(double latitud, double longitud) {
        this.destinoLatitud = latitud;
        this.destinoLongitud = longitud;
        marcador.setPosition(new LatLng(this.latitud, this.longitud));
        marcador.setTitle("Autobus: " + this.ruta + " Chofer: " + this.userName);
        mostrarDatos();
        if(marcador.isInfoWindowShown()) {
            marcador.hideInfoWindow();
            marcador.showInfoWindow();
        }
    }

    public void mostrarDatos() {
        try {
            String datos = new Connection().execute(marcador, this.latitud, this.longitud, destinoLatitud, destinoLongitud).get().toString();
            marcador.setSnippet(datos);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void remover() {
        marcador.remove();
    }

    private class Connection extends AsyncTask {

        @Override
        protected Object doInBackground(Object... arg0) {
            String resultado = "";
            try {
                JSONObject datos = getJson("https://maps.googleapis.com/maps/api/directions/json?origin=" + arg0[1] + "," + arg0[2] + "&destination=" + arg0[3] + "," + arg0[4]);
                if(datos.has("routes")) {
                    JSONArray routes = datos.getJSONArray("routes");
                    for(int i = 0; i < routes.length(); i++) {
                        JSONObject routesObject = routes.getJSONObject(i);
                        JSONArray legs = routesObject.getJSONArray("legs");
                        for(int j = 0; j < legs.length(); j++) {
                            JSONObject legsObject = legs.getJSONObject(j);
                            JSONObject distance = legsObject.getJSONObject("distance");
                            JSONObject duration = legsObject.getJSONObject("duration");
                            String distanceText = distance.getString("text");
                            String durationText = duration.getString("text");
                            resultado = "Distancia: " + distanceText + ", Tiempo: " + durationText;
                        }
                    }
                }
            }
            catch(JSONException e) {
                System.out.println("JSONException: " + "Error...");
            }
            return resultado;
        }
    }

    public JSONObject getJson(String url) {
        String responce = null;
        HttpURLConnection conn = null;
        try {
            URL jUrl = new URL(url);
            conn = (HttpURLConnection)jUrl.openConnection();
            InputStream is = (InputStream)conn.getContent();
            java.util.Scanner scanner = new java.util.Scanner(is).useDelimiter("\\A");
            responce = scanner.hasNext() ? scanner.next() : "";
            return new JSONObject(responce);
        }
        catch(IOException ex) {
            String errTxt = "Internet access failure";
            if(conn != null && (ex instanceof java.io.FileNotFoundException))
                try {
                    errTxt = (new BufferedReader(new InputStreamReader(conn.getErrorStream()))).readLine();
                    if(errTxt == null)
                        errTxt = "(Server provided no ErrorStream)";
                }
                catch(Exception ex2) {
                    System.out.println(errTxt + " when retrieving ErrorStream" + " " + ex2);
                }
            System.out.println(errTxt + " " + ex);
        }
        catch(JSONException ex) {
        }
        catch (Exception ex) {
        }
        finally {
            if(conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }
}