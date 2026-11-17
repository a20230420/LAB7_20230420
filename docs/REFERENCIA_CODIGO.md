# Fuentes de LAB6_20230420

Código de sesiones de estudio. Los archivos reales del proyecto son los que compila Android. No incluye google-services.json ni claves privadas.

## app/src/main/AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET"/>
    <application android:name=".EstudioApp" android:theme="@style/Theme.Estudio" android:label="@string/app_name" android:icon="@drawable/ic_estudio" android:allowBackup="false" android:supportsRtl="true">
        <activity android:name=".GraficosActivity" android:exported="false"/>
        <activity android:name=".FormularioSesionActivity" android:exported="false" android:windowSoftInputMode="adjustResize"/>
        <activity android:name=".SesionesActivity" android:exported="false"/>
        <activity android:name=".LoginActivity" android:exported="true">
            <intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>
        </activity>
    </application>
</manifest>
```

## app/src/main/java/com/example/lab06_20230420/EstadisticasEstudio.java

```java
package com.example.lab06_20230420;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public final class EstadisticasEstudio {
    public static final String[] ACTIVIDADES={"Teoría","Práctica","Laboratorio"};
    private EstadisticasEstudio() { }
    public static String cursoLimpio(String curso) { return curso==null?"":curso.trim().replaceAll("\\s+"," "); }
    public static double leerHoras(String texto) {
        String value=texto.trim().replace(',','.');
        if(!value.matches("[0-9]+(?:\\.[0-9]+)?")) throw new IllegalArgumentException("Ingresa horas numéricas; puedes usar coma o punto decimal.");
        double horas=Double.parseDouble(value);
        if(!Double.isFinite(horas)||horas<=0||horas>24) throw new IllegalArgumentException("Las horas deben ser mayores que 0 y como máximo 24 por sesión.");
        return horas;
    }
    public static void validar(SesionEstudio s) {
        if(cursoLimpio(s.getCurso()).isEmpty()||s.getCurso().length()>100) throw new IllegalArgumentException("Ingresa un curso de hasta 100 caracteres.");
        try { if(!s.getFecha().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))throw new IllegalArgumentException();LocalDate.parse(s.getFecha()); } catch(Exception e) { throw new IllegalArgumentException("Selecciona una fecha válida."); }
        if(!Double.isFinite(s.getHoras())||s.getHoras()<=0||s.getHoras()>24) throw new IllegalArgumentException("Las horas deben estar entre más de 0 y 24.");
        if(!Arrays.asList(ACTIVIDADES).contains(s.getTipoActividad())) throw new IllegalArgumentException("Selecciona un tipo de actividad válido.");
    }
    public static List<SesionEstudio> filtrar(List<SesionEstudio> todas,String curso) {
        List<SesionEstudio> r=new ArrayList<>();
        for(SesionEstudio s:todas) if(curso==null||cursoLimpio(s.getCurso()).equalsIgnoreCase(cursoLimpio(curso))) r.add(s);
        return r;
    }
    public static List<String> cursos(List<SesionEstudio> sesiones) {
        Set<String> cursos=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for(SesionEstudio s:sesiones) cursos.add(cursoLimpio(s.getCurso()));
        return new ArrayList<>(cursos);
    }
    public static Map<String,Double> porMes(List<SesionEstudio> sesiones) {
        Map<String,Double> r=new TreeMap<>();
        for(SesionEstudio s:sesiones) r.merge(YearMonth.from(LocalDate.parse(s.getFecha())).toString(),s.getHoras(),Double::sum);
        return r;
    }
    public static Map<String,Double> porActividad(List<SesionEstudio> sesiones) {
        Map<String,Double> r=new LinkedHashMap<>();
        for(String actividad:ACTIVIDADES) r.put(actividad,0d);
        for(SesionEstudio s:sesiones) r.merge(s.getTipoActividad(),s.getHoras(),Double::sum);
        return r;
    }
    public static double total(List<SesionEstudio> sesiones) { double t=0; for(SesionEstudio s:sesiones)t+=s.getHoras();return t; }
    public static String horas(double value) { return String.format(new Locale("es","PE"),"%.2f",value).replaceAll("0+$","").replaceAll("[.,]$",""); }
}
```

## app/src/main/java/com/example/lab06_20230420/EstudioApp.java

```java
package com.example.lab06_20230420;
import android.app.Application;
import android.app.LocaleManager;
import android.os.LocaleList;
public class EstudioApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        getSystemService(LocaleManager.class).setApplicationLocales(LocaleList.forLanguageTags("es"));
    }
}
```

## app/src/main/java/com/example/lab06_20230420/FormularioSesionActivity.java

```java
package com.example.lab06_20230420;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import com.example.lab06_20230420.databinding.ActivityFormularioSesionBinding;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class FormularioSesionActivity extends PantallaPrivada {
    private ActivityFormularioSesionBinding binding;
    private String id;
    private boolean editar, ocupada, cargada;
    private LocalDate fecha=LocalDate.now();
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);if(isFinishing()||repository==null)return;
        binding=ActivityFormularioSesionBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());ajustarBarras();
        binding.tipo.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,EstadisticasEstudio.ACTIVIDADES));
        String existente=getIntent().getStringExtra("sesionId");editar=existente!=null;
        id=editar?existente:saved!=null?saved.getString("nuevoId"):repository.nuevoId();
        if(id==null||!id.matches("[A-Za-z0-9_-]{1,150}")){aviso("Identificador de sesión inválido.");finish();return;}
        binding.titulo.setText(editar?"Editar sesión":"Nueva sesión");
        binding.volver.setOnClickListener(v->finish());binding.fecha.setOnClickListener(v->elegirFecha());binding.guardar.setOnClickListener(v->guardar());
        if(saved!=null&&saved.getBoolean("cargada")){cargada=true;fecha=LocalDate.parse(saved.getString("fecha"));mostrarFecha();}
        else if(editar)cargar(); else {cargada=true;mostrarFecha();}
    }
    private void elegirFecha(){if(ocupada)return;new DatePickerDialog(this,(v,a,m,d)->{fecha=LocalDate.of(a,m+1,d);mostrarFecha();},fecha.getYear(),fecha.getMonthValue()-1,fecha.getDayOfMonth()).show();}
    private void mostrarFecha(){binding.fecha.setText(fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));}
    private void cargando(boolean valor){ocupada=valor;binding.guardar.setEnabled(!valor);binding.curso.setEnabled(!valor);binding.horas.setEnabled(!valor);binding.tipo.setEnabled(!valor);binding.fecha.setEnabled(!valor);}
    private void cargar(){
        cargando(true);binding.error.setText("Cargando sesión…");
        repository.buscar(id).addOnCompleteListener(this,t->{
            if(!t.isSuccessful()){binding.error.setText("No se pudo cargar. Vuelve a la lista e intenta de nuevo.");return;}
            if(t.getResult()==null||!t.getResult().exists()){aviso("Esta sesión ya no existe.");finish();return;}
            try {
                SesionEstudio s=t.getResult().toObject(SesionEstudio.class);if(s==null)throw new IllegalStateException();EstadisticasEstudio.validar(s);
                binding.curso.setText(s.getCurso());binding.horas.setText(EstadisticasEstudio.horas(s.getHoras()));
                binding.tipo.setSelection(Arrays.asList(EstadisticasEstudio.ACTIVIDADES).indexOf(s.getTipoActividad()));
                fecha=LocalDate.parse(s.getFecha());mostrarFecha();binding.error.setText("");cargada=true;cargando(false);
            }catch(Exception e){binding.error.setText("La sesión tiene datos inválidos. Revisa el documento en Firestore.");}
        });
    }
    private void guardar(){
        if(ocupada)return;
        SesionEstudio s;
        try {
            s=new SesionEstudio(id,EstadisticasEstudio.cursoLimpio(binding.curso.getText().toString()),fecha.toString(),
                EstadisticasEstudio.leerHoras(binding.horas.getText().toString()),binding.tipo.getSelectedItem().toString());
            EstadisticasEstudio.validar(s);
        }catch(IllegalArgumentException e){binding.error.setText(e.getMessage());return;}
        cargando(true);binding.error.setText("Guardando en Firestore… Si no hay conexión, espera a recuperarla.");
        repository.guardar(s,editar).addOnCompleteListener(this,t->{
            if(t.isSuccessful()){aviso(editar?"Sesión actualizada.":"Sesión registrada.");finish();}
            else{cargando(false);binding.error.setText("No se pudo guardar. Revisa conexión y permisos. Si la sesión fue eliminada, vuelve a la lista.");}
        });
    }
    @Override protected void onSaveInstanceState(Bundle out){if(binding!=null){out.putString("fecha",fecha.toString());out.putString("nuevoId",id);out.putBoolean("cargada",cargada);}super.onSaveInstanceState(out);}
}
```

## app/src/main/java/com/example/lab06_20230420/GraficoHorasView.java

```java
package com.example.lab06_20230420;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Representa SUMAS DE HORAS: nunca cuenta documentos para calcular estos gráficos. */
public class GraficoHorasView extends View {
    private final Map<String,Double> valores;
    private final boolean circular;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] colores={0xff2454b7,0xffe79a35,0xff16978d};
    public GraficoHorasView(Context context,Map<String,Double> valores,boolean circular){
        super(context);this.valores=new LinkedHashMap<>(valores);this.circular=circular;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        StringBuilder descripcion=new StringBuilder(circular?"Horas por actividad. ":"Horas por mes. ");
        for(Map.Entry<String,Double> e:valores.entrySet())descripcion.append(etiqueta(e.getKey())).append(": ").append(EstadisticasEstudio.horas(e.getValue())).append(" horas. ");
        setContentDescription(descripcion.toString());
    }
    public int alturaDp(){return circular?340:Math.max(90,valores.size()*56+16);}
    private String etiqueta(String key){return circular?key:YearMonth.parse(key).format(DateTimeFormatter.ofPattern("MMM yyyy",new Locale("es","PE")));}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float d=getResources().getDisplayMetrics().density;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.NORMAL));paint.setTextSize(13*d);
        double total=0,max=0;for(double n:valores.values()){total+=n;max=Math.max(max,n);}
        if(total<=0){paint.setColor(0xff59667a);canvas.drawText("Sin horas para este curso",12*d,36*d,paint);return;}
        if(circular){
            float size=Math.min(getWidth()-32*d,200*d),left=(getWidth()-size)/2;
            RectF box=new RectF(left,12*d,left+size,12*d+size);float angulo=-90;int i=0;
            for(Map.Entry<String,Double> e:valores.entrySet()){
                paint.setColor(colores[i%colores.length]);float arco=(float)(360*e.getValue()/total);
                if(arco>0)canvas.drawArc(box,angulo,arco,true,paint);angulo+=arco;
                float y=size+44*d+i*28*d;canvas.drawRoundRect(12*d,y-12*d,24*d,y,3*d,3*d,paint);
                paint.setColor(0xff172944);canvas.drawText(e.getKey()+": "+EstadisticasEstudio.horas(e.getValue())+" h ("+Math.round(100*e.getValue()/total)+"%)",32*d,y,paint);i++;
            }
        }else{
            int i=0;float ancho=Math.max(0,getWidth()-24*d);
            for(Map.Entry<String,Double> e:valores.entrySet()){
                float y=12*d+i*56*d;paint.setColor(0xff172944);canvas.drawText(etiqueta(e.getKey()),12*d,y+13*d,paint);
                String horas=EstadisticasEstudio.horas(e.getValue())+" h";
                canvas.drawText(horas,getWidth()-12*d-paint.measureText(horas),y+13*d,paint);
                paint.setColor(0xffe7edf8);canvas.drawRoundRect(12*d,y+23*d,12*d+ancho,y+37*d,5*d,5*d,paint);
                paint.setColor(colores[0]);canvas.drawRoundRect(12*d,y+23*d,12*d+(float)(ancho*e.getValue()/max),y+37*d,5*d,5*d,paint);i++;
            }
        }
    }
}
```

## app/src/main/java/com/example/lab06_20230420/GraficosActivity.java

```java
package com.example.lab06_20230420;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.lab06_20230420.databinding.ActivityGraficosBinding;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.*;

public class GraficosActivity extends PantallaPrivada {
    private ActivityGraficosBinding binding;
    private ListenerRegistration listener;
    private List<SesionEstudio> sesiones=new ArrayList<>();
    private String curso;
    private boolean preparando;
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);if(isFinishing()||repository==null)return;
        binding=ActivityGraficosBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());ajustarBarras();
        if(saved!=null)curso=saved.getString("curso");
        binding.volver.setOnClickListener(v->finish());binding.reintentar.setOnClickListener(v->escuchar());
        binding.filtroCurso.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onNothingSelected(AdapterView<?> p){}
            @Override public void onItemSelected(AdapterView<?> p,View v,int posicion,long id){
                if(preparando)return;
                curso=posicion==0?null:(String)p.getItemAtPosition(posicion);dibujar();
            }
        });
    }
    @Override protected void onStart(){super.onStart();if(binding!=null&&!isFinishing())escuchar();}
    @Override protected void onStop(){detener();super.onStop();}
    private void detener(){if(listener!=null){listener.remove();listener=null;}}
    private void escuchar(){
        detener();binding.estado.setText("Cargando horas de estudio…");binding.reintentar.setVisibility(View.GONE);
        listener=repository.escuchar((datos,cache,pendientes,error)->{
            if(error!=null){binding.estado.setText("No se pudieron cargar los gráficos. Revisa conexión y reglas de Firestore.");binding.reintentar.setVisibility(View.VISIBLE);return;}
            sesiones=datos;actualizarCursos();dibujar();
            binding.estado.setText(pendientes?"Hay cambios pendientes de sincronizar.":cache?"Gráficos calculados con la caché; esperando al servidor.":"Gráficos actualizados en tiempo real");
        });
    }
    private void actualizarCursos(){
        preparando=true;
        List<String> opciones=new ArrayList<>();opciones.add("Todos los cursos");opciones.addAll(EstadisticasEstudio.cursos(sesiones));
        int seleccion=0;
        if(curso!=null){for(int i=1;i<opciones.size();i++)if(opciones.get(i).equalsIgnoreCase(curso)){seleccion=i;break;}}
        curso=seleccion==0?null:opciones.get(seleccion);
        binding.filtroCurso.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,opciones));binding.filtroCurso.setSelection(seleccion);preparando=false;
    }
    private void dibujar(){
        List<SesionEstudio> filtradas=EstadisticasEstudio.filtrar(sesiones,curso);
        binding.resumen.setText(EstadisticasEstudio.horas(EstadisticasEstudio.total(filtradas))+" horas · "+filtradas.size()+(filtradas.size()==1?" sesión":" sesiones"));
        grafico(binding.barras,EstadisticasEstudio.porMes(filtradas),false);
        grafico(binding.torta,EstadisticasEstudio.porActividad(filtradas),true);
    }
    private void grafico(FrameLayout contenedor,Map<String,Double> datos,boolean circular){
        contenedor.removeAllViews();GraficoHorasView grafico=new GraficoHorasView(this,datos,circular);
        contenedor.addView(grafico,new FrameLayout.LayoutParams(-1,(int)(getResources().getDisplayMetrics().density*grafico.alturaDp())));
    }
    @Override protected void onSaveInstanceState(Bundle out){out.putString("curso",curso);super.onSaveInstanceState(out);}
}
```

## app/src/main/java/com/example/lab06_20230420/LoginActivity.java

```java
package com.example.lab06_20230420;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import com.example.lab06_20230420.databinding.ActivityLoginBinding;
import com.firebase.ui.auth.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Arrays;

/** FirebaseUI se encarga de registro, contraseña e inicio por correo/Google. */
public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private final ActivityResultLauncher<Intent> launcher=registerForActivityResult(
        new FirebaseAuthUIActivityResultContract(),result->{
            if(auth!=null&&auth.getCurrentUser()!=null){entrar();return;}
            binding.abrirAcceso.setEnabled(true);
            binding.estadoLogin.setText(result.getIdpResponse()==null?"Acceso cancelado. Puedes intentarlo de nuevo.":"No se completó el acceso. Revisa la conexión y la configuración del proveedor.");
        });
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);binding=ActivityLoginBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());
        if(FirebaseApp.initializeApp(this)!=null){auth=FirebaseAuth.getInstance();auth.setLanguageCode("es");}
        if(auth!=null&&auth.getCurrentUser()!=null){entrar();return;}
        binding.abrirAcceso.setOnClickListener(v->abrir());
        if(auth==null)binding.estadoLogin.setText("Falta app/google-services.json de Lab6-20230420. Consulta README para conectar Firebase.");
        else if(saved==null)abrir();
    }
    private void abrir(){
        if(auth==null)return;
        if(getResources().getIdentifier("default_web_client_id","string",getPackageName())==0){
            binding.estadoLogin.setText("El archivo Firebase todavía no contiene la configuración de Google. Habilita Google, registra la firma SHA y descarga google-services.json actualizado; luego sincroniza y ejecuta otra vez.");return;
        }
        binding.abrirAcceso.setEnabled(false);
        AuthMethodPickerLayout layout=new AuthMethodPickerLayout.Builder(R.layout.firebase_login)
            .setEmailButtonId(R.id.accesoCorreo).setGoogleButtonId(R.id.accesoGoogle).build();
        Intent intent=AuthUI.getInstance().createSignInIntentBuilder()
            .setAvailableProviders(Arrays.asList(new AuthUI.IdpConfig.EmailBuilder().setRequireName(false).build(),new AuthUI.IdpConfig.GoogleBuilder().build()))
            .setAuthMethodPickerLayout(layout).setTheme(R.style.Theme_Estudio_Auth)
            .setLogo(R.drawable.ic_estudio).setCredentialManagerEnabled(false).build();
        launcher.launch(intent);
    }
    private void entrar(){startActivity(new Intent(this,SesionesActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));finish();}
}
```

## app/src/main/java/com/example/lab06_20230420/PantallaPrivada.java

```java
package com.example.lab06_20230420;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.*;

public abstract class PantallaPrivada extends AppCompatActivity {
    protected FirebaseAuth auth;
    protected SesionRepository repository;
    private String uid;
    private final FirebaseAuth.AuthStateListener cambio=a->{if(a.getCurrentUser()==null||!a.getCurrentUser().getUid().equals(uid)) salir();};
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        if(FirebaseApp.initializeApp(this)==null){salir();return;}
        auth=FirebaseAuth.getInstance();
        if(auth.getCurrentUser()==null){salir();return;}
        uid=auth.getCurrentUser().getUid();repository=new SesionRepository(uid);
    }
    @Override protected void onStart(){super.onStart();if(auth!=null)auth.addAuthStateListener(cambio);}
    @Override protected void onStop(){if(auth!=null)auth.removeAuthStateListener(cambio);super.onStop();}
    protected void salir(){startActivity(new Intent(this,LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));finish();}
    protected void aviso(String texto){Toast.makeText(this,texto,Toast.LENGTH_LONG).show();}
    protected void ajustarBarras(){ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main),(v,insets)->{
        androidx.core.graphics.Insets b=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.ime());
        v.setPadding(b.left,b.top,b.right,b.bottom);return insets;
    });}
}
```

## app/src/main/java/com/example/lab06_20230420/SesionAdapter.java

```java
package com.example.lab06_20230420;

import android.content.res.ColorStateList;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.lab06_20230420.databinding.FilaSesionBinding;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SesionAdapter extends RecyclerView.Adapter<SesionAdapter.Fila> {
    public interface Acciones { void editar(SesionEstudio s); void eliminar(SesionEstudio s); }
    private final List<SesionEstudio> sesiones=new ArrayList<>();
    private final Acciones acciones;
    public SesionAdapter(Acciones acciones){this.acciones=acciones;}
    public void mostrar(List<SesionEstudio> datos){sesiones.clear();sesiones.addAll(datos);notifyDataSetChanged();}
    static class Fila extends RecyclerView.ViewHolder {
        final FilaSesionBinding b;
        Fila(FilaSesionBinding binding){super(binding.getRoot());b=binding;}
    }
    @NonNull @Override public Fila onCreateViewHolder(@NonNull ViewGroup p,int type){return new Fila(FilaSesionBinding.inflate(LayoutInflater.from(p.getContext()),p,false));}
    @Override public void onBindViewHolder(@NonNull Fila f,int position){
        SesionEstudio s=sesiones.get(position);
        f.b.curso.setText(s.getCurso());
        f.b.detalle.setText(LocalDate.parse(s.getFecha()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))+" · "+EstadisticasEstudio.horas(s.getHoras())+" h");
        f.b.actividad.setText(s.getTipoActividad());
        int color=s.getTipoActividad().equals("Teoría")?0xffe3ebff:s.getTipoActividad().equals("Práctica")?0xffffead8:0xffd9f1ed;
        f.b.actividad.setBackgroundTintList(ColorStateList.valueOf(color));
        f.b.editar.setContentDescription("Editar sesión de "+s.getCurso());
        f.b.eliminar.setContentDescription("Eliminar sesión de "+s.getCurso());
        f.b.editar.setOnClickListener(v->acciones.editar(s));f.b.eliminar.setOnClickListener(v->acciones.eliminar(s));
    }
    @Override public int getItemCount(){return sesiones.size();}
}
```

## app/src/main/java/com/example/lab06_20230420/SesionesActivity.java

```java
package com.example.lab06_20230420;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.lab06_20230420.databinding.ActivitySesionesBinding;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.*;

public class SesionesActivity extends PantallaPrivada implements SesionAdapter.Acciones {
    private ActivitySesionesBinding binding;
    private SesionAdapter adapter;
    private ListenerRegistration listener;
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);if(isFinishing()||repository==null)return;
        binding=ActivitySesionesBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());ajustarBarras();
        adapter=new SesionAdapter(this);binding.lista.setLayoutManager(new LinearLayoutManager(this));binding.lista.setAdapter(adapter);
        binding.agregar.setOnClickListener(v->startActivity(new Intent(this,FormularioSesionActivity.class)));
        binding.verGraficos.setOnClickListener(v->startActivity(new Intent(this,GraficosActivity.class)));
        binding.reintentar.setOnClickListener(v->escuchar());
        binding.cerrarSesion.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Cerrar sesión")
            .setMessage("¿Deseas cerrar tu sesión?").setNegativeButton("Cancelar",null).setPositiveButton("Cerrar sesión",(d,w)->{
                detener();binding.cerrarSesion.setEnabled(false);adapter.mostrar(Collections.emptyList());
                AuthUI.getInstance().signOut(this).addOnCompleteListener(t->{
                    if(t.isSuccessful()||auth.getCurrentUser()==null)salir();
                    else{binding.cerrarSesion.setEnabled(true);aviso("No se pudo cerrar sesión. Intenta de nuevo.");escuchar();}
                });
            }).show());
    }
    @Override protected void onStart(){super.onStart();if(binding!=null&&!isFinishing())escuchar();}
    @Override protected void onStop(){detener();super.onStop();}
    private void detener(){if(listener!=null){listener.remove();listener=null;}}
    private void escuchar(){
        detener();binding.cargando.setVisibility(View.VISIBLE);binding.reintentar.setVisibility(View.GONE);
        listener=repository.escuchar((datos,cache,pendientes,error)->{
            binding.cargando.setVisibility(View.GONE);
            if(error!=null){binding.estado.setText("No se pudo cargar la lista. Comprueba conexión, sesión y reglas de Firestore.");binding.reintentar.setVisibility(View.VISIBLE);binding.vacio.setVisibility(View.GONE);return;}
            adapter.mostrar(datos);binding.contador.setText(datos.size()+ (datos.size()==1?" sesión registrada":" sesiones registradas"));
            binding.vacio.setVisibility(datos.isEmpty()?View.VISIBLE:View.GONE);
            binding.estado.setText(pendientes?"Sincronización pendiente: mantén la conexión.":cache?"Mostrando datos guardados en este dispositivo; esperando al servidor.":"Actualizado en tiempo real");
        });
    }
    @Override public void editar(SesionEstudio s){startActivity(new Intent(this,FormularioSesionActivity.class).putExtra("sesionId",s.getId()));}
    @Override public void eliminar(SesionEstudio s){
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Eliminar sesión")
            .setMessage("Se eliminará la sesión de "+s.getCurso()+" del "+s.getFecha()+".")
            .setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{
            dialog.getButton(-1).setEnabled(false);
            repository.eliminar(s.getId()).addOnCompleteListener(t->{
                if(t.isSuccessful()){dialog.dismiss();aviso("Sesión eliminada.");}
                else{dialog.getButton(-1).setEnabled(true);aviso("No se pudo eliminar. Revisa la conexión y tus permisos.");}
            });
        }));dialog.show();
    }
}
```

## app/src/main/java/com/example/lab06_20230420/SesionEstudio.java

```java
package com.example.lab06_20230420;

import com.google.firebase.firestore.Exclude;
import java.util.LinkedHashMap;
import java.util.Map;

/** Firestore persiste cuatro campos; el ID viene del documento. */
public class SesionEstudio {
    private String id, curso, fecha, tipoActividad;
    private double horas;
    public SesionEstudio() { }
    public SesionEstudio(String id, String curso, String fecha, double horas, String tipoActividad) {
        this.id=id; this.curso=curso; this.fecha=fecha; this.horas=horas; this.tipoActividad=tipoActividad;
    }
    @Exclude public String getId() { return id; }
    public void setId(String id) { this.id=id; }
    public String getCurso() { return curso; }
    public String getFecha() { return fecha; }
    public double getHoras() { return horas; }
    public String getTipoActividad() { return tipoActividad; }
    public Map<String,Object> datos() {
        Map<String,Object> d=new LinkedHashMap<>();
        d.put("curso",curso); d.put("fecha",fecha); d.put("horas",horas); d.put("tipoActividad",tipoActividad);
        return d;
    }
}
```

## app/src/main/java/com/example/lab06_20230420/SesionRepository.java

```java
package com.example.lab06_20230420;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.*;
import java.util.*;

/** Toda operación queda bajo el UID autenticado; las reglas verifican lo mismo en el servidor. */
public class SesionRepository {
    private final CollectionReference sesiones;
    public interface Cambios { void recibir(List<SesionEstudio> datos, boolean cache, boolean pendientes, Exception error); }
    public SesionRepository(String uid) {
        if(uid==null||uid.isEmpty()||uid.contains("/"))throw new IllegalArgumentException("Se requiere un UID válido.");
        sesiones=FirebaseFirestore.getInstance().collection("usuarios").document(uid).collection("sesiones");
    }
    public String nuevoId() { return sesiones.document().getId(); }
    public Task<DocumentSnapshot> buscar(String id) { return sesiones.document(id).get(); }
    public Task<Void> guardar(SesionEstudio s,boolean editar) {
        EstadisticasEstudio.validar(s);
        if(editar) return sesiones.document(s.getId()).update(s.datos());
        return sesiones.document(s.getId()).set(s.datos());
    }
    public Task<Void> eliminar(String id) { return sesiones.document(id).delete(); }
    public ListenerRegistration escuchar(Cambios cambios) {
        return sesiones.addSnapshotListener(MetadataChanges.INCLUDE,(snap,error)->{
            if(error!=null){cambios.recibir(Collections.emptyList(),false,false,error);return;}
            if(snap==null)return;
            List<SesionEstudio> r=new ArrayList<>();
            try {
                for(DocumentSnapshot d:snap.getDocuments()) {
                    SesionEstudio s=d.toObject(SesionEstudio.class);
                    if(s==null)throw new IllegalStateException("Sesión vacía");
                    s.setId(d.getId());EstadisticasEstudio.validar(s);r.add(s);
                }
                r.sort(Comparator.comparing(SesionEstudio::getFecha).reversed().thenComparing(SesionEstudio::getId));
                cambios.recibir(r,snap.getMetadata().isFromCache(),snap.getMetadata().hasPendingWrites(),null);
            }catch(Exception e){cambios.recibir(Collections.emptyList(),false,false,new IllegalStateException("Hay una sesión con campos inválidos en Firestore.",e));}
        });
    }
}
```

## app/src/main/res/drawable/fondo_etiqueta.xml

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android"><solid android:color="#E3EBFF"/><corners android:radius="9dp"/><padding android:left="10dp" android:right="10dp" android:top="4dp" android:bottom="4dp"/></shape>
```

## app/src/main/res/drawable/ic_agregar.xml

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#FFFFFF" android:pathData="M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6z"/></vector>
```

## app/src/main/res/drawable/ic_editar.xml

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#2454B7" android:pathData="M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04a0.996,0.996 0,0 0,0 -1.41l-2.34,-2.34a0.996,0.996 0,0 0,-1.41 0l-1.83,1.83 3.75,3.75 1.83,-1.83z"/></vector>
```

## app/src/main/res/drawable/ic_eliminar.xml

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#B3261E" android:pathData="M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z"/></vector>
```

## app/src/main/res/drawable/ic_estudio.xml

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="80dp" android:height="80dp" android:viewportWidth="80" android:viewportHeight="80">
    <path android:fillColor="#2454B7" android:pathData="M16,0L64,0Q80,0 80,16L80,64Q80,80 64,80L16,80Q0,80 0,64L0,16Q0,0 16,0"/>
    <path android:fillColor="#FFFFFF" android:pathData="M14,21L34,21Q40,21 40,27Q40,21 46,21L66,21L66,55L46,55Q40,55 40,61Q40,55 34,55L14,55Z"/>
    <path android:strokeColor="#2454B7" android:strokeWidth="2" android:pathData="M40,27L40,54M20,30L33,30M20,37L33,37M47,30L60,30M47,37L60,37"/>
</vector>
```

## app/src/main/res/layout/activity_formulario_sesion.xml

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/main" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:background="#F4F6FC">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="72dp" android:orientation="horizontal" android:gravity="center_vertical" android:padding="12dp" android:background="#2454B7">
        <com.google.android.material.button.MaterialButton android:id="@+id/volver" style="@style/Widget.MaterialComponents.Button.TextButton" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Volver" android:textColor="#FFFFFF" android:textAllCaps="false"/>
        <TextView android:id="@+id/titulo" style="@style/Titulo" android:text="Nueva sesión"/>
    </LinearLayout>
    <ScrollView android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:fillViewport="true">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:padding="24dp">
            <TextView style="@style/Etiqueta" android:text="Curso" android:labelFor="@id/curso"/>
            <EditText android:id="@+id/curso" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="56dp" android:hint="Ej.: Servicios y Aplicaciones para IoT" android:inputType="textCapSentences" android:maxLength="100" android:autofillHints=""/>
            <TextView style="@style/Etiqueta" android:text="Fecha de estudio" android:labelFor="@id/fecha"/>
            <com.google.android.material.button.MaterialButton android:id="@+id/fecha" style="@style/Widget.MaterialComponents.Button.OutlinedButton" android:layout_width="match_parent" android:layout_height="56dp" android:text="Seleccionar fecha" android:textAllCaps="false"/>
            <TextView style="@style/Etiqueta" android:text="Horas dedicadas" android:labelFor="@id/horas"/>
            <EditText android:id="@+id/horas" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="56dp" android:hint="Ej.: 1,5" android:inputType="numberDecimal" android:maxLength="8" android:digits="0123456789.," android:autofillHints=""/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="Mayor que 0 y hasta 24 horas por sesión." android:textSize="12sp" android:textColor="#59667A"/>
            <TextView style="@style/Etiqueta" android:text="Tipo de actividad" android:labelFor="@id/tipo"/>
            <Spinner android:id="@+id/tipo" android:layout_width="match_parent" android:layout_height="56dp"/>
            <TextView android:id="@+id/error" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="20dp" android:textColor="#B3261E" android:accessibilityLiveRegion="polite"/>
            <com.google.android.material.button.MaterialButton android:id="@+id/guardar" android:layout_width="match_parent" android:layout_height="56dp" android:layout_marginTop="16dp" android:text="Guardar sesión" android:textAllCaps="false"/>
        </LinearLayout>
    </ScrollView>
</LinearLayout>
```

## app/src/main/res/layout/activity_graficos.xml

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/main" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:background="#F4F6FC">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="72dp" android:orientation="horizontal" android:gravity="center_vertical" android:padding="12dp" android:background="#2454B7">
        <com.google.android.material.button.MaterialButton android:id="@+id/volver" style="@style/Widget.MaterialComponents.Button.TextButton" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Volver" android:textColor="#FFFFFF" android:textAllCaps="false"/>
        <TextView style="@style/Titulo" android:text="Mis gráficos"/>
    </LinearLayout>
    <ScrollView android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:fillViewport="true">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:padding="20dp">
            <TextView style="@style/Etiqueta" android:text="Filtrar por curso" android:labelFor="@id/filtroCurso"/>
            <Spinner android:id="@+id/filtroCurso" android:layout_width="match_parent" android:layout_height="56dp"/>
            <TextView android:id="@+id/resumen" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="16dp" android:textSize="22sp" android:textStyle="bold" android:textColor="#172944"/>
            <TextView android:id="@+id/estado" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="8dp" android:textColor="#59667A" android:accessibilityLiveRegion="polite"/>
            <com.google.android.material.button.MaterialButton android:id="@+id/reintentar" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Reintentar" android:visibility="gone"/>
            <TextView style="@style/Etiqueta" android:text="Horas de estudio por mes"/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="Cada barra suma las horas del mes y año indicados." android:textColor="#59667A"/>
            <FrameLayout android:id="@+id/barras" android:layout_width="match_parent" android:layout_height="wrap_content" android:background="#FFFFFF" android:layout_marginTop="10dp"/>
            <TextView style="@style/Etiqueta" android:text="Horas por tipo de actividad"/>
            <FrameLayout android:id="@+id/torta" android:layout_width="match_parent" android:layout_height="wrap_content" android:background="#FFFFFF" android:layout_marginTop="10dp"/>
        </LinearLayout>
    </ScrollView>
</LinearLayout>
```

## app/src/main/res/layout/activity_login.xml

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/main" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:gravity="center" android:padding="28dp" android:background="#F4F6FC">
    <ImageView android:layout_width="88dp" android:layout_height="88dp" android:src="@drawable/ic_estudio" android:contentDescription="Logo: libro abierto"/>
    <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="24dp" android:gravity="center" android:text="Mis horas de estudio" android:textSize="28sp" android:textStyle="bold" android:textColor="#172944"/>
    <TextView android:id="@+id/estadoLogin" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="16dp" android:gravity="center" android:text="Organiza tus sesiones y conoce cómo distribuyes tu tiempo." android:textColor="#59667A" android:textSize="16sp"/>
    <com.google.android.material.button.MaterialButton android:id="@+id/abrirAcceso" android:layout_width="match_parent" android:layout_height="56dp" android:layout_marginTop="28dp" android:text="Abrir inicio de sesión" android:textAllCaps="false"/>
</LinearLayout>
```

## app/src/main/res/layout/activity_sesiones.xml

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" android:id="@+id/main" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:background="#F4F6FC">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="72dp" android:orientation="horizontal" android:gravity="center_vertical" android:padding="16dp" android:background="#2454B7">
        <TextView style="@style/Titulo" android:text="Mis sesiones de estudio"/>
        <com.google.android.material.button.MaterialButton android:id="@+id/cerrarSesion" style="@style/Widget.MaterialComponents.Button.TextButton" android:layout_width="100dp" android:layout_height="wrap_content" android:text="Cerrar sesión" android:textColor="#FFFFFF" android:textAllCaps="false"/>
    </LinearLayout>
    <FrameLayout android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1">
        <LinearLayout android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:paddingHorizontal="12dp">
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical" android:orientation="horizontal" android:paddingVertical="12dp">
                <TextView android:id="@+id/contador" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:text="Mis sesiones" android:textColor="#172944"/>
                <com.google.android.material.button.MaterialButton android:id="@+id/verGraficos" style="@style/Widget.MaterialComponents.Button.OutlinedButton" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Ver gráficos" android:textAllCaps="false" app:cornerRadius="24dp"/>
            </LinearLayout>
            <TextView android:id="@+id/estado" android:layout_width="match_parent" android:layout_height="wrap_content" android:paddingBottom="8dp" android:textColor="#59667A" android:textSize="12sp" android:accessibilityLiveRegion="polite"/>
            <ProgressBar android:id="@+id/cargando" style="?android:attr/progressBarStyleHorizontal" android:layout_width="match_parent" android:layout_height="4dp" android:indeterminate="true"/>
            <com.google.android.material.button.MaterialButton android:id="@+id/reintentar" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Reintentar" android:visibility="gone"/>
            <TextView android:id="@+id/vacio" android:layout_width="match_parent" android:layout_height="wrap_content" android:padding="24dp" android:gravity="center" android:text="Todavía no tienes sesiones.\nPulsa + para registrar la primera." android:textColor="#59667A" android:visibility="gone"/>
            <androidx.recyclerview.widget.RecyclerView android:id="@+id/lista" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:clipToPadding="false" android:paddingBottom="88dp"/>
        </LinearLayout>
        <com.google.android.material.floatingactionbutton.FloatingActionButton android:id="@+id/agregar" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_gravity="bottom|end" android:layout_margin="20dp" android:contentDescription="Agregar sesión de estudio" app:srcCompat="@drawable/ic_agregar" app:backgroundTint="#2454B7" app:tint="#FFFFFF"/>
    </FrameLayout>
</LinearLayout>
```

## app/src/main/res/layout/fila_sesion.xml

```xml
<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginBottom="12dp" app:cardCornerRadius="16dp" app:cardElevation="0dp" app:strokeWidth="1dp" app:strokeColor="#DCE3F1" app:cardBackgroundColor="#FFFFFF">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:gravity="center_vertical" android:padding="16dp">
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
            <TextView android:id="@+id/curso" android:layout_width="match_parent" android:layout_height="wrap_content" android:textSize="18sp" android:textStyle="bold" android:textColor="#172944"/>
            <TextView android:id="@+id/detalle" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="6dp" android:textSize="15sp" android:textColor="#2454B7"/>
            <TextView android:id="@+id/actividad" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="8dp" android:textSize="13sp" android:textColor="#172944" android:background="@drawable/fondo_etiqueta"/>
        </LinearLayout>
        <ImageButton android:id="@+id/editar" android:layout_width="48dp" android:layout_height="48dp" android:src="@drawable/ic_editar" android:background="?attr/selectableItemBackgroundBorderless" android:contentDescription="Editar sesión"/>
        <ImageButton android:id="@+id/eliminar" android:layout_width="48dp" android:layout_height="48dp" android:src="@drawable/ic_eliminar" android:background="?attr/selectableItemBackgroundBorderless" android:contentDescription="Eliminar sesión"/>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

## app/src/main/res/layout/firebase_login.xml

```xml
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android" android:layout_width="match_parent" android:layout_height="match_parent" android:fillViewport="true" android:background="#F4F6FC">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:gravity="center" android:padding="28dp">
        <ImageView android:layout_width="88dp" android:layout_height="88dp" android:src="@drawable/ic_estudio" android:contentDescription="Logo de Mis horas de estudio"/>
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="24dp" android:gravity="center" android:text="Mis horas de estudio" android:textSize="28sp" android:textStyle="bold" android:textColor="#172944"/>
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="12dp" android:gravity="center" android:text="Tu tiempo también cuenta.\nRegistra lo que aprendes cada día." android:textColor="#59667A" android:textSize="17sp"/>
        <com.google.android.material.button.MaterialButton android:id="@+id/accesoCorreo" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="56dp" android:layout_marginTop="32dp" android:text="@string/acceso_correo" android:textAllCaps="false"/>
        <com.google.android.material.button.MaterialButton android:id="@+id/accesoGoogle" style="@style/Widget.MaterialComponents.Button.OutlinedButton" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="56dp" android:layout_marginTop="8dp" android:text="@string/acceso_google" android:textAllCaps="false"/>
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="24dp" android:gravity="center" android:text="@string/ayuda_acceso" android:textColor="#59667A" android:textSize="14sp"/>
    </LinearLayout>
</ScrollView>
```

## app/src/main/res/values/strings.xml

```xml
<resources>
    <string name="app_name">Mis horas de estudio</string>
    <string name="acceso_correo">Ingresar o registrarme con correo</string>
    <string name="acceso_google">Continuar con Google</string>
    <string name="ayuda_acceso">¿Olvidaste tu contraseña? Selecciona correo para recuperarla.\n¿Eres nuevo? Desde ese mismo acceso puedes crear tu cuenta.</string>
</resources>
```

## app/src/main/res/values/styles.xml

```xml
<resources>
    <style name="Theme.Estudio" parent="Theme.MaterialComponents.Light.NoActionBar">
        <item name="colorPrimary">#2454B7</item><item name="colorPrimaryVariant">#193E8D</item><item name="colorSecondary">#16978D</item><item name="colorAccent">#2454B7</item>
        <item name="android:fontFamily">sans</item><item name="android:windowLightStatusBar">false</item><item name="android:statusBarColor">#193E8D</item>
        <item name="android:windowBackground">#F4F6FC</item>
    </style>
    <style name="Theme.Estudio.Auth" parent="Theme.Estudio">
        <item name="android:windowBackground">#F4F6FC</item>
    </style>
    <style name="Titulo"><item name="android:layout_width">0dp</item><item name="android:layout_height">wrap_content</item><item name="android:layout_weight">1</item><item name="android:textSize">22sp</item><item name="android:textStyle">bold</item><item name="android:textColor">#FFFFFF</item></style>
    <style name="Etiqueta"><item name="android:layout_width">match_parent</item><item name="android:layout_height">wrap_content</item><item name="android:textSize">16sp</item><item name="android:textStyle">bold</item><item name="android:textColor">#172944</item><item name="android:layout_marginTop">18dp</item><item name="android:layout_marginBottom">6dp</item></style>
</resources>
```

## app/src/test/java/com/example/lab06_20230420/EstadisticasEstudioTest.java

```java
package com.example.lab06_20230420;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class EstadisticasEstudioTest {
    private SesionEstudio s(String curso,String fecha,double horas,String actividad){return new SesionEstudio("id",curso,fecha,horas,actividad);}
    @Test public void sumaHorasDecimalesPorMesYNoNumeroDeSesiones(){
        List<SesionEstudio> lista=Arrays.asList(s("IoT","2026-10-01",1.5,"Teoría"),s("IoT","2026-10-20",2,"Práctica"),s("IoT","2026-11-01",0.75,"Laboratorio"));
        assertEquals(3.5,EstadisticasEstudio.porMes(lista).get("2026-10"),1e-9);
        assertEquals(0.75,EstadisticasEstudio.porMes(lista).get("2026-11"),1e-9);
    }
    @Test public void noMezclaElMismoMesDeDiferentesAnios(){
        Map<String,Double> r=EstadisticasEstudio.porMes(Arrays.asList(s("IoT","2027-01-01",2,"Teoría"),s("IoT","2026-01-01",1,"Teoría")));
        assertEquals(Arrays.asList("2026-01","2027-01"),new ArrayList<>(r.keySet()));
    }
    @Test public void ambosGraficosUsanElCursoFiltrado(){
        List<SesionEstudio> lista=Arrays.asList(s("IoT","2026-11-01",2.5,"Teoría"),s("Redes","2026-11-01",6,"Práctica"),s("IoT","2026-11-03",1,"Laboratorio"));
        List<SesionEstudio> filtro=EstadisticasEstudio.filtrar(lista," iot ");
        assertEquals(3.5,EstadisticasEstudio.total(filtro),1e-9);
        assertEquals(3.5,EstadisticasEstudio.porMes(filtro).get("2026-11"),1e-9);
        assertEquals(0,EstadisticasEstudio.porActividad(filtro).get("Práctica"),1e-9);
        assertEquals(2.5,EstadisticasEstudio.porActividad(filtro).get("Teoría"),1e-9);
    }
    @Test public void cursosSinDuplicadosDeMayusculas(){
        assertEquals(2,EstadisticasEstudio.cursos(Arrays.asList(s("IoT","2026-11-01",1,"Teoría"),s("iot","2026-11-01",1,"Teoría"),s("Redes","2026-11-01",1,"Teoría"))).size());
    }
    @Test public void aceptaComaYPuntoEnHoras(){assertEquals(1.5,EstadisticasEstudio.leerHoras("1,5"),1e-9);assertEquals(1.5,EstadisticasEstudio.leerHoras("1.5"),1e-9);}
    @Test public void rechazaHorasInvalidas(){
        for(String value:new String[]{"0","-2","25","NaN","Infinity","1,2.3","","1e2"}){
            try{EstadisticasEstudio.leerHoras(value);fail(value);}catch(IllegalArgumentException expected){}
        }
    }
    @Test public void validaFechaRealYActividad(){
        try{EstadisticasEstudio.validar(s("IoT","2026-02-30",1,"Teoría"));fail();}catch(IllegalArgumentException expected){}
        try{EstadisticasEstudio.validar(s("IoT","2026-02-28",1,"Otra"));fail();}catch(IllegalArgumentException expected){}
        EstadisticasEstudio.validar(s("IoT","2026-02-28",1,"Teoría"));
    }
    @Test public void listaVaciaYFiltroSinResultadosSonCero(){assertTrue(EstadisticasEstudio.porMes(Collections.emptyList()).isEmpty());assertEquals(0,EstadisticasEstudio.total(Collections.emptyList()),0);assertEquals(3,EstadisticasEstudio.porActividad(Collections.emptyList()).size());}
}
```

## app/src/androidTest/java/com/example/lab06_20230420/AppFlowTest.java

```java
package com.example.lab06_20230420;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AppFlowTest {
    private <T>T esperar(Task<T> t)throws Exception{return Tasks.await(t,30,TimeUnit.SECONDS);}
    private void visible(org.hamcrest.Matcher<View> matcher){
        AssertionError last=null;
        for(int i=0;i<80;i++){try{onView(matcher).check(matches(isDisplayed()));return;}catch(androidx.test.espresso.NoMatchingViewException|AssertionError e){last=new AssertionError(e);SystemClock.sleep(200);}}
        throw last;
    }
    private void capturar(String nombre)throws Exception{
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Collection<android.app.Activity> activities=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED);
            for(android.app.Activity a:activities){View v=a.getWindow().getDecorView().getRootView();
                if(v.getWidth()==0||v.getHeight()==0)continue;
                Bitmap bitmap=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new android.graphics.Canvas(bitmap));
                File dir=new File(a.getExternalFilesDir(null),"capturas");dir.mkdirs();
                try(FileOutputStream out=new FileOutputStream(new File(dir,nombre+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}catch(IOException e){throw new RuntimeException(e);}bitmap.recycle();break;
            }
        });
    }
    @Test public void interfazCrudGraficosYRestauracion()throws Exception{
        FirebaseApp app=FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext());assertEquals("lab6-20230420",app.getOptions().getProjectId());
        FirebaseAuth auth=FirebaseAuth.getInstance();auth.signOut();
        String correo="pantalla-"+UUID.randomUUID()+"@example.com",clave="Lab6!"+UUID.randomUUID(),uid=null;
        try{
            uid=esperar(auth.createUserWithEmailAndPassword(correo,clave)).getUser().getUid();
            try(ActivityScenario<SesionesActivity> scenario=ActivityScenario.launch(SesionesActivity.class)){
                visible(withId(R.id.agregar));onView(withId(R.id.agregar)).perform(click());
                visible(withId(R.id.curso));onView(withId(R.id.curso)).perform(replaceText("Servicios y Aplicaciones para IoT"),closeSoftKeyboard());
                onView(withId(R.id.horas)).perform(scrollTo(),replaceText("1,5"),closeSoftKeyboard());
                onView(withId(R.id.tipo)).perform(scrollTo(),click());onData(is("Laboratorio")).perform(click());
                capturar("formulario");onView(withId(R.id.guardar)).perform(scrollTo(),click());
                visible(withText("Servicios y Aplicaciones para IoT"));visible(withText("1 sesión registrada"));capturar("lista");
                onView(withId(R.id.editar)).perform(click());visible(withId(R.id.guardar));
                onView(withId(R.id.curso)).perform(replaceText("Redes de Computadoras"),closeSoftKeyboard());
                onView(withId(R.id.horas)).perform(scrollTo(),replaceText("2.25"),closeSoftKeyboard());
                onView(withId(R.id.guardar)).perform(scrollTo(),click());visible(withId(R.id.verGraficos));visible(withText("Redes de Computadoras"));
                onView(withId(R.id.verGraficos)).perform(click());visible(withId(R.id.filtroCurso));visible(withText(containsString("horas · 1 sesión")));capturar("graficos");
                onView(withId(R.id.filtroCurso)).perform(click());onData(is("Redes de Computadoras")).perform(click());
                onView(withId(R.id.resumen)).check(matches(withText(containsString("horas"))));
                onView(withId(R.id.volver)).perform(click());visible(withId(R.id.editar));
                onView(withId(R.id.eliminar)).perform(click());onView(withText("Eliminar")).perform(click());visible(withText("0 sesiones registradas"));
                scenario.recreate();visible(withId(R.id.agregar));onView(withId(R.id.cerrarSesion)).check(matches(isDisplayed()));
            }
        }finally{
            if(uid!=null){esperar(auth.signInWithEmailAndPassword(correo,clave));
                FirebaseFirestore db=FirebaseFirestore.getInstance();for(DocumentSnapshot d:esperar(db.collection("usuarios").document(uid).collection("sesiones").get()).getDocuments())esperar(d.getReference().delete());
                esperar(auth.getCurrentUser().delete());}auth.signOut();
        }
    }
    @Test public void firebaseUiRegistroLoginRecuperacionYCierreEnEspanol()throws Exception{
        FirebaseAuth auth=FirebaseAuth.getInstance();auth.signOut();
        String correo="acceso-"+UUID.randomUUID()+"@example.com",clave="Lab6!"+UUID.randomUUID();
        try(ActivityScenario<LoginActivity> scenario=ActivityScenario.launch(LoginActivity.class)){
            visible(withId(R.id.accesoCorreo));onView(withId(R.id.accesoGoogle)).check(matches(isDisplayed()));capturar("login");
            onView(withId(R.id.accesoCorreo)).perform(click());visible(withId(com.firebase.ui.auth.R.id.email));
            onView(withId(com.firebase.ui.auth.R.id.email)).perform(replaceText(correo),closeSoftKeyboard());
            onView(withId(com.firebase.ui.auth.R.id.button_sign_up)).perform(click());visible(withId(com.firebase.ui.auth.R.id.password));
            onView(withId(com.firebase.ui.auth.R.id.password)).perform(replaceText(clave),closeSoftKeyboard());
            onView(withId(com.firebase.ui.auth.R.id.button_create)).perform(click());visible(withId(R.id.agregar));
            assertEquals(correo,auth.getCurrentUser().getEmail());
            onView(withId(R.id.cerrarSesion)).perform(click());onView(withId(android.R.id.button1)).perform(click());
            visible(withId(R.id.accesoCorreo));assertNull(auth.getCurrentUser());
            onView(withId(R.id.accesoCorreo)).perform(click());visible(withId(com.firebase.ui.auth.R.id.email));
            onView(withId(com.firebase.ui.auth.R.id.email)).perform(replaceText(correo),closeSoftKeyboard());
            onView(withId(com.firebase.ui.auth.R.id.button_sign_in)).perform(click());visible(withId(com.firebase.ui.auth.R.id.password));
            onView(withId(com.firebase.ui.auth.R.id.trouble_signing_in)).perform(click());visible(withId(com.firebase.ui.auth.R.id.email));
            onView(withId(com.firebase.ui.auth.R.id.button_done)).check(matches(withText("Enviar")));pressBack();
            visible(withId(com.firebase.ui.auth.R.id.password));onView(withId(com.firebase.ui.auth.R.id.password)).perform(replaceText(clave),closeSoftKeyboard());
            onView(withId(com.firebase.ui.auth.R.id.button_done)).perform(click());visible(withId(R.id.agregar));assertEquals(correo,auth.getCurrentUser().getEmail());
        }finally{
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
                for(androidx.test.runner.lifecycle.Stage stage:new androidx.test.runner.lifecycle.Stage[]{androidx.test.runner.lifecycle.Stage.RESUMED,androidx.test.runner.lifecycle.Stage.STARTED,androidx.test.runner.lifecycle.Stage.STOPPED})
                    for(android.app.Activity a:new ArrayList<>(androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage)))a.finish();
            });
            try{esperar(auth.signInWithEmailAndPassword(correo,clave));esperar(auth.getCurrentUser().delete());}catch(Exception ignored){}auth.signOut();
        }
    }
}
```

## app/src/androidTest/java/com/example/lab06_20230420/FirebaseIntegrationTest.java

```java
package com.example.lab06_20230420;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Solo lab6-20230420: crea y limpia cuentas y documentos de prueba con UUID propio. */
@RunWith(AndroidJUnit4.class)
public class FirebaseIntegrationTest {
    private <T>T esperar(Task<T> t)throws Exception{return Tasks.await(t,30,TimeUnit.SECONDS);}
    private void denegada(Task<?> t)throws Exception{
        try{esperar(t);fail("El servidor debe rechazar esta operación");}
        catch(ExecutionException e){assertTrue(e.getCause() instanceof FirebaseFirestoreException);assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED,((FirebaseFirestoreException)e.getCause()).getCode());}
    }
    @Test public void crudTiempoRealYReglasAislanDosUsuarios()throws Exception{
        FirebaseApp app=FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext());
        assertNotNull(app);assertEquals("lab6-20230420",app.getOptions().getProjectId());
        FirebaseAuth auth=FirebaseAuth.getInstance();FirebaseFirestore db=FirebaseFirestore.getInstance();auth.signOut();
        String run=UUID.randomUUID().toString().replace("-","");String clave="Lab6!"+UUID.randomUUID();
        String a="sesion-"+run+"-a@example.com",b="sesion-"+run+"-b@example.com",uid=null;boolean creadoA=false,creadoB=false;
        ListenerRegistration listener=null;
        try{
            uid=esperar(auth.createUserWithEmailAndPassword(a,clave)).getUser().getUid();creadoA=true;
            SesionRepository repo=new SesionRepository(uid);String id=repo.nuevoId();
            DocumentReference ref=db.collection("usuarios").document(uid).collection("sesiones").document(id);
            CountDownLatch recibido=new CountDownLatch(1);
            listener=repo.escuchar((datos,cache,pendientes,error)->{if(error==null&&!cache&&!pendientes&&!datos.isEmpty())recibido.countDown();});
            SesionEstudio sesion=new SesionEstudio(id,"IoT","2026-11-17",1.5,"Laboratorio");esperar(repo.guardar(sesion,false));
            assertTrue("El listener debe recibir el alta del servidor",recibido.await(20,TimeUnit.SECONDS));
            assertEquals(1.5,esperar(ref.get(Source.SERVER)).getDouble("horas"),0);
            esperar(repo.guardar(new SesionEstudio(id,"Redes","2026-10-28",2.25,"Práctica"),true));
            assertEquals("Redes",esperar(ref.get(Source.SERVER)).getString("curso"));
            Map<String,Object> invalido=new HashMap<>(sesion.datos());invalido.put("horas",-1);denegada(ref.update(invalido));
            invalido=new HashMap<>(sesion.datos());invalido.put("admin",true);denegada(ref.set(invalido));
            listener.remove();listener=null;auth.signOut();
            esperar(auth.createUserWithEmailAndPassword(b,clave));creadoB=true;
            denegada(ref.get(Source.SERVER));denegada(ref.update("horas",3));
            esperar(auth.getCurrentUser().delete());creadoB=false;
            esperar(auth.signInWithEmailAndPassword(a,clave));esperar(repo.eliminar(id));assertFalse(esperar(ref.get(Source.SERVER)).exists());
        }finally{
            if(listener!=null)listener.remove();
            if(creadoB){esperar(auth.signInWithEmailAndPassword(b,clave));esperar(auth.getCurrentUser().delete());}
            if(creadoA){esperar(auth.signInWithEmailAndPassword(a,clave));
                for(DocumentSnapshot d:esperar(db.collection("usuarios").document(uid).collection("sesiones").get()).getDocuments())esperar(d.getReference().delete());
                esperar(auth.getCurrentUser().delete());}
            auth.signOut();
        }
    }
}
```

## build.gradle

```groovy
plugins {
    id 'com.android.application' version '9.0.0' apply false
    id 'com.google.gms.google-services' version '4.4.4' apply false
}
```

## app/build.gradle

```groovy
plugins { id 'com.android.application' }
if (file('google-services.json').exists()) { apply plugin: 'com.google.gms.google-services' }
android {
    namespace 'com.example.lab06_20230420'
    compileSdk 36
    defaultConfig {
        applicationId 'com.example.lab06_20230420'
        minSdk 34
        targetSdk 36
        versionCode 1
        versionName '1.0'
        testInstrumentationRunner 'androidx.test.runner.AndroidJUnitRunner'
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }
    buildFeatures { viewBinding true }
}
dependencies {
    implementation 'androidx.appcompat:appcompat:1.7.1'
    implementation 'com.google.android.material:material:1.13.0'
    implementation 'androidx.activity:activity:1.12.2'
    implementation 'androidx.recyclerview:recyclerview:1.4.0'
    implementation platform('com.google.firebase:firebase-bom:34.13.0')
    implementation 'com.google.firebase:firebase-auth'
    implementation 'com.google.firebase:firebase-firestore'
    implementation 'com.firebaseui:firebase-ui-auth:9.1.1'
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.3.0'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.7.0'
}
```

## settings.gradle

```groovy
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = 'LAB6_20230420'
include ':app'
```

## gradle.properties

```text
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
```

## gradle/wrapper/gradle-wrapper.properties

```text
#Mon Jan 19 22:14:28 PET 2026
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionSha256Sum=a17ddd85a26b6a7f5ddb71ff8b05fc5104c0202c6e64782429790c933686c806
distributionUrl=https\://services.gradle.org/distributions/gradle-9.1.0-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

## .gitignore

```text
/.idea/
/.gradle/
**/build/
/local.properties
/app/google-services.json
*.jks
*.keystore
/firebase-debug.log
/firestore-debug.log
```

## firestore.rules

```text
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /usuarios/{uid}/sesiones/{sesionId} {
      function propietario() { return request.auth != null && request.auth.uid == uid; }
      function valida() {
        let d = request.resource.data;
        return d.keys().hasAll(['curso','fecha','horas','tipoActividad'])
          && d.keys().hasOnly(['curso','fecha','horas','tipoActividad'])
          && d.curso is string && d.curso.size() > 0 && d.curso.size() <= 100
          && d.fecha is string && d.fecha.matches('[0-9]{4}-[0-9]{2}-[0-9]{2}')
          && (d.horas is int || d.horas is float) && d.horas > 0 && d.horas <= 24
          && d.tipoActividad in ['Teoría','Práctica','Laboratorio'];
      }
      allow read, delete: if propietario();
      allow create, update: if propietario() && valida();
    }
  }
}
```

## firebase.json

```text
{"firestore":{"rules":"firestore.rules"}}
```

## .firebaserc

```text
{"projects":{"default":"lab6-20230420"}}
```
