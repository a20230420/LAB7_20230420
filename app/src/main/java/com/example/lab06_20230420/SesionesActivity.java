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
        findViewById(R.id.mc_abrir).setOnClickListener(v ->
                startActivity(new Intent(this, MicroserviciosActivity.class)));
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
