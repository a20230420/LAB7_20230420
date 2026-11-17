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
