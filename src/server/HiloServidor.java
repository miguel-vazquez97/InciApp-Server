package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.ibatis.common.jdbc.ScriptRunner;
import java.io.Reader;

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
        
        crearBaseDatos(pathPrincipal);
        
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
    
    public static void crearBaseDatos(String path){
        
        Connection connection = conectarBaseDatos();        

        try {
            // Inicializamos el objeto ScriptSunner
            ScriptRunner sr = new ScriptRunner(connection, false, false);

            // Objtenemos Reader del script con la base de datos
            Reader reader = new BufferedReader( new FileReader(path+"\\scriptInciAppDatabase.sql"));

            // Ejecutamos el script
            sr.runScript(reader);

        } catch (IOException | SQLException e) {
            System.err.println("Error al crear base de datos");
        }
        
    }
    
    public static Connection conectarBaseDatos(){
        String mysql_jdbd_driver = "com.mysql.jdbc.Driver";
        String prefix = "jdbc:" + "mysql:";
        String hostName = "//localhost:3306/";

        String url = prefix + hostName;

        String driver = mysql_jdbd_driver;
        String user = "root";
        String password = "";
        
        Connection conexion = null;
        
        try {            
            Class.forName(driver);
            conexion = (Connection) DriverManager.getConnection(url, user, password);
            return conexion;
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(HiloServidor.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return conexion;
    }
 }
  

