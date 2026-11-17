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
