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
