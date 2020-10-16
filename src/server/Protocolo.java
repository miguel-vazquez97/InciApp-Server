package server;

import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author mivap
 */
public class Protocolo {
    private static final String[] codigosProtocolo={};
  
    
    private int state = INICIO;
    private static final int INICIO = 0;    
    
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

        }while(transicionNula==true);
        
        return respuestaProtocolo;
        
    }
}
