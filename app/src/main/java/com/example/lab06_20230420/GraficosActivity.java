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
