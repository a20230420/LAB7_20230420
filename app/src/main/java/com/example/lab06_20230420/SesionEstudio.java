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
