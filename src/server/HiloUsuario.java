package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mivap
 */
public class HiloUsuario extends Thread{
    
    Socket usuario;    
    ControlGestion controlGestion;
    String path;
    int tiempo_espera;
    
    HiloUsuario(Socket socket, ControlGestion cg, String path) {
        this.usuario = socket;
        this.controlGestion = cg;  
        this.path = path;
        this.tiempo_espera = 0;
    }
    
    public void run(){
        Protocolo protocolo = new Protocolo(usuario, controlGestion);
        ProtocoloAndroid protocoloAndroid = new ProtocoloAndroid(usuario, controlGestion, path);
        
        InputStream input = null;
        OutputStream enviarUsuario = null;
        boolean finalizar = false;
        String mensajeUsuario;
        String mensajeProtocolo;
        
        try {
            
            enviarUsuario = usuario.getOutputStream();
            input = usuario.getInputStream();
            
            byte[] enviarUsu = "Conexion con servidor con exito!".getBytes();
            
            enviarUsuario.write(enviarUsu);
            enviarUsuario.flush();
            
            byte[] respuestaUsu;
            while(!finalizar){
                
                if(input.available()>0){
                    //si el cliente ha interactuado, igualamos el tiempo de espera a 0
                    tiempo_espera = 0;
                    
                    respuestaUsu = new byte[input.available()];
                    input.read(respuestaUsu);
                    mensajeUsuario = new String(respuestaUsu);
                    System.out.println(mensajeUsuario);
                    //mensajes menores o iguales a id 50 se trataran en el protocolo para la app escritorio 
                    if(Integer.parseInt(mensajeUsuario.substring(0, mensajeUsuario.indexOf("|"))) <= 50 ){
                        mensajeProtocolo = protocolo.processInput(mensajeUsuario);
                        
                    }else{
                        mensajeProtocolo = protocoloAndroid.processInput(mensajeUsuario);                      
                    }
                    
                    enviarUsu = mensajeProtocolo.getBytes();
                    enviarUsuario.write(enviarUsu);
                    enviarUsuario.flush();
                    
                    if(mensajeProtocolo.equals("71||salirAppOk||"))
                        finalizar=true;
                    
                }                
                Thread.sleep(3000);
                tiempo_espera += 3000;
                //con esto comprobamos que si lleva 30 mnts sin actividad con el cliente
                if(tiempo_espera==1800000)
                    finalizar=true;
            }
            System.out.println("Salio");
            //comprobamos si tenia sesion iniciada el cliente
            //String correo = protocoloAndroid.getCorreo();
            //if(correo!=null)
                //si es así, cerramos su sesion
                //controlGestion.logOutUsuario(correo);
            //cerramos la conexion
            usuario.close();
            
        } catch (IOException ex) {            
            Logger.getLogger(HiloUsuario.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {     
            Logger.getLogger(HiloUsuario.class.getName()).log(Level.SEVERE, null, ex);
        }
            
    }    
    
}
