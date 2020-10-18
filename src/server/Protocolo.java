package server;

import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author mivap
 */
public class Protocolo {
    private static final String[] codigosProtocolo={"",
                                       "1||registrarUsuarioOk||",
                                       "2||registrarUsuarioDenegado||",
                                       "3||logAdminOk||",
                                       "4||logAdminDenegado||"};
  
    
    private int state = INICIO;
    private static final int INICIO = 0;
    private static final int REGISTRAR_USUARIO = 1;    
    private static final int LOG_ADMIN = 2;
    
    static boolean transicionNula = false;
    
    String correoUsuario;
    ControlGestion controlGestion;
    Socket socket;
    
    public Protocolo(Socket socket, ControlGestion cg){
        this.socket = socket;
        this.controlGestion = cg;
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
                            System.out.println(respuesta);
                            
                            String [] respuestaS = respuesta.split("\\|\\|");
                            
                            //si el tipo de usuario es 1, significa que ese usuario es administrador por lo que podremos loggearnos
                            if(respuestaS[0].equals("1")){
                                respuestaProtocolo += codigosProtocolo[3]+respuestaS[1]+"||";
                                correoUsuario = resUsuario[1];
                            }else{
                                respuestaProtocolo += codigosProtocolo[4];
                            }
                        }else{
                            respuestaProtocolo += codigosProtocolo[4];
                        }

                        state = INICIO;
                        transicionNula=false;
                        break;
                    
            }
            
        }while(transicionNula==true);
        
        return respuestaProtocolo;
        
    }
}
