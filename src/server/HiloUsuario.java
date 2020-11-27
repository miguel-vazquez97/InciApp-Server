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
    
    protected Socket usuario;    
    protected ControlGestion controlGestion;
    protected String path;
    protected boolean finalizar = false;
    protected HiloTemporizador hiloTemporizador;
    // 0 == aplicacion escritorio ; 1 == aplicacion móvil
    protected int tipoAplicacion;
    protected Protocolo protocolo;
    protected ProtocoloAndroid protocoloAndroid;
    
    HiloUsuario(Socket socket, ControlGestion cg, String path) {
        this.usuario = socket;
        this.controlGestion = cg;  
        this.path = path;
        //lanzaremos este hilo cuando el usuario haga login para así llevar un control de la sesion
        //una vez pase un tiempo determinado sin que el usuario haya interaccionado con el servidor cerraremos su sesion
        hiloTemporizador = new HiloTemporizador(this);
    }
    
    public void run(){

        InputStream leerUsuario = null;
        OutputStream enviarUsuario = null;
        byte[] respuestaUsu;       
        String mensajeUsuario = "", mensajeProtocolo = "";
        
        try {
            
            enviarUsuario = usuario.getOutputStream();
            leerUsuario = usuario.getInputStream();
            
            byte[] enviarUsu = "Conexion con servidor con exito!".getBytes();
            
            enviarUsuario.write(enviarUsu);
            enviarUsuario.flush();
            
            while(leerUsuario.available()<1){}
            respuestaUsu = new byte[leerUsuario.available()];
            leerUsuario.read(respuestaUsu);
            mensajeUsuario = new String(respuestaUsu);
            System.out.println(mensajeUsuario);
            //enviaremos un primer mensaje por parte de la app escritorio/movil cuando conectemos con el servidor
            //para establecer que clase de app se ha conectado
            if(mensajeUsuario.equals("ConectadoAppEscritorio||0||")){
                tipoAplicacion = 0;
                protocolo = new Protocolo(usuario, controlGestion, hiloTemporizador);
            }else{
                tipoAplicacion = 1;
                protocoloAndroid = new ProtocoloAndroid(usuario, controlGestion, path, hiloTemporizador);    
            }
            
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
                    }else{
                        mensajeProtocolo = protocoloAndroid.processInput(mensajeUsuario);                                              
                    }
                    
                    enviarUsu = mensajeProtocolo.getBytes();
                    enviarUsuario.write(enviarUsu);
                    enviarUsuario.flush();
                    
                    if(mensajeProtocolo.equals("72||logOutOk||") || mensajeProtocolo.equals("6||logOutAdminOk||"))
                        finalizar=true;
                    
                }                
                Thread.sleep(1000);
                if(usuario.isClosed())
                    finalizar=true;
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
