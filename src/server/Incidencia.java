package appservidor;

/**
 *
 * @author mivap
 */
public class Incidencia {
    private int id;
    private String tipo;
    private String estado;
    private String ubicacion;

    public Incidencia(){}
    
    public Incidencia(int id, String tipo, String estado, String ubicacion) {
        this.id = id;
        this.tipo = tipo;
        this.estado = estado;
        this.ubicacion = ubicacion;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getEstado() {
        return estado;
    }
    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getUbicacion() {
        return ubicacion;
    }
    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }
}
