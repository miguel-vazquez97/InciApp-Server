package server;

import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author mivap
 */
public class ProtocoloAndroid {
     private static final String[] codigosProtocoloAndroid={};
    
    static String path;
    
    private int state = INICIO;
    private static final int INICIO = 0;
    
    static boolean transicionNula = false;
    
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
        
        System.out.println(resUsuario[0]);
                
        do{
        }while(transicionNula==true);
        
        return respuestaProtocolo;
        
    }

}
