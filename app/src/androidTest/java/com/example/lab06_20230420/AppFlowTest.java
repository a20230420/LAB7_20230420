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
