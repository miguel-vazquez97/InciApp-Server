package server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import sun.misc.BASE64Encoder;

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
                                       "12||empleadoAsignadoDenegado||",
                                       "13||incidenciaSolucionadaOk||",
                                       "14||incidenciaSolucionadaDenegado||",
                                       "15||incidenciaDenegadaSolucionOk||",
                                       "16||incidenciaDenegadaSolucionNoOk||",
                                       "17||registroDepartamentoOk||",
                                       "18||registroDepartamentoYaExiste||",
                                       "19||registroDepartamentoDenegado||",
                                       "20||eliminarDepartamentoOk||",
                                       "21||eliminarDepartamentoDenegado||",
                                       "22||modificarUsuarioOk||",
                                       "23||modificarUsuarioDenegado||",
                                       "24||eliminarUsuarioOk||",
                                       "25||eliminarUsuarioDenegado||"};
  
    
    private int state = INICIO;
    private static final int INICIO = 0;
    private static final int REGISTRAR_USUARIO = 1;    
    private static final int LOG_ADMIN = 2;
    private static final int LOG_OUT = 3;
    private static final int INCIDENCIAS_TABLA = 4;
    private static final int DATOS_INCIDENCIA_ESTADO = 5;
    private static final int ASIGNAR_INCIDENCIA_SUPERVISOR = 6;
    private static final int ASIGNAR_INCIDENCIA_EMPLEADO = 8;
    private static final int DATOS_INCIDENCIA_ARREGLADA = 9;
    private static final int SOLUCIONAR_INCIDENCIA = 10;
    private static final int DETALLES_INCIDENCIA = 11;
    private static final int DENEGAR_SOLUCION_INCIDENCIA = 12;
    private static final int LISTADO_DEPARTAMENTOS = 13;
    private static final int REGISTRAR_DEPARTAMENTO = 14;
    private static final int ELIMINAR_DEPARTAMENTO = 15;
    
    private static final int LISTADO_USUARIOS = 20;
    private static final int DETALLES_USUARIO = 21;
    private static final int MODIFICAR_USUARIO = 22;
    private static final int ELIMINAR_USUARIO = 23;
    
    static boolean transicionNula = false;
    
    String correoUsuario;
    ControlGestion controlGestion;
    Socket socket;
    HiloTemporizador hiloTemporizador;
    DataOutputStream dataOutputStream;
    
    BASE64Encoder encoder;
    String base64;
    
    public Protocolo(Socket socket, ControlGestion cg, HiloTemporizador hiloTemporizador){
        this.socket = socket;
        this.controlGestion = cg;
        this.hiloTemporizador = hiloTemporizador;
        
        try {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(Protocolo.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        encoder = new BASE64Encoder();
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
                            state = DATOS_INCIDENCIA_ESTADO;
                            transicionNula=true;
                            break;
                            
                        case "6":
                            state = ASIGNAR_INCIDENCIA_SUPERVISOR;
                            transicionNula=true;
                            break;   

                        case "8":
                            state = ASIGNAR_INCIDENCIA_EMPLEADO;
                            transicionNula=true;
                            break;   
                            
                        case "9":
                            state = DATOS_INCIDENCIA_ARREGLADA;
                            transicionNula=true;
                            break;
                            
                        case "10":
                            state = SOLUCIONAR_INCIDENCIA;
                            transicionNula=true;
                            break;
                            
                        case "11":
                            state = DETALLES_INCIDENCIA;
                            transicionNula=true;
                            break; 
                        case "12":
                            state = DENEGAR_SOLUCION_INCIDENCIA;
                            transicionNula=true;
                            break;

                        case "13":
                            state = LISTADO_DEPARTAMENTOS;
                            transicionNula=true;
                            break;
                            
                        case "14":
                            state = REGISTRAR_DEPARTAMENTO;
                            transicionNula=true;
                            break;
                            
                        case "15":
                            state = ELIMINAR_DEPARTAMENTO;
                            transicionNula=true;
                            break;
                            
                        case "20":
                            state = LISTADO_USUARIOS;
                            transicionNula=true;
                            break;
                            
                        case "21":
                            state = DETALLES_USUARIO;
                            transicionNula=true;
                            break;
                            
                        case "22":
                            state = MODIFICAR_USUARIO;
                            transicionNula=true;
                            break;

                        case "23":
                            state = ELIMINAR_USUARIO;
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
                        
                    case DATOS_INCIDENCIA_ESTADO:
                    
                        JSONArray detalles_incidencia = controlGestion.obtenerDetallesIncidenciaEstado(Integer.parseInt(resUsuario[1]), resUsuario[2]);    
                    
                        dataOutputStream.write(detalles_incidencia.toJSONString().getBytes());

                        //respuestaProtocolo += codigosProtocolo[8];                    
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

                case DATOS_INCIDENCIA_ARREGLADA:
                    
                    JSONArray detalles_incidenciaArreglada = controlGestion.obtenerDetallesIncidenciaArreglada(Integer.parseInt(resUsuario[1]));        
                    
                    base64 = encoder.encode(detalles_incidenciaArreglada.toJSONString().getBytes());
                    dataOutputStream.writeInt(base64.length());
                    dataOutputStream.write(base64.getBytes());  
                    
                    respuestaProtocolo = "";                    
                    transicionNula=false;
                    state = INICIO;
                    break;
                    
                case SOLUCIONAR_INCIDENCIA:
                    
                    boolean incidenciaSolucionada = controlGestion.solucionarIncidencia(Integer.parseInt(resUsuario[1]));
                    if(incidenciaSolucionada){
                        respuestaProtocolo = codigosProtocolo[13];
                    }else{
                        respuestaProtocolo = codigosProtocolo[14];
                    }
                    
                    transicionNula=false;
                    state = INICIO;
                    break;
                    
                case DENEGAR_SOLUCION_INCIDENCIA:
                    
                    boolean denegarSolucion = controlGestion.denegarSolucionIncidencia(Integer.parseInt(resUsuario[1]), resUsuario[2]);
                    if(denegarSolucion){
                        respuestaProtocolo = codigosProtocolo[15];
                    }else{
                        respuestaProtocolo = codigosProtocolo[16];
                    }
                    
                    transicionNula=false;
                    state = INICIO;
                    break;
                    
                case DETALLES_INCIDENCIA:
                    
                    JSONArray detalles_incidenciaH = controlGestion.obtenerHistorialIncidencia(Integer.parseInt(resUsuario[1])); 
                    
                    //codificamos JSONArray a Base64
                    base64 = encoder.encode(detalles_incidenciaH.toJSONString().getBytes());
                    //enviamos primero el tamano que tendra la cadena en Base64
                    dataOutputStream.writeInt(base64.length());
                    //enviamos los bytes de dicha cadena
                    dataOutputStream.write(base64.getBytes());
                    
                    respuestaProtocolo="";
                    transicionNula=false;
                    state = INICIO;
                    break; 
                    
                case LISTADO_DEPARTAMENTOS:
                    
                    JSONObject objectDepartamentos = controlGestion.obtenerDepartamentos();
                    dataOutputStream.writeUTF(objectDepartamentos.toJSONString());
                    
                    respuestaProtocolo="";
                    transicionNula=false;
                    state = INICIO;
                    break; 
                    
                case REGISTRAR_DEPARTAMENTO:
                    
                    String res = controlGestion.registrarDepartamento(resUsuario[1]);
                    
                    switch(res){
                        case "registrado":
                            respuestaProtocolo += codigosProtocolo[17];
                            break;
                        case "yaExiste":
                            respuestaProtocolo += codigosProtocolo[18];
                            break;
                        case "denegado":
                            respuestaProtocolo += codigosProtocolo[19];
                            break;
                    }
                    
                    transicionNula=false;
                    state = INICIO;
                    break;
                    
                case ELIMINAR_DEPARTAMENTO:
                    
                    boolean departamentoEliminado = controlGestion.eliminarDepartamento(Integer.parseInt(resUsuario[1]));
                    if(departamentoEliminado){
                        respuestaProtocolo = codigosProtocolo[20];
                    }else{
                        respuestaProtocolo = codigosProtocolo[21];
                    }
                    
                    transicionNula=false;
                    state = INICIO;
                    break;   
                    
                case LISTADO_USUARIOS:
                    
                    JSONArray lista_usuarios = controlGestion.listadoUsuariosTabla(Integer.parseInt(resUsuario[1]));
                    base64 = encoder.encode(lista_usuarios.toJSONString().getBytes());
                    dataOutputStream.writeInt(base64.length());
                    dataOutputStream.write(base64.getBytes());
                    
                    
                    transicionNula=false;
                    state = INICIO;
                    break;
                    
                case DETALLES_USUARIO:
                    JSONObject detalle_usuario = controlGestion.obtenerDetallesUsuario(resUsuario[1]);
                    dataOutputStream.writeUTF(detalle_usuario.toJSONString());
                    
                    transicionNula=false;
                    state = INICIO;
                    break;
                    
                case MODIFICAR_USUARIO:
                    
                    boolean modificarUsuario = controlGestion.modificarUsuarioEscritorio(resUsuario[1],resUsuario[2],resUsuario[3],resUsuario[4],Integer.parseInt(resUsuario[5]), resUsuario[6]);
                    if(modificarUsuario){
                        respuestaProtocolo = codigosProtocolo[22];
                    }else{
                        respuestaProtocolo = codigosProtocolo[23];
                    }
                    
                    transicionNula=false;
                    state = INICIO;
                    break;  

                case ELIMINAR_USUARIO:

                    boolean eliminarUsuario = controlGestion.eliminarUsuario(resUsuario[1]);
                    if(eliminarUsuario){
                        respuestaProtocolo = codigosProtocolo[24];
                    }else{
                        respuestaProtocolo = codigosProtocolo[25];
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
