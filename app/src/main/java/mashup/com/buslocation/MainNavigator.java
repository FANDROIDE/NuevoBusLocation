package mashup.com.buslocation;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Marker;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.User;



import android.graphics.Bitmap;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import android.widget.TextView;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ToggleButton;


import org.json.JSONObject;

import retrofit2.Call;

import static mashup.com.buslocation.R.id.map;

public class MainNavigator extends FragmentActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {
    private Usuario usuario;
    TextView nombre,correo;
    ImageView perfil;
    Bitmap Imagebitmap;
    String ImageUrl ;

    private GoogleMap mMap;

    private Marker marcador;

    private LinkedList<Usuario> listaUsuarios = new LinkedList();

    private TextView textview_coordenadas;

    private Button gps_button;

    private ToggleButton bluetooth_toggle_button;

    private BluetoothAdapter adaptadorBluetooth;

    private LocationManager locationManager;

    private LocationListener locationListener;

    private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";

    private static final Region ALL_ESTIMOTE_BEACONS = new Region("regionId", UUID.fromString(ESTIMOTE_PROXIMITY_UUID), null, null);

    BeaconManager beaconManager;

    public void BeaconManager() {
        beaconManager = new BeaconManager(getApplicationContext());
        beaconManager.setBackgroundScanPeriod(TimeUnit.SECONDS.toMillis(1), 0);
        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<Beacon> list) {
                usuario.setTipoUsuario("Chofer");
                usuario.setMac(list.get(0).getMacAddress().toString());
            }
            @Override
            public void onExitedRegion(Region region) {
                usuario.setTipoUsuario("Normal");
                usuario.setMac("");
                socket.emit("bluetoothApagado");
            }
        });
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startMonitoring(ALL_ESTIMOTE_BEACONS);
                }
                catch (Exception e) {

                }
            }
        });
    }

    private Socket socket;
    {
        try {
            socket = IO.socket("http://nodejs-proyectov16.44fs.preview.openshiftapps.com/");
        }
        catch(URISyntaxException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_navigator);

       // navHeaderView= navigationView.inflateHeaderView(R.layout.nav_header_main);
       // tvHeaderName= (TextView) navHeaderView.findViewById(R.id.tvHeaderName);
       // tvHeaderName.setText("Saly");


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);


        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);
        nombre = (TextView)headerView.findViewById(R.id.textViewNombre);
        correo=(TextView)headerView.findViewById(R.id.textViewCorreo);
        perfil=(ImageView)headerView.findViewById(R.id.imageViewPerfil);
        if(!verificarSesion()) {
            goLoginScreen();
        }
        //nombre.setText(usuario.getUserName());
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        socket.on("recibirCoordenadas", recibirCoordenadas);
        socket.on("eliminarCoordenadas", eliminarCoordenadas);
        socket.on("bluetoothApagadoAndroid", eliminarCoordenadas);
        socket.connect();

        textview_coordenadas = (TextView) findViewById(R.id.textview_coordenadas);

        bluetooth_toggle_button = (ToggleButton) findViewById(R.id.bluetooth_toggle_button);

        adaptadorBluetooth = BluetoothAdapter.getDefaultAdapter();
        if (adaptadorBluetooth == null) {
            bluetooth_toggle_button.setClickable(false);
        }
        estadoBluetooth();
        bluetooth_toggle_button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);
                }
                else {
                    adaptadorBluetooth.disable();
                    socket.emit("bluetoothApagado");
                }
            }
        });

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if(usuario.getLatitud() == 0.0 && usuario.getLongitud() == 0.0) {
                    usuario.setLatitud(location.getLatitude());
                    usuario.setLongitud(location.getLongitude());
                    usuario.setMarcadorMapa(mMap);
                }
                else {
                    usuario.setLatitud(location.getLatitude());
                    usuario.setLongitud(location.getLongitude());
                    usuario.actualizarPosition();
                }
                if(usuario.getTipoUsuario() == "Chofer") {
                    socket.emit("enviarCoordenadas", usuario.getIdUser() + "," + usuario.getUserName() + "," + usuario.getTipoUsuario() + "," + location.getLatitude() + "," + location.getLongitude() + "," + usuario.getMac());
                }
                textview_coordenadas.setText(usuario.getLatitud() + ", " + usuario.getLongitud());
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        BeaconManager();
    }

    public void verificarPermisos() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName()));
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            else {
                ActivityCompat.requestPermissions(this, new String[] {
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                }, 1);
            }
        }
        else {
            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(getApplicationContext(), "Iniciando el servicio GPS.", Toast.LENGTH_LONG).show();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        }
    }

    public void estadoBluetooth() {
        if(adaptadorBluetooth.isEnabled()) {
            bluetooth_toggle_button.setChecked(true);
        }
        else {
            bluetooth_toggle_button.setChecked(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        estadoBluetooth();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socket.disconnect();
        beaconManager.disconnect();
    }

    private Emitter.Listener recibirCoordenadas = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];

                    try {
                        //System.out.println(data.getString("ruta"));
                        if(listaUsuarios.size() == 0) {
                            Usuario nuevoUsuario = new Usuario(data.getString("idUser"), data.getString("userName"));
                            nuevoUsuario.setIdSocket(data.getString("idSocket"));
                            nuevoUsuario.setLatitud(Double.parseDouble(data.getString("latitud")));
                            nuevoUsuario.setLongitud(Double.parseDouble(data.getString("longitud")));
                            nuevoUsuario.setDistancia(mMap, usuario.getLatitud(), usuario.getLongitud());
                            nuevoUsuario.setRuta(data.getString("ruta"));
                            listaUsuarios.add(nuevoUsuario);
                        }
                        else {
                            for(int i = 0; i < listaUsuarios.size(); i++) {
                                Usuario listaUsuario = listaUsuarios.get(i);
                                if(listaUsuario.getIdSocket().equals(data.getString("idSocket"))) {
                                    listaUsuario.setLatitud(Double.parseDouble(data.getString("latitud")));
                                    listaUsuario.setLongitud(Double.parseDouble(data.getString("longitud")));
                                    listaUsuario.actualizarPositionDistancia(usuario.getLatitud(), usuario.getLongitud());
                                    return;
                                }
                            }
                            Usuario nuevoUsuario = new Usuario(data.getString("idUser"), data.getString("userName"));
                            nuevoUsuario.setIdSocket(data.getString("idSocket"));
                            nuevoUsuario.setLatitud(Double.parseDouble(data.getString("latitud")));
                            nuevoUsuario.setLongitud(Double.parseDouble(data.getString("longitud")));
                            nuevoUsuario.setDistancia(mMap, usuario.getLatitud(), usuario.getLongitud());
                            listaUsuarios.add(nuevoUsuario);
                        }
                    }
                    catch(JSONException e) {
                        return;
                    }
                }
            });
        }
    };

    private Emitter.Listener eliminarCoordenadas = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];

                    try {
                        for(int i = 0; i < listaUsuarios.size(); i++) {
                            Usuario listaUsuario = listaUsuarios.get(i);
                            if(listaUsuario.getIdSocket().equals(data.getString("idSocket"))) {
                                listaUsuario.remover();
                                listaUsuarios.remove(i);
                            }
                        }
                    }
                    catch(JSONException e) {
                        return;
                    }
                }
            });
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch(requestCode) {
            case 1: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Los permisos fueron otorgados.", Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Los permisos fueron denegados.", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_navigator, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_gps) {
            verificarPermisos();
            //nombre.setText(usuario.getUserName());
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_BT) {

        } else if (id == R.id.nav_desarrolladores) {
            Toast.makeText(this, "Flores Perez Mario Andrez\nMartinez Santiago Feliciano\nPerez Hernandez Gabriela Ivet\n   Ruiz Gonzalez Alexander\nSanchez Villegas Esteban", Toast.LENGTH_SHORT).show();

        }else if(id== R.id.nav_cerrar){
            cerrarSesion();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    public boolean verificarSesion() {

        if(Twitter.getInstance().core.getSessionManager().getActiveSession() != null) {

            TwitterSession session = Twitter.getSessionManager().getActiveSession();
            Call<User> userResult = Twitter.getApiClient(session).getAccountService().verifyCredentials(true, false);
            userResult.enqueue(new Callback<User>() {
                @Override
                public void failure(TwitterException e) {
                }

                @Override
                public void success(Result<User> userResult) {
                    User user = userResult.data;
                    usuario = new Usuario(Long.toString(user.getId()), user.name);
                    nombre.setText(user.name);
                    correo.setText(user.screenName);
                    ImageUrl=user.profileImageUrl;
                    new ImageLoaderClass().execute(ImageUrl);
                }
            });

            return true;
        }
        if(AccessToken.getCurrentAccessToken() != null) {

            Bundle params = new Bundle();
            params.putString("fields", "id, name, email, picture");
            new GraphRequest(
                    AccessToken.getCurrentAccessToken(), "/me", params, HttpMethod.GET,
                    new GraphRequest.Callback() {
                        public void onCompleted(GraphResponse response) {
                            try {
                                JSONObject data = response.getJSONObject();
                                usuario = new Usuario(data.getString("id"), data.getString("name"));
                                nombre.setText(data.getString("name"));
                                correo.setText(data.getString("email"));
                                JSONObject data1 = response.getJSONObject().getJSONObject("picture").getJSONObject("data");
                                ImageUrl=data1.getString("url");
                                new ImageLoaderClass().execute(ImageUrl);

                            }
                            catch(Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
            ).executeAsync();

            return true;
        }
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }


    private class ImageLoaderClass extends AsyncTask<String, String, Bitmap> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }
        protected Bitmap doInBackground(String... args) {
            try {
                Imagebitmap = BitmapFactory.decodeStream((InputStream)new URL(args[0]).getContent());

            } catch (Exception e) {
                e.printStackTrace();
            }
            return Imagebitmap;
        }

        protected void onPostExecute(Bitmap image) {

            if(image != null){
                perfil.setImageBitmap(image);

            }
        }
    }

    public void cerrarSesion() {
        if(Twitter.getInstance().core.getSessionManager().getActiveSession() != null) {
            Twitter.getInstance().core.getSessionManager().clearActiveSession();
        }
        if(AccessToken.getCurrentAccessToken() != null) {
            LoginManager.getInstance().logOut();
        }
        goLoginScreen();
    }

    private void goLoginScreen() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }




}
