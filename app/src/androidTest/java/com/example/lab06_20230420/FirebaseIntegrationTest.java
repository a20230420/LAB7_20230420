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
