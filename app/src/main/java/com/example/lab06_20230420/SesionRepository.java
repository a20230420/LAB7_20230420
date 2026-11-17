package com.example.lab06_20230420;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.*;
import java.util.*;

/** Toda operación queda bajo el UID autenticado; las reglas verifican lo mismo en el servidor. */
public class SesionRepository {
    private final CollectionReference sesiones;
    public interface Cambios { void recibir(List<SesionEstudio> datos, boolean cache, boolean pendientes, Exception error); }
    public SesionRepository(String uid) {
        if(uid==null||uid.isEmpty()||uid.contains("/"))throw new IllegalArgumentException("Se requiere un UID válido.");
        sesiones=FirebaseFirestore.getInstance().collection("usuarios").document(uid).collection("sesiones");
    }
    public String nuevoId() { return sesiones.document().getId(); }
    public Task<DocumentSnapshot> buscar(String id) { return sesiones.document(id).get(); }
    public Task<Void> guardar(SesionEstudio s,boolean editar) {
        EstadisticasEstudio.validar(s);
        if(editar) return sesiones.document(s.getId()).update(s.datos());
        return sesiones.document(s.getId()).set(s.datos());
    }
    public Task<Void> eliminar(String id) { return sesiones.document(id).delete(); }
    public ListenerRegistration escuchar(Cambios cambios) {
        return sesiones.addSnapshotListener(MetadataChanges.INCLUDE,(snap,error)->{
            if(error!=null){cambios.recibir(Collections.emptyList(),false,false,error);return;}
            if(snap==null)return;
            List<SesionEstudio> r=new ArrayList<>();
            try {
                for(DocumentSnapshot d:snap.getDocuments()) {
                    SesionEstudio s=d.toObject(SesionEstudio.class);
                    if(s==null)throw new IllegalStateException("Sesión vacía");
                    s.setId(d.getId());EstadisticasEstudio.validar(s);r.add(s);
                }
                r.sort(Comparator.comparing(SesionEstudio::getFecha).reversed().thenComparing(SesionEstudio::getId));
                cambios.recibir(r,snap.getMetadata().isFromCache(),snap.getMetadata().hasPendingWrites(),null);
            }catch(Exception e){cambios.recibir(Collections.emptyList(),false,false,new IllegalStateException("Hay una sesión con campos inválidos en Firestore.",e));}
        });
    }
}
