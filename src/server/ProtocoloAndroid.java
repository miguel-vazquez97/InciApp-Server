package server;

import java.io.IOException;
import java.net.Socket;

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
                                       "54||logUsuarioDenegado||"};
    
    static String path;
    
    private int state = INICIO;
    private static final int INICIO = 0;
    private static final int REGISTRAR_USUARIO = 1;
    private static final int LOG_USUARIO = 2;
    
    static boolean transicionNula = false;
    
    String correoUsuario = null;
    ControlGestion controlGestion;
    Socket socket;



    public ProtocoloAndroid(Socket socket, ControlGestion cg, String path){
        this.socket = socket;
        this.controlGestion = cg;
        this.path = path;
        
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
                
            }
            
        }while(transicionNula==true);
        
        return respuestaProtocolo;
        
    }

}
