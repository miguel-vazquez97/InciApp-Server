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
                                       "",
                                       "",
                                       "",
                                       "",
                                       "",
                                       "",
                                       "",
                                       "",
                                       "",
                                       "70||logOutOk||"};
    
    static String path;
    
    private int state = INICIO;
    private static final int INICIO = 0;
    private static final int REGISTRAR_USUARIO = 1;
    private static final int LOG_USUARIO = 2;
    private static final int LOG_OUT_USUARIO = 15;
    private static final int REGISTRAR_INCIDENCIA = 3;
    private static final int HISTORIAL_INCIDENCIAS = 4;    
    private static final int INCIDENCIAS_ACTIVAS = 5;
    private static final int DETALLES_INCIDENCIA = 8;
    
    static boolean transicionNula = false;
    
    String correoUsuario = null;
    ControlGestion controlGestion;
    Socket socket;
    HiloTemporizador hiloTemporizador;
    
    OutputStream out;
    InputStream input;
    DataOutputStream outputStream;
    DataInputStream inputStream;
    
    File file;
    OutputStream outputImagen;
    long longitud;
    byte[] bytesImagen;
    Long consecutivo;
    String name, pictureFile;


    public ProtocoloAndroid(Socket socket, ControlGestion cg, String path, HiloTemporizador hiloTemporizador){
        this.socket = socket;
        this.controlGestion = cg;
        this.path = path;
        this.hiloTemporizador = hiloTemporizador;
        
        try {
            out = socket.getOutputStream();
            input = socket.getInputStream();
            outputStream = new DataOutputStream(socket.getOutputStream());
            inputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(ProtocoloAndroid.class.getName()).log(Level.SEVERE, null, ex);
        }
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
                            
                        case "65":
                            state = LOG_OUT_USUARIO;
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
                            
                        case "60":
                            state = DETALLES_INCIDENCIA;
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
                        System.out.println(respuestaProtocolo);
                        respuestaProtocolo += correoUsuario + "||" + tipoUsuario + "||";                        
                    }else{
                        respuestaProtocolo += codigosProtocoloAndroid[4];
                    }
                
                    state = INICIO;
                    transicionNula=false;
                    break;  
                    
                case LOG_OUT_USUARIO:
                    
                    controlGestion.logOutUsuario(correoUsuario);
                    respuestaProtocolo = codigosProtocoloAndroid[20];
                    
                    state = INICIO;
                    transicionNula=false;
                    
                    break;
                    
                case REGISTRAR_INCIDENCIA:
                    
                    outputStream.writeUTF("EnviarImagen");
                    
                    consecutivo = System.currentTimeMillis() / 1000;
                    name = consecutivo.toString();
                    pictureFile = name+".jpg";
                    
                    file = new File(path+"\\"+pictureFile);
                    file.createNewFile();
                    outputImagen = new FileOutputStream(file);
                    while(inputStream.available()<1){}
                     longitud = inputStream.readLong();

                     while(file.length()<longitud){
                        bytesImagen = new byte[inputStream.available()];
                        inputStream.read(bytesImagen);
                        outputImagen.write(bytesImagen);                        
                    }                  
                    outputImagen.close();
                    
                    outputStream.writeUTF("EnviarDatosIncidencia");
                    String datosInci = inputStream.readUTF();
                    String [] datos = datosInci.split("\\|\\|");
                    String respuesta_registrar_incidencia = controlGestion.nuevaIncidencia(datos[0],datos[1],datos[2],datos[3],datos[4],file.getName());
                    
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
                    
                case DETALLES_INCIDENCIA:
                                    
                    JSONArray detalles_incidencia = controlGestion.obtenerDetallesIncidencia(Integer.parseInt(resUsuario[1]));                        
                    //codificamos JSONArray a Base64
                    BASE64Encoder encoder = new BASE64Encoder();
                    String base64 = encoder.encode(detalles_incidencia.toJSONString().getBytes());
                    //enviamos primero el tamano que tendra la cadena en Base64
                    outputStream.writeInt(base64.length());
                    //enviamos los bytes de dicha cadena
                    outputStream.write(base64.getBytes());
                                       
                    respuestaProtocolo = "";
                    state = INICIO;
                    transicionNula=false;
                    break;     
            }
            
        }while(transicionNula==true);
        
        return respuestaProtocolo;
        
    }
    
    public String getCorreoUsuario(){
        return correoUsuario;
    }

}
