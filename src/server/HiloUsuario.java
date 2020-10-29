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
    boolean finalizar = false;
    HiloTemporizador hiloTemporizador;
    // 0 == aplicacion escritorio ; 1 == aplicacion móvil
    int tipoAplicacion;
    
    HiloUsuario(Socket socket, ControlGestion cg, String path) {
        this.usuario = socket;
        this.controlGestion = cg;  
        this.path = path;
        hiloTemporizador = new HiloTemporizador(this);
    }
    
    public void run(){
        Protocolo protocolo = new Protocolo(usuario, controlGestion, hiloTemporizador);
        ProtocoloAndroid protocoloAndroid = new ProtocoloAndroid(usuario, controlGestion, path, hiloTemporizador);
        //lanzaremos este hilo cuando el usuario haga login para así llevar un control de la sesion
        //una vez pase un tiempo determinado sin que el usuario haya interaccionado con el servidor cerraremos su sesion
        
        InputStream leerUsuario = null;
        OutputStream enviarUsuario = null;
       
        String mensajeUsuario;
        String mensajeProtocolo = "";
        
        try {
            
            enviarUsuario = usuario.getOutputStream();
            leerUsuario = usuario.getInputStream();
            
            byte[] enviarUsu = "Conexion con servidor con exito!".getBytes();
            
            enviarUsuario.write(enviarUsu);
            enviarUsuario.flush();
            
            byte[] respuestaUsu;
            while(!finalizar){
                
                if(leerUsuario.available()>0){
                    //si el cliente ha interactuado, igualamos el tiempo de espera a 0
                    hiloTemporizador.interrupt();
                                        
                    respuestaUsu = new byte[leerUsuario.available()];
                    leerUsuario.read(respuestaUsu);
                    mensajeUsuario = new String(respuestaUsu);
                    System.out.println(mensajeUsuario);
                    //mensajes menores o iguales a id 50 se trataran en el protocolo para la app escritorio 
                    if(Integer.parseInt(mensajeUsuario.substring(0, mensajeUsuario.indexOf("|"))) <= 50 ){
                        mensajeProtocolo = protocolo.processInput(mensajeUsuario);  
                        tipoAplicacion = 0;
                    }else{
                        mensajeProtocolo = protocoloAndroid.processInput(mensajeUsuario);                      
                        tipoAplicacion = 1;
                    }
                    
                    enviarUsu = mensajeProtocolo.getBytes();
                    enviarUsuario.write(enviarUsu);
                    enviarUsuario.flush();
                    
                    if(mensajeProtocolo.equals("70||logOutOk||") || mensajeProtocolo.equals("6||logOutAdminOk||"))
                        finalizar=true;
                    
                }                
                Thread.sleep(3000);
            }
            
                System.out.println("Salio");
                //cerramos la conexiones
                usuario.close();
                
        } catch (IOException ex) {
            Logger.getLogger(HiloUsuario.class.getName()).log(Level.SEVERE, null, ex);
        } catch(InterruptedException ie){
            //cuando se hagota el tiempo de interactividad, el hilo encargado nos enviara una interrupción para hacernoslo saber y cerrar la sesión
            System.out.println("Sesion caducada");  
           
            if(tipoAplicacion==0){
                controlGestion.logOutAdmin(protocolo.getCorreoUsuario());
            }else{
                controlGestion.logOutUsuario(protocoloAndroid.getCorreoUsuario());
            }
            
            try {
                usuario.close();
            } catch (IOException ex) {
                System.out.println("Error al cerrar socket. HiloUsuario");
            }
            
        }
    }    
    
}
