package com.example.lab06_20230420;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Representa SUMAS DE HORAS: nunca cuenta documentos para calcular estos gráficos. */
public class GraficoHorasView extends View {
    private final Map<String,Double> valores;
    private final boolean circular;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] colores={0xff2454b7,0xffe79a35,0xff16978d};
    public GraficoHorasView(Context context,Map<String,Double> valores,boolean circular){
        super(context);this.valores=new LinkedHashMap<>(valores);this.circular=circular;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        StringBuilder descripcion=new StringBuilder(circular?"Horas por actividad. ":"Horas por mes. ");
        for(Map.Entry<String,Double> e:valores.entrySet())descripcion.append(etiqueta(e.getKey())).append(": ").append(EstadisticasEstudio.horas(e.getValue())).append(" horas. ");
        setContentDescription(descripcion.toString());
    }
    public int alturaDp(){return circular?340:Math.max(90,valores.size()*56+16);}
    private String etiqueta(String key){return circular?key:YearMonth.parse(key).format(DateTimeFormatter.ofPattern("MMM yyyy",new Locale("es","PE")));}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float d=getResources().getDisplayMetrics().density;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.NORMAL));paint.setTextSize(13*d);
        double total=0,max=0;for(double n:valores.values()){total+=n;max=Math.max(max,n);}
        if(total<=0){paint.setColor(0xff59667a);canvas.drawText("Sin horas para este curso",12*d,36*d,paint);return;}
        if(circular){
            float size=Math.min(getWidth()-32*d,200*d),left=(getWidth()-size)/2;
            RectF box=new RectF(left,12*d,left+size,12*d+size);float angulo=-90;int i=0;
            for(Map.Entry<String,Double> e:valores.entrySet()){
                paint.setColor(colores[i%colores.length]);float arco=(float)(360*e.getValue()/total);
                if(arco>0)canvas.drawArc(box,angulo,arco,true,paint);angulo+=arco;
                float y=size+44*d+i*28*d;canvas.drawRoundRect(12*d,y-12*d,24*d,y,3*d,3*d,paint);
                paint.setColor(0xff172944);canvas.drawText(e.getKey()+": "+EstadisticasEstudio.horas(e.getValue())+" h ("+Math.round(100*e.getValue()/total)+"%)",32*d,y,paint);i++;
            }
        }else{
            int i=0;float ancho=Math.max(0,getWidth()-24*d);
            for(Map.Entry<String,Double> e:valores.entrySet()){
                float y=12*d+i*56*d;paint.setColor(0xff172944);canvas.drawText(etiqueta(e.getKey()),12*d,y+13*d,paint);
                String horas=EstadisticasEstudio.horas(e.getValue())+" h";
                canvas.drawText(horas,getWidth()-12*d-paint.measureText(horas),y+13*d,paint);
                paint.setColor(0xffe7edf8);canvas.drawRoundRect(12*d,y+23*d,12*d+ancho,y+37*d,5*d,5*d,paint);
                paint.setColor(colores[0]);canvas.drawRoundRect(12*d,y+23*d,12*d+(float)(ancho*e.getValue()/max),y+37*d,5*d,5*d,paint);i++;
            }
        }
    }
}
