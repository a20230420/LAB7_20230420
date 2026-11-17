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
