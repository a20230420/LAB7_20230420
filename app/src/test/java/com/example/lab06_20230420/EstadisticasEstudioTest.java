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
