package server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

/**
 *
 * @author mivap
 */
public class HiloServidor {
    public static void main(String args[]) throws IOException{
        ServerSocket serverSocket = null;
        boolean listening = true;

        //obtenemos la direccion donde se esta ejecutando el código
        String pathPrincipal = System.getProperty("user.dir");
        //añadimos la carpeta donde se guardaran las imagenes de las incidencias
        String pathImagenes = pathPrincipal+"\\imagenes_incidencias";
        File carpetaImagenes = new File(pathImagenes);
        //comprobamos si existe la carpeta
        if(!carpetaImagenes.exists()){
            //si no existe, la creamos
            if(carpetaImagenes.mkdirs()){
                System.out.println("Directorio para imagenes creado.");
            }else{
                System.out.println("No se ha podido crear el directorio para las imagenes.");
            }
        }
        
        ControlGestion controlGestion = new ControlGestion();
        
        try {
            System.out.println("Iniciado!");
            serverSocket = new ServerSocket(4444);
        } catch (IOException e) {
            System.err.println("Error puerto: 4444.");
            System.exit(-1);
        }
        
        while (listening){
            System.out.println("Nuevo");
            new HiloUsuario(serverSocket.accept(), controlGestion, pathImagenes).start();
        }

        serverSocket.close();
    }
}
  

