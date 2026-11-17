package com.example.lab06_20230420;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public final class EstadisticasEstudio {
    public static final String[] ACTIVIDADES={"Teoría","Práctica","Laboratorio"};
    private EstadisticasEstudio() { }
    public static String cursoLimpio(String curso) { return curso==null?"":curso.trim().replaceAll("\\s+"," "); }
    public static double leerHoras(String texto) {
        String value=texto.trim().replace(',','.');
        if(!value.matches("[0-9]+(?:\\.[0-9]+)?")) throw new IllegalArgumentException("Ingresa horas numéricas; puedes usar coma o punto decimal.");
        double horas=Double.parseDouble(value);
        if(!Double.isFinite(horas)||horas<=0||horas>24) throw new IllegalArgumentException("Las horas deben ser mayores que 0 y como máximo 24 por sesión.");
        return horas;
    }
    public static void validar(SesionEstudio s) {
        if(cursoLimpio(s.getCurso()).isEmpty()||s.getCurso().length()>100) throw new IllegalArgumentException("Ingresa un curso de hasta 100 caracteres.");
        try { if(!s.getFecha().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))throw new IllegalArgumentException();LocalDate.parse(s.getFecha()); } catch(Exception e) { throw new IllegalArgumentException("Selecciona una fecha válida."); }
        if(!Double.isFinite(s.getHoras())||s.getHoras()<=0||s.getHoras()>24) throw new IllegalArgumentException("Las horas deben estar entre más de 0 y 24.");
        if(!Arrays.asList(ACTIVIDADES).contains(s.getTipoActividad())) throw new IllegalArgumentException("Selecciona un tipo de actividad válido.");
    }
    public static List<SesionEstudio> filtrar(List<SesionEstudio> todas,String curso) {
        List<SesionEstudio> r=new ArrayList<>();
        for(SesionEstudio s:todas) if(curso==null||cursoLimpio(s.getCurso()).equalsIgnoreCase(cursoLimpio(curso))) r.add(s);
        return r;
    }
    public static List<String> cursos(List<SesionEstudio> sesiones) {
        Set<String> cursos=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for(SesionEstudio s:sesiones) cursos.add(cursoLimpio(s.getCurso()));
        return new ArrayList<>(cursos);
    }
    public static Map<String,Double> porMes(List<SesionEstudio> sesiones) {
        Map<String,Double> r=new TreeMap<>();
        for(SesionEstudio s:sesiones) r.merge(YearMonth.from(LocalDate.parse(s.getFecha())).toString(),s.getHoras(),Double::sum);
        return r;
    }
    public static Map<String,Double> porActividad(List<SesionEstudio> sesiones) {
        Map<String,Double> r=new LinkedHashMap<>();
        for(String actividad:ACTIVIDADES) r.put(actividad,0d);
        for(SesionEstudio s:sesiones) r.merge(s.getTipoActividad(),s.getHoras(),Double::sum);
        return r;
    }
    public static double total(List<SesionEstudio> sesiones) { double t=0; for(SesionEstudio s:sesiones)t+=s.getHoras();return t; }
    public static String horas(double value) { return String.format(new Locale("es","PE"),"%.2f",value).replaceAll("0+$","").replaceAll("[.,]$",""); }
}
