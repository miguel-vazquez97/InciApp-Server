package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
public class ProtocoloAndroid {
     private static final String[] codigosProtocoloAndroid={
                                       "50||registrarUsuarioOk||",
                                       "51||registrarUsuarioDenegado||",
                                       "",
                                       "53||logUsuarioOk||",
                                       "54||logUsuarioDenegado||",
                                       "55||registrar_nueva_incidencia||",
                                       "56||incidenciaOk||",
                                       "57||incidenciaDenegada||",
                                       "58||incidenciaYaRegistrada||",
                                       "59||listadoIncidenciasOk||",
                                       "60||detallesIncidenciaOk||",
                                       "61||detallesIncidenciaSupervisorOk||",
                                       "62||opcionEnTramiteIncidenciaValidada||",
                                       "63||opcionEnTramiteIncidenciaDenegada||",
                                       "64||opcionValidarArregloIncidenciaValidada||",
                                       "65||opcionValidarArregloIncidenciaDenegada||",
                                       "66||detallesIncidenciaEmpleadoOk||",
                                       "67||esperandoImagenArregloIncidencia||",
                                       "68||arregloIncidenciaOk||",
                                       "69||arregloIncidenciaNo||",
                                       "70||esperandoImagenNuevaRegistrada||",
                                       "71||enviarDatosNuevaRegistrada||",
                                       "72||logOutOk||",
                                       "73||modificarUsuarioOk||",
                                       "74||modificarUsuarioDenegado||"};
    
    static String path;
    
    private int state = INICIO;
    private static final int INICIO = 0;
    private static final int REGISTRAR_USUARIO = 1;
    private static final int LOG_USUARIO = 2;    
    private static final int REGISTRAR_INCIDENCIA = 3;
    private static final int HISTORIAL_INCIDENCIAS = 4;    
    private static final int INCIDENCIAS_ACTIVAS = 5;    
    private static final int INCIDENCIAS_ENTRAMITE = 6;
    private static final int INCIDENCIAS_VALIDARARREGLO = 7;
    private static final int DETALLES_INCIDENCIA = 8;    
    private static final int DETALLES_INCIDENCIA_SUPERVISOR = 9;
    private static final int OPCION_ENTRAMITE = 10;
    private static final int OPCION_VALIDAR_ARREGLO = 11;
    private static final int INCIDENCIAS_ENARREGLO = 12;
    private static final int DETALLES_INCIDENCIA_EMPLEADO = 13;
    private static final int ENVIAR_ARREGLO = 14;
    private static final int LOG_OUT_USUARIO = 15;    
    private static final int DETALLES_USUARIO = 16;
    private static final int MODIFICAR_USUARIO = 17;
    
    static boolean transicionNula = false;
    
    String correoUsuario = null;
    ControlGestion controlGestion;
    Socket socket;
    HiloTemporizador hiloTemporizador;
    
    OutputStream output;
    InputStream input;
    DataOutputStream dataOutputStream;
    DataInputStream dataInputStream;
    
    File file;
    OutputStream outputImagen;
    long longitud;
    byte[] bytesImagen;
    Long consecutivo;
    String name, pictureFile;

    BASE64Encoder encoder;
    String base64;
    
    String respuestaControlGestion;

    public ProtocoloAndroid(Socket socket, ControlGestion cg, String path, HiloTemporizador hiloTemporizador){
        this.socket = socket;
        this.controlGestion = cg;
        this.path = path;
        this.hiloTemporizador = hiloTemporizador;
        
        try {
            output = socket.getOutputStream();
            input = socket.getInputStream();
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(ProtocoloAndroid.class.getName()).log(Level.SEVERE, null, ex);
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
                        case "51":
                            state = REGISTRAR_USUARIO;
                            transicionNula=true;
                            break;
                            
                        case "52":
                            state = LOG_USUARIO;
                            transicionNula=true;
                            break;
                            
                        case "53":
                            state = REGISTRAR_INCIDENCIA;
                            transicionNula=true;
                            break;                            
                        
                        case "54":
                            state = HISTORIAL_INCIDENCIAS;
                            transicionNula=true;
                            break;
                            
                        case "55":
                            state = INCIDENCIAS_ACTIVAS;
                            transicionNula=true;
                            break;  
                            
                        case "56":
                            state = INCIDENCIAS_ENTRAMITE;
                            transicionNula=true;
                            break;  
                            
                        case "57":
                            state = INCIDENCIAS_VALIDARARREGLO;
                            transicionNula=true;
                            break;
                            
                        case "58":
                            state = INCIDENCIAS_ENARREGLO;
                            transicionNula=true;
                            break;
                            
                        case "59":
                            state = DETALLES_INCIDENCIA_EMPLEADO;
                            transicionNula=true;
                            break;    
                            
                        case "60":
                            state = DETALLES_INCIDENCIA;
                            transicionNula=true;
                            break;   
                            
                        case "61":
                            state = DETALLES_INCIDENCIA_SUPERVISOR;
                            transicionNula=true;
                            break;
                            
                        case "62":
                            state = OPCION_ENTRAMITE;
                            transicionNula=true;
                            break;  
                            
                        case "63":
                            state = OPCION_VALIDAR_ARREGLO;
                            transicionNula=true;
                            break;
                            
                        case "64":
                            state = ENVIAR_ARREGLO;
                            transicionNula=true;
                            break; 
                            
                        case "65":
                            state = LOG_OUT_USUARIO;
                            transicionNula=true;
                            break;    
                            
                        case "66":
                            state = DETALLES_USUARIO;
                            transicionNula=true;
                            break;
                        
                        case "67":
                            state = MODIFICAR_USUARIO;
                            transicionNula=true;
                            break;    
                    }
                    
                    break;
                    
                case REGISTRAR_USUARIO:
                    
                    if(controlGestion.registrarUsuario(resUsuario[1],resUsuario[2],resUsuario[3],resUsuario[4],resUsuario[5],Integer.parseInt(resUsuario[6]), "NULL", 4)){
                        respuestaProtocolo += codigosProtocoloAndroid[0];
                    }else{
                        respuestaProtocolo += codigosProtocoloAndroid[1];
                    }
                    
                    state = INICIO;
                    transicionNula=false;
                    break;    
                    
                case LOG_USUARIO:
                    int tipoUsuario = controlGestion.logUsuario(resUsuario[1],resUsuario[2],Boolean.parseBoolean(resUsuario[3]));
                    if(tipoUsuario != 0){
                        System.out.println(tipoUsuario);
                        correoUsuario = resUsuario[1];
                        respuestaProtocolo += codigosProtocoloAndroid[3];
                        respuestaProtocolo += correoUsuario + "||" + tipoUsuario + "||";                        
                        hiloTemporizador.start();
                    }else{
                        respuestaProtocolo += codigosProtocoloAndroid[4];
                    }
                
                    state = INICIO;
                    transicionNula=false;
                    break;  
                    
                case LOG_OUT_USUARIO:
                    
                    controlGestion.logOutUsuario(correoUsuario);
                    respuestaProtocolo = codigosProtocoloAndroid[22];
                    
                    state = INICIO;
                    transicionNula=false;
                    
                    break;
                    
                case REGISTRAR_INCIDENCIA:
                    respuestaProtocolo = codigosProtocoloAndroid[20];
                    dataOutputStream.writeUTF(respuestaProtocolo);
                    
                    consecutivo = System.currentTimeMillis() / 1000;
                    name = consecutivo.toString();
                    pictureFile = name+".jpg";
                    
                    file = new File(path+"\\"+pictureFile);
                    file.createNewFile();
                    outputImagen = new FileOutputStream(file);
                    while(dataInputStream.available()<1){}
                     longitud = dataInputStream.readLong();

                     while(file.length()<longitud){
                        bytesImagen = new byte[dataInputStream.available()];
                        dataInputStream.read(bytesImagen);
                        outputImagen.write(bytesImagen);                        
                    }                  
                    outputImagen.close();
                    
                    respuestaProtocolo = codigosProtocoloAndroid[21];
                    dataOutputStream.writeUTF(respuestaProtocolo);                    
                    String datosInci = dataInputStream.readUTF();
                    String [] datos = datosInci.split("\\|\\|");
                    String respuesta_registrar_incidencia = controlGestion.nuevaIncidencia(datos[0],datos[1],datos[2],datos[3],datos[4],file.getName());
                    
                    respuestaProtocolo="";
                    switch(respuesta_registrar_incidencia){
                        case "Ok":
                            respuestaProtocolo += codigosProtocoloAndroid[6];
                            break;
                        case "NoOk":
                            respuestaProtocolo += codigosProtocoloAndroid[7];
                            break;
                        case "YaExiste":
                            respuestaProtocolo += codigosProtocoloAndroid[8];
                            break;
                    }     
                    
                    dataOutputStream.writeUTF(respuestaProtocolo);
                    respuestaProtocolo="";
                    
                    state = INICIO;
                    transicionNula=false;
                    break;    
                
                    
                case HISTORIAL_INCIDENCIAS:
                    
                    JSONArray incidencias = controlGestion.obtenerListadoIncidencias(resUsuario[1],resUsuario[2]);
                    respuestaProtocolo += codigosProtocoloAndroid[9]+incidencias.toString()+"||";  

                    state = INICIO;
                    transicionNula=false;
                    break;    
                    
                case INCIDENCIAS_ACTIVAS:
                    JSONArray incidencias_activas = controlGestion.obtenerListadoIncidencias(resUsuario[1],resUsuario[2]);
                    respuestaProtocolo += codigosProtocoloAndroid[9]+incidencias_activas.toString()+"||";
                    
                    state = INICIO;
                    transicionNula=false;
                    break;
                    
                case INCIDENCIAS_ENTRAMITE:
                    JSONArray incidencias_enTramite = controlGestion.obtenerListadoIncidencias(resUsuario[1],resUsuario[2]);
                    respuestaProtocolo += codigosProtocoloAndroid[9]+incidencias_enTramite.toString()+"||";
                    
                    state = INICIO;
                    transicionNula=false;
                    break;   
                    
                case INCIDENCIAS_ENARREGLO:
                    JSONArray incidencias_enArreglo = controlGestion.obtenerListadoIncidencias(resUsuario[1],resUsuario[2]);
                    respuestaProtocolo += codigosProtocoloAndroid[9]+incidencias_enArreglo.toString()+"||";

                    state = INICIO;
                    transicionNula=false;
                    break; 
                    
                case INCIDENCIAS_VALIDARARREGLO:
                    JSONArray incidencias_validarArreglo = controlGestion.obtenerListadoIncidencias(resUsuario[1],resUsuario[2]);
                    respuestaProtocolo += codigosProtocoloAndroid[9]+incidencias_validarArreglo.toString()+"||"; 
                    
                    state = INICIO;
                    transicionNula=false;
                    break;    
                    
                case DETALLES_INCIDENCIA:
                                    
                    JSONArray detalles_incidencia = controlGestion.obtenerDetallesIncidencia(Integer.parseInt(resUsuario[1]));                        
                    //codificamos JSONArray a Base64
                    
                    base64 = encoder.encode(detalles_incidencia.toJSONString().getBytes());
                    //enviamos primero el tamano que tendra la cadena en Base64
                    dataOutputStream.writeInt(base64.length());
                    //enviamos los bytes de dicha cadena
                    dataOutputStream.write(base64.getBytes());
                                       
                    respuestaProtocolo = "";
                    state = INICIO;
                    transicionNula=false;
                    break;  
                    
                case DETALLES_INCIDENCIA_SUPERVISOR:
                    
                    JSONObject detalle_incidencia_supervisor = controlGestion.obtenerDetallesIncidenciaSupervisor(Integer.parseInt(resUsuario[1]),resUsuario[2]);
                    
                    base64 = encoder.encode(detalle_incidencia_supervisor.toJSONString().getBytes());
                    dataOutputStream.writeInt(base64.length());
                    dataOutputStream.write(base64.getBytes());
                                        
                    respuestaProtocolo = "";
                    state = INICIO;
                    transicionNula=false;
                    
                    break;
                    
                case DETALLES_INCIDENCIA_EMPLEADO:
                    
                    JSONObject detalle_incidencia_empleado = controlGestion.obtenerDetallesIncidenciaEmpleado(Integer.parseInt(resUsuario[1]));
                    
                    base64 = encoder.encode(detalle_incidencia_empleado.toJSONString().getBytes());
                    dataOutputStream.writeInt(base64.length());
                    dataOutputStream.write(base64.getBytes());

                    respuestaProtocolo = "";
                    
                    state = INICIO;
                    transicionNula=false;
                    
                    break;    
                    
                case OPCION_ENTRAMITE:

                    respuestaControlGestion = controlGestion.validacionIncidenciaSupervisor(Integer.parseInt(resUsuario[1]), resUsuario[2], resUsuario[3]);
                    
                    switch(respuestaControlGestion){
                        case "validadaOk":
                            respuestaProtocolo += codigosProtocoloAndroid[12];
                            break;
                        case "denegadaOk":
                            respuestaProtocolo += codigosProtocoloAndroid[13];
                            break;
                    }
                                        
                    state = INICIO;
                    transicionNula=false;
                    
                    break; 
                    
                case ENVIAR_ARREGLO:

                    respuestaProtocolo = codigosProtocoloAndroid[17];         
                    dataOutputStream.writeUTF(respuestaProtocolo);
                     
                    consecutivo = System.currentTimeMillis() / 1000;
                    name = consecutivo.toString();
                    pictureFile = name+".jpg";
                    file = new File(path+"\\"+pictureFile);
                    file.createNewFile();
                    
                     outputImagen = new FileOutputStream(file);
                     while(dataInputStream.available()<1){}
                     longitud = dataInputStream.readLong();
                     
                     while(file.length()<longitud){
                        bytesImagen = new byte[dataInputStream.available()];
                        dataInputStream.read(bytesImagen);
                        outputImagen.write(bytesImagen);
                        
                    }                  
                    outputImagen.close();

                     
                     boolean arregloRegistrado = controlGestion.arregloIncidenciaEmpleado(Integer.parseInt(resUsuario[2]),resUsuario[3],file.getName());
                     if(arregloRegistrado){
                         respuestaProtocolo = codigosProtocoloAndroid[18];
                     }else{
                         respuestaProtocolo = codigosProtocoloAndroid[19];
                     }
                     
                    state = INICIO;
                    transicionNula=false;
                    
                    break;    
                    
                case OPCION_VALIDAR_ARREGLO:

                    respuestaControlGestion = controlGestion.validacionArregloIncidenciaSupervisor(Integer.parseInt(resUsuario[1]), resUsuario[2], resUsuario[3]);
                    
                    switch(respuestaControlGestion){
                        case "validadaArregloOk":
                            respuestaProtocolo += codigosProtocoloAndroid[14];
                            break;
                        case "denegadaArregloOk":
                            respuestaProtocolo += codigosProtocoloAndroid[15];
                            break;
                    }
                    
                    state = INICIO;
                    transicionNula=false;                    
                    break;    
                    
                case DETALLES_USUARIO:
                    JSONObject detalle_usuario = controlGestion.obtenerDetallesUsuario(resUsuario[1]);
                    dataOutputStream.writeUTF(detalle_usuario.toJSONString());
                    
                    transicionNula=false;
                    state = INICIO;
                    break;
                    
                case MODIFICAR_USUARIO:
                    
                    boolean modificarUsuario = controlGestion.modificarUsuarioMovil(resUsuario[1],resUsuario[2],resUsuario[3],resUsuario[4], resUsuario[5], Integer.parseInt(resUsuario[6]));
                    if(modificarUsuario){
                        respuestaProtocolo = codigosProtocoloAndroid[23];
                    }else{
                        respuestaProtocolo = codigosProtocoloAndroid[24];
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
