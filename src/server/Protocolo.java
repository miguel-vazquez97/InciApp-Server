package server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author mivap
 */
public class Protocolo {
    private static final String[] codigosProtocolo={"",
                                       "1||registrarUsuarioOk||",
                                       "2||registrarUsuarioDenegado||",
                                       "3||logAdminOk||",
                                       "4||logAdminDenegado||",
                                       "5||sesionIniciadaOtroDispositivo||",
                                       "6||logOutAdminOk||",
                                       "7||listadoIncidenciasOk||",
                                       "8||detallesIncidenciaOk||",
                                       "9||supervisorAsignadoOk||",
                                       "10||supervisorAsignadoDenegado||",
                                       "11||empleadoAsignadoOk||",
                                       "12||empleadoAsignadoDenegado||"};
  
    
    private int state = INICIO;
    private static final int INICIO = 0;
    private static final int REGISTRAR_USUARIO = 1;    
    private static final int LOG_ADMIN = 2;
    private static final int LOG_OUT = 3;
    private static final int INCIDENCIAS_TABLA = 4;
    private static final int DATOS_INCIDENCIA_NUEVA_REGISTRADA = 5;
    private static final int ASIGNAR_INCIDENCIA_SUPERVISOR = 6;
    private static final int LISTADO_EMPLEADOS = 7;
    private static final int ASIGNAR_INCIDENCIA_EMPLEADO = 8;
    
    static boolean transicionNula = false;
    
    String correoUsuario;
    ControlGestion controlGestion;
    Socket socket;
    HiloTemporizador hiloTemporizador;
    DataOutputStream dataOutputStream;
    
    public Protocolo(Socket socket, ControlGestion cg, HiloTemporizador hiloTemporizador){
        this.socket = socket;
        this.controlGestion = cg;
        this.hiloTemporizador = hiloTemporizador;
        
        try {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(Protocolo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public String processInput(String respuestaUsuario) throws IOException{
        String respuestaProtocolo = "";
        String [] resUsuario = respuestaUsuario.split("\\|\\|");
                        
        do{
            
            switch(state){
                
                case INICIO:
                    
                    switch(resUsuario[0]){
                        
                        case "1":
                            state = REGISTRAR_USUARIO;
                            transicionNula=true;
                            break;
                            
                        case "2":
                            state = LOG_ADMIN;
                            transicionNula=true;
                            break;
                            
                        case "3":
                            state = LOG_OUT;
                            transicionNula=true;
                            break;
                            
                        case "4":
                            state = INCIDENCIAS_TABLA;
                            transicionNula=true;
                            break;
                            
                        case "5":
                            state = DATOS_INCIDENCIA_NUEVA_REGISTRADA;
                            transicionNula=true;
                            break;
                            
                        case "6":
                            state = ASIGNAR_INCIDENCIA_SUPERVISOR;
                            transicionNula=true;
                            break;   
                            
                        case "7":
                            state = LISTADO_EMPLEADOS;
                            transicionNula=true;
                            break;
                            
                        case "8":
                            state = ASIGNAR_INCIDENCIA_EMPLEADO;
                            transicionNula=true;
                            break;    
                    }                    
                    break;
                    
                    case REGISTRAR_USUARIO:
                    
                        if(controlGestion.registrarUsuario(resUsuario[1],resUsuario[2],resUsuario[3],resUsuario[4],resUsuario[5],Integer.parseInt(resUsuario[6]),resUsuario[7], Integer.parseInt(resUsuario[8]))){
                            respuestaProtocolo += codigosProtocolo[1];
                        }else{
                            respuestaProtocolo += codigosProtocolo[2];
                        }

                        state = INICIO;
                        transicionNula=false;
                        break;
                        
                    case LOG_ADMIN:
                        String respuesta = controlGestion.logAdmin(resUsuario[1],resUsuario[2]);
                        //si no es nulo significa que ese usuario existe                    
                        if(respuesta != null){  
                            String [] respuestaS = respuesta.split("\\|\\|");                            
                            //si el tipo de usuario es 1, significa que ese usuario es administrador por lo que podremos loggearnos
                            
                            if(respuestaS[0].equals("sesionIniciadaEnOtroDispositivo")){ 
                                respuestaProtocolo += codigosProtocolo[5];
                            }else{
                                if(respuestaS[0].equals("1")){
                                    respuestaProtocolo += codigosProtocolo[3]+respuestaS[1]+"||";
                                    correoUsuario = resUsuario[1];
                                    hiloTemporizador.start();
                                }else{
                                    respuestaProtocolo += codigosProtocolo[4];
                                }
                            }
                        
                        
                        }else{
                            respuestaProtocolo += codigosProtocolo[4];
                        }

                        state = INICIO;
                        transicionNula=false;
                        break;
                        
                    case LOG_OUT:
                    
                        controlGestion.logOutAdmin(correoUsuario);
                        respuestaProtocolo += codigosProtocolo[6];
                        
                        transicionNula=false;
                        state = INICIO;
                        break;
                    
                    case INCIDENCIAS_TABLA:

                        JSONArray lista_incidencias = controlGestion.datosIncidenciaTabla(resUsuario[1], resUsuario[2]);
                        //respuestaProtocolo += codigosProtocolo[7]+lista_incidencias.toJSONString()+"||";

                        dataOutputStream.writeInt(lista_incidencias.size());
                        for(int i = 0; i<lista_incidencias.size(); i++){
                            JSONObject object = (JSONObject) lista_incidencias.get(i);
                            dataOutputStream.writeUTF(object.toJSONString());
                        }

                        respuestaProtocolo = "";

                        transicionNula=false;
                        state = INICIO;
                        break;
                        
                    case DATOS_INCIDENCIA_NUEVA_REGISTRADA:
                    
                        JSONArray detalles_incidenciaNR = controlGestion.obtenerDetallesNuevaRegistrada(Integer.parseInt(resUsuario[1]));    
                    
                        dataOutputStream.write(detalles_incidenciaNR.toJSONString().getBytes());

                        respuestaProtocolo += codigosProtocolo[8];                    
                        respuestaProtocolo = "";
                    
                        transicionNula=false;
                        state = INICIO;
                        break;
                    
                case ASIGNAR_INCIDENCIA_SUPERVISOR:
                    
                    boolean supervisorAsignado = controlGestion.asignarIncidenciaSupervisor(Integer.parseInt(resUsuario[1]),resUsuario[2], resUsuario[3]);
                    if(supervisorAsignado){
                        respuestaProtocolo = codigosProtocolo[9];
                    }else{
                        respuestaProtocolo = codigosProtocolo[10];
                    }
                                        
                    transicionNula=false;
                    state = INICIO;
                    break; 
                    
                case LISTADO_EMPLEADOS:
                    
                    JSONObject object = controlGestion.obtenerEmpleados(Integer.parseInt(resUsuario[1]));

                    if(object == null){
                        dataOutputStream.writeUTF("null");
                    }else{

                        dataOutputStream.writeUTF(object.toString());
                    }
                        
                    respuestaProtocolo = "";
                
                    transicionNula=false;
                    state = INICIO;
                    break;
                    
                case ASIGNAR_INCIDENCIA_EMPLEADO:
                    
                    boolean empleadoAsignado = controlGestion.asignarIncidenciaEmpleado(Integer.parseInt(resUsuario[1]),resUsuario[2]);
                    if(empleadoAsignado){
                        respuestaProtocolo = codigosProtocolo[11];
                    }else{
                        respuestaProtocolo = codigosProtocolo[12];
                    }
                    
                    transicionNula=false;
                    state = INICIO;
                    break;    
                    
            }
            
        }while(transicionNula==true);
        
        return respuestaProtocolo;
        
    }

    public String getCorreoUsuario(){
        return correoUsuario;
    }
}
