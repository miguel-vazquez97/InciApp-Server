package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author mivap
 */
public class ControlGestion {
    
    //  COLECCIONES    
    
    //solo usuarios administradores
    ArrayList<String> usuariosAdminLog = new ArrayList<>();
    //resto de usuarios    
    HashMap<String, Integer> usuariosLog = new HashMap<>();
    
    //  CONEXION
    static String mysql_jdbd_driver="com.mysql.jdbc.Driver";
    static String prefix="jdbc:"+"mysql:";
    static String hostName="//localhost:3306/";
    static String urlFolder="";
    static String dbName="inciappdatabase";
    
    static String url=prefix+hostName+urlFolder+dbName;
		
    static String driver=mysql_jdbd_driver;
    static String user="root";
    static String password="";
    
    
    private String pathPrincipal;
    private String pathImagenes;
    
    ControlGestion(){
        pathPrincipal = System.getProperty("user.dir");
        pathImagenes = pathPrincipal+"\\imagenes_incidencias";

    }
    
    //      APP ESCRITORIO
   
    public synchronized String logAdmin(String correo, String contrasena){
        String resultado = null;
        
        try {
            Class.forName(driver);  
            Connection connection = (Connection) DriverManager.getConnection(url,user,password);
            
            String consulta="SELECT nombre, apellido, tipoUsuario FROM usuario WHERE correo=? AND contrasena=?";
            PreparedStatement psConsulta = (PreparedStatement) connection.prepareStatement(consulta);
            psConsulta.setString(1,correo);
            psConsulta.setString(2,contrasena);
            ResultSet result = psConsulta.executeQuery();
          
            if(result.next()){  
                resultado = Integer.toString(result.getInt(3))+"||"+result.getString(1)+" "+result.getString(2)+"||";
            }
            
            connection.close();
            psConsulta.close();
            result.close();
            
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return resultado;
    }
    
    
    //      APP ANDROID

    public synchronized int logUsuario(String correo, String contrasena, boolean sesionIniciada){
        int tipoUsuario = 0;
        try {
            Class.forName(driver);  
            Connection connection = (Connection) DriverManager.getConnection(url,user,password);
            
            String consulta="SELECT correo, contrasena, nombre, apellido, tipoUsuario FROM usuario WHERE correo=? AND contrasena=?";
            PreparedStatement psConsulta = (PreparedStatement) connection.prepareStatement(consulta);
            psConsulta.setString(1,correo);
            psConsulta.setString(2,contrasena);
            ResultSet result = psConsulta.executeQuery();
          
            //comprobamos si hay algun usuario con ese correo
            //si es así iniciamos sesion
            //si no es así informamos que no es correcto el correo/contrasena
            if(result.next()){               
                //comprobamos si es la primera vez que el usuario inicia sesion
                //si es asi, registramos el correo con valor 1 
                if(!usuariosLog.containsKey(correo)){
                    usuariosLog.put(correo, 1);        
                }else{
                //si ya ha iniciado sesion en otro dispositivo, iremos sumando tantas veces lo haga
                //cuando el valor de sesionInciada es true significa que ya habia iniciado sesion con ese dispositivo
                //asi llevaremos un control del usuario y el numero de veces que ha inicado sesion en otro dispositivo
                    if(!sesionIniciada){
                        int n_sesiones_iniciadas = usuariosLog.get(correo);
                        n_sesiones_iniciadas++;
                        usuariosLog.put(correo,n_sesiones_iniciadas);
                    }
                }

                tipoUsuario = result.getInt(5);

                connection.close();
                psConsulta.close();
                result.close();
                
                return tipoUsuario;
            }
            
            connection.close();
            psConsulta.close();
            result.close();
            
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return tipoUsuario;
    }
    
    
    //      COMPARTIDO POR AMBAS APPS
    
    public synchronized boolean registrarUsuario(String correo, String contrasena, String nombre, String apellido, String dni, int tlf, String departamento, int tipoUsu){
        boolean registrarUsuario = false;
        try {
            
            Class.forName(driver);
            Connection connection = (Connection) DriverManager.getConnection(url,user,password);
            
            String consulta="SELECT correo, contrasena FROM usuario WHERE correo=?";
            PreparedStatement psConsulta = (PreparedStatement) connection.prepareStatement(consulta);

            psConsulta.setString(1,correo);
            ResultSet result = psConsulta.executeQuery();            
            //comprobamos si hay algun usuario con ese correo
            //si es así informamos que ya existe alguien con ese correo
            //si no es así lo registraremos en la base de datos
            if(!result.next()){             
                                
                String insertar="INSERT INTO `usuario`(`correo`, `contrasena`, `nombre`, `apellido`, `dni`, `tlf`, `idDepartamento`, `tipoUsuario`) VALUES (?,?,?,?,?,?,?,?)";
                PreparedStatement psInsertar = (PreparedStatement) connection.prepareStatement(insertar);
                psInsertar.setString(1,correo);
                psInsertar.setString(2,contrasena);
                psInsertar.setString(3,nombre);
                psInsertar.setString(4,apellido);
                psInsertar.setString(5,dni);
                psInsertar.setInt(6,tlf);
                
                if(departamento.equals("NULL")){
                     psInsertar.setNull(7, java.sql.Types.NULL);
                }else{
                    psInsertar.setInt(7, Integer.parseInt(departamento));
                }
                
                psInsertar.setInt(8, tipoUsu);
                long n = psInsertar.executeUpdate();
               
                if(n>0){
                    System.out.println("Usuario registrado.");
                    registrarUsuario = true;
                }else{
                    System.out.println("Usuario no registrado.");
                }

                psInsertar.close();
                
            }
            
            psConsulta.close();
            result.close();
            connection.close();
            
            
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
        return registrarUsuario;   
    }    
}