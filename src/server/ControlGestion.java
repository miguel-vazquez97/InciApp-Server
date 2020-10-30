package server;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import sun.misc.BASE64Encoder;


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
    
    private Date myDate;
    private String fecha;
    private final String pathPrincipal;
    private final String pathImagenes;
    
    ControlGestion(){
        pathPrincipal = System.getProperty("user.dir");
        pathImagenes = pathPrincipal+"\\imagenes_incidencias";

    }
    
    //      APP ESCRITORIO
   
    public synchronized String logAdmin(String correo, String contrasena){
        String resultado = null;
        
        if(usuariosAdminLog.contains(correo)){
            resultado="sesionIniciadaEnOtroDispositivo||";
            return resultado;
        }
        
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
                usuariosAdminLog.add(correo);
            }
            
            connection.close();
            psConsulta.close();
            result.close();
            
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return resultado;
    }
    
    public void logOutAdmin(String correo){   
        
        if(usuariosAdminLog.contains(correo))
            usuariosAdminLog.remove(correo);  
        
    }
    
    public JSONArray datosIncidenciaTabla(String correo, String tipoIncidencia){
        JSONArray ar = null;
        
        try {           
            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                String consulta=null;
                switch(tipoIncidencia){
                    //INCIDENCIAS_NUEVAS
                    case "0":
                        consulta="SELECT MAX(ei.idEstado), i.id, ei.fecha, t.nombre, i.descripcion, i.direccion\n" +
                                "FROM incidencia i LEFT JOIN estadoincidencia ei ON(i.id=ei.idIncidencia) LEFT JOIN tipoincidencia t ON(i.idTipo=t.id) LEFT JOIN estado e ON(ei.idEstado=e.id)\n" +
                                "GROUP BY(i.id)\n" +
                                "HAVING MAX(ei.idEstado) = 1;";
                        break;
                        
                        //INCIDENCIAS_EN_VALIDACION
                    case "1":
                        consulta="SELECT MAX(ei.idEstado), i.id, ei.fecha, t.nombre, i.descripcion, i.direccion\n" +
                                "FROM incidencia i LEFT JOIN estadoincidencia ei ON(i.id=ei.idIncidencia) LEFT JOIN tipoincidencia t ON(i.idTipo=t.id) LEFT JOIN estado e ON(ei.idEstado=e.id)\n" +
                                "WHERE i.usuarioAdministrador=?\n"+
                                "GROUP BY(i.id)\n" +
                                "HAVING MAX(ei.idEstado) = 2;";
                        break;    
                        
                        //INCIDENCIAS_VALIDADAS
                    case "2":
                        consulta="SELECT MAX(ei.idEstado), i.id, ei.fecha, t.nombre, i.descripcion, i.direccion\n" +
                                "FROM incidencia i LEFT JOIN estadoincidencia ei ON(i.id=ei.idIncidencia) LEFT JOIN tipoincidencia t ON(i.idTipo=t.id) LEFT JOIN estado e ON(ei.idEstado=e.id)\n" +
                                "WHERE i.usuarioAdministrador=?\n"+
                                "GROUP BY(i.id)\n" +
                                "HAVING MAX(ei.idEstado) = 3;";
                        break;
                        
                        //INCIDENCIAS_ARREGLADAS
                    case "3":
                        consulta="SELECT MAX(ei.idEstado), i.id, ei.fecha, t.nombre, i.descripcion, i.direccion\n" +
                                "FROM incidencia i LEFT JOIN estadoincidencia ei ON(i.id=ei.idIncidencia) LEFT JOIN tipoincidencia t ON(i.idTipo=t.id) LEFT JOIN estado e ON(ei.idEstado=e.id)\n" +
                                "WHERE i.usuarioAdministrador=?\n"+
                                "GROUP BY(i.id)\n" +
                                "HAVING MAX(ei.idEstado) = 6;";
                        break;
                        
                        //HISTORIAL_INCIDENCIAS
                    case "4":
                        consulta="SELECT MAX(ei.idEstado), i.id, ei.fecha, t.nombre, i.descripcion, i.direccion\n" +
                                "FROM incidencia i LEFT JOIN estadoincidencia ei ON(i.id=ei.idIncidencia) LEFT JOIN tipoincidencia t ON(i.idTipo=t.id) LEFT JOIN estado e ON(ei.idEstado=e.id)\n" +
                                "GROUP BY(i.id)\n" +
                                "HAVING MAX(ei.idEstado) > 6;";
                        break;
                }   ResultSet resultTipo;
                try (PreparedStatement psConsulta = (PreparedStatement) connection.prepareStatement(consulta)) {
                    if(tipoIncidencia.equals("1") || tipoIncidencia.equals("2") || tipoIncidencia.equals("3")){
                        psConsulta.setString(1, correo);
                    }   resultTipo = psConsulta.executeQuery();
                    int id;
                    String fechaDatosIncidenciaTabla,tipo,descripcion,ubicacion;
                    ar=new JSONArray();
                    JSONObject obj;
                    while(resultTipo.next()){
                        obj = new JSONObject();
                        id = resultTipo.getInt(2);
                        obj.put("id", id);
                        fechaDatosIncidenciaTabla = resultTipo.getString(3);
                        obj.put("fecha",fechaDatosIncidenciaTabla);
                        tipo = resultTipo.getString(4);
                        obj.put("tipo", tipo);
                        descripcion = resultTipo.getString(5);
                        obj.put("descripcion",descripcion);
                        ubicacion = resultTipo.getString(6);
                        obj.put("direccion",ubicacion);
                        ar.add(obj);
                    }
                }
                resultTipo.close();
            }
            
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return ar;
    }
    
    public JSONArray obtenerDetallesNuevaRegistrada(int id){
        JSONArray ar = new JSONArray();
        JSONObject obj;
        
        try {
            
            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                String consulta1,consulta2,consulta3;
                //  primera consulta para botener la mayoria de datos sobre la incidencia
                //  y guardarlo en el primer object del json
                
                consulta1 = "SELECT u.nombre, u.apellido, i.usuarioCiudadano, ei.fecha, ti.nombre, i.descripcion, i.direccion, d.nombre\n" +
                        "FROM incidencia i LEFT JOIN usuario u ON (i.usuarioCiudadano=u.correo) LEFT JOIN estadoincidencia ei ON (i.id=ei.idIncidencia) LEFT JOIN tipoincidencia ti ON (i.idTipo=ti.id) LEFT JOIN departamento d ON (ti.idDepartamento=d.id)\n" +
                        "WHERE i.id=? AND ei.idEstado=1";
                ResultSet resultConsulta1;
                String departamento = null;
                try (PreparedStatement psConsulta1 = (PreparedStatement) connection.prepareStatement(consulta1)) {
                    psConsulta1.setInt(1,id);
                    resultConsulta1 = psConsulta1.executeQuery();
                    String nombreApellido, correo, obtenerDetallesNuevaRegistrada, tipo, descripcion, direccion;
                    while(resultConsulta1.next()){
                        obj = new JSONObject();
                        nombreApellido = resultConsulta1.getString(1)+" "+resultConsulta1.getString(2);
                        obj.put("nombreApellido", nombreApellido);
                        correo = resultConsulta1.getString(3);
                        obj.put("correo", correo);
                        obtenerDetallesNuevaRegistrada = resultConsulta1.getString(4);
                        obj.put("fecha", obtenerDetallesNuevaRegistrada);
                        tipo = resultConsulta1.getString(5);
                        obj.put("tipo", tipo);
                        descripcion = resultConsulta1.getString(6);
                        obj.put("descripcion",descripcion);
                        direccion = resultConsulta1.getString(7);
                        obj.put("direccion",direccion);
                        departamento = resultConsulta1.getString(8);
                        obj.put("departamento",departamento);
                        ar.add(obj);
                    }
                }
                resultConsulta1.close();
                //  segunda consulta para obtener el nombre y correo de los supervisores
                //  que pertenecen al mismo departamente que está destinado a cubrir la incidencia
                
                consulta2 = "SELECT u.nombre, u.apellido, u.correo \n" +
                        "FROM departamento d LEFT JOIN usuario u ON (d.id = u.idDepartamento)\n" +
                        "WHERE d.nombre=? AND u.tipoUsuario=2";
                ResultSet resultConsulta2;
                try (PreparedStatement psConsulta2 = (PreparedStatement) connection.prepareStatement(consulta2)) {
                    psConsulta2.setString(1,departamento);
                    resultConsulta2 = psConsulta2.executeQuery();
                    String nombreApellidoS, correoS, supervisores = "";
                    while(resultConsulta2.next()){
                        nombreApellidoS = resultConsulta2.getString(1)+" "+resultConsulta2.getString(2);
                        correoS = resultConsulta2.getString(3);
                        
                        supervisores += nombreApellidoS+":"+correoS+";";
                    }   obj = new JSONObject();
                    obj.put("supervisores", supervisores);
                    ar.add(obj);
                }
                resultConsulta2.close();
                //  tercera consulta para obtener la imagen de la incidencia
                
                consulta3 = "SELECT ii.imagen \n" +
                        "FROM incidencia i LEFT JOIN estadoincidencia ei ON (i.id = ei.idIncidencia) LEFT JOIN imagen ii ON (ei.codImagen = ii.id)\n" +
                        "WHERE i.id=? AND ei.idEstado=1";
                ResultSet resultConsulta3;
                try (PreparedStatement psConsulta3 = (PreparedStatement) connection.prepareStatement(consulta3)) {
                    psConsulta3.setInt(1,id);
                    resultConsulta3 = psConsulta3.executeQuery();
                    File file;
                    String imagen, base64;
                    while(resultConsulta3.next()){
                        obj = new JSONObject();
                        imagen = resultConsulta3.getString(1);
                        file = new File(pathImagenes+"\\"+imagen);
                        if(file.exists()){
                            
                            BufferedImage img = ImageIO.read(file);
                            //BufferedImage image = resize(img, 390, 450);
                            BufferedImage image = resize(img, 400, 500);
                            
                            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                                ImageIO.write(image, "jpg", bos);
                                byte[] imageBytes = bos.toByteArray();
                                
                                BASE64Encoder encoder = new BASE64Encoder();
                                base64 = encoder.encode(imageBytes);
                            }
                            
                            obj.put("imagen",base64);
                            ar.add(obj);
                        }
                        
                    }
                }
                resultConsulta3.close();
            } catch (IOException ex) {
                Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return ar;
    }
    
    public synchronized boolean asignarIncidenciaSupervisor(int idIncidencia, String correoAdmin, String correoSupervisor){
        boolean asignarSupervisor = false;
        try {
            Class.forName(driver);  
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                String consulta="SELECT `id` \n" +
                        "FROM `estadoincidencia` \n" +
                        "WHERE idIncidencia=? AND idEstado=2";
                try (PreparedStatement psConsulta = (PreparedStatement) connection.prepareStatement(consulta)) {
                    psConsulta.setInt(1,idIncidencia);
                    //comprobamos que aún no ha sido asignado ningun supervisor a dicha incidencia
                    try (ResultSet result = psConsulta.executeQuery()) {
                        //comprobamos que aún no ha sido asignado ningun supervisor a dicha incidencia
                        if(!result.next()){
                            
                            //obtenemos el codigo de la imagen de la incidencia
                            String consultaCodImagen="SELECT `codImagen` \n" +
                                    "FROM `estadoincidencia` \n" +
                                    "WHERE idIncidencia=? AND idEstado=1";
                            int codImagen;
                            try (PreparedStatement psConsultaCodImagen = (PreparedStatement) connection.prepareStatement(consultaCodImagen)) {
                                psConsultaCodImagen.setInt(1,idIncidencia);
                                try (ResultSet resultConsultaCodImagen = psConsultaCodImagen.executeQuery()) {
                                    codImagen = 0;
                                    if(resultConsultaCodImagen.next()){
                                        codImagen = resultConsultaCodImagen.getInt(1);
                                    }
                                }
                            }
                            
                            //comenzamos actualizando la incidencia donde dejaremos reflejado quien es el administrador y el supervisor encargados de ella
                            String update = "UPDATE `incidencia` \n" +
                                    "SET `usuarioAdministrador`=?,`usuarioSupervisor`=? \n" +
                                    "WHERE id=?";
                            try (PreparedStatement psUpdate = (PreparedStatement) connection.prepareStatement(update)) {
                                psUpdate.setString(1, correoAdmin);
                                psUpdate.setString(2, correoSupervisor);
                                psUpdate.setInt(3, idIncidencia);
                                long nUpdate = psUpdate.executeUpdate();
                                
                                if(nUpdate>0){
                                    //una vez actualizada la incidencia procedemos a insertar el nuevo estado de la incidencia
                                    String insert = "INSERT INTO `estadoincidencia`(`id`, `fecha`, `descripcion`, `idIncidencia`, `idEstado`, `codImagen`) VALUES (NULL,?,NULL,?,2,?)";
                                    
                                    myDate = new Date();
                                    fecha = new SimpleDateFormat("yyyy-MM-dd").format(myDate);
                                    
                                    try (PreparedStatement psInsert = (PreparedStatement) connection.prepareStatement(insert)) {
                                        psInsert.setString(1, fecha);
                                        psInsert.setInt(2, idIncidencia);
                                        psInsert.setInt(3, codImagen);
                                        long nInsert = psInsert.executeUpdate();
                                        
                                        if(nInsert>0){
                                            asignarSupervisor=true;
                                        }
                                    }
                                    
                                }
                            }
                            
                        }
                    }
                }
            }
            
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return asignarSupervisor;
    }
    
    public JSONObject obtenerEmpleados(int idIncidencia) {
        JSONObject obj = null;
        
        try {
            
            Class.forName(driver);
            Connection connection = (Connection) DriverManager.getConnection(url,user,password);
            
            String consulta= "SELECT u.nombre, u.apellido, u.correo \n" +
                        "FROM incidencia i LEFT JOIN tipoincidencia tp ON (i.idTipo=tp.id) LEFT JOIN departamento d ON (tp.idDepartamento=d.id) LEFT JOIN usuario u ON (d.id=u.idDepartamento)\n" +
                        "where i.id=? AND u.tipoUsuario=3";

            PreparedStatement psConsulta = (PreparedStatement) connection.prepareStatement(consulta);
            psConsulta.setInt(1,idIncidencia);
            ResultSet resultConsulta = psConsulta.executeQuery();
            
            String nombreApellido, correo, empleados = "";
            

            while(resultConsulta.next()){                
                nombreApellido = resultConsulta.getString(1)+" "+resultConsulta.getString(2);
                correo = resultConsulta.getString(3);

                empleados += nombreApellido+":"+correo+";";
                
                if(resultConsulta.isLast()){
                    obj = new JSONObject();
                    obj.put("empleados", empleados);  
                }
            }  

            psConsulta.close();
            resultConsulta.close();
            connection.close();
            
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return obj;
    }
    
    public synchronized boolean asignarIncidenciaEmpleado(int idIncidencia, String correoEmpleado){
        boolean asignarEmpleado = false;
        try {
            Class.forName(driver);  
            //obtenemos el codigo de la imagen de la incidencia
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                //obtenemos el codigo de la imagen de la incidencia
                String consultaCodImagen="SELECT `codImagen` \n" +
                        "FROM `estadoincidencia` \n" +
                        "WHERE idIncidencia=? AND idEstado=1";
                PreparedStatement psConsultaCodImagen = (PreparedStatement) connection.prepareStatement(consultaCodImagen);
                psConsultaCodImagen.setInt(1,idIncidencia);
                ResultSet resultConsultaCodImagen = psConsultaCodImagen.executeQuery();
                int codImagen = 0;
                if(resultConsultaCodImagen.next()){
                    codImagen = resultConsultaCodImagen.getInt(1);
                }
                
                resultConsultaCodImagen.close();
                psConsultaCodImagen.close();
                
                //comenzamos actualizando la incidencia donde dejaremos reflejado quien es el empleado encargados de ella
                String update = "UPDATE `incidencia` \n" +
                        "SET `usuarioEmpleado`=? \n" +
                        "WHERE id=?";
                PreparedStatement psUpdate = (PreparedStatement) connection.prepareStatement(update);
                psUpdate.setString(1, correoEmpleado);
                psUpdate.setInt(2, idIncidencia);
                long nUpdate = psUpdate.executeUpdate();
                
                if(nUpdate>0){
                    //una vez actualizada la incidencia procedemos a insertar el nuevo estado de la incidencia
                    String insert = "INSERT INTO `estadoincidencia`(`id`, `fecha`, `descripcion`, `idIncidencia`, `idEstado`, `codImagen`) VALUES (NULL,?,NULL,?,4,?)";
                    
                    myDate = new Date();
                    fecha = new SimpleDateFormat("yyyy-MM-dd").format(myDate);
                    
                    PreparedStatement psInsert = (PreparedStatement) connection.prepareStatement(insert);
                    psInsert.setString(1, fecha);
                    psInsert.setInt(2, idIncidencia);
                    psInsert.setInt(3, codImagen);
                    long nInsert = psInsert.executeUpdate();
                    
                    if(nInsert>0){
                        asignarEmpleado=true;
                    }
                    psInsert.close();
                    
                }
                psUpdate.close();
            }
            
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return asignarEmpleado;    
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
            
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return tipoUsuario;
    }
    
    public void logOutUsuario(String correo){                   
        int n_sesiones_iniciadas = usuariosLog.get(correo);
        n_sesiones_iniciadas--;
        
        if(n_sesiones_iniciadas==0){
            usuariosLog.remove(correo);
        }else{
            usuariosLog.put(correo,n_sesiones_iniciadas);
        } 
        
    }
    
    public synchronized String nuevaIncidencia(String ubicacion, String direccion, String descripcion, String tipo, String correoUsuario, String imagen){
        String respuesta = null;
        int idIncidencia = 0,idTipoIncidencia,idImagenIncidencia = 0;
        String[] ubi = ubicacion.split(";");
        
        try {
            Class.forName(driver);            
            Connection connection = (Connection) DriverManager.getConnection(url,user,password);
            //COMPROBAMOS SI EXISTE LA INCIDENCIA
            String consulta="SELECT i.id FROM incidencia i, tipoincidencia t WHERE i.idTipo=t.id AND T.nombre = ? "
                    + "AND CAST(SUBSTRING_INDEX(i.ubicacion,';',1) AS DECIMAL(8,6)) BETWEEN ? AND ?"
                    + "AND CAST(SUBSTRING_INDEX(i.ubicacion,';',-1) AS DECIMAL(10,10)) BETWEEN ? AND ?";
            PreparedStatement psConsulta = (PreparedStatement) connection.prepareStatement(consulta);

            psConsulta.setString(1,tipo);
            psConsulta.setDouble(2, (Double.parseDouble(ubi[0])-10));
            psConsulta.setDouble(3, (Double.parseDouble(ubi[0])+10));
            psConsulta.setDouble(4, (Double.parseDouble(ubi[1])-10));
            psConsulta.setDouble(5, (Double.parseDouble(ubi[1])+10));
            ResultSet result = psConsulta.executeQuery();            
            //si hay alguna incidencia con la misma ubicacion en un radio de 10m y que no sea ni nueva/arreglada
            //si es así, significa que estamos hablando de la misma incidencia y no habrá que registrarla
            if(result.next()){             
                respuesta = "YaExiste";
                
                psConsulta.close();
                result.close();
                connection.close();
                
                return respuesta;
            }else{
                psConsulta.close();
                result.close();
            }  
            
            //OBTENEMOS ID DEL TIPO DE INCIDENCIA
            
            String insertTipo = "SELECT id FROM `tipoincidencia` WHERE nombre = ?";
            PreparedStatement psConsultaTipo = (PreparedStatement) connection.prepareStatement(insertTipo);
            psConsultaTipo.setString(1,tipo);
            ResultSet resultTipo = psConsultaTipo.executeQuery();
            if(resultTipo.next()){
                idTipoIncidencia = resultTipo.getInt(1);
                
                psConsultaTipo.close();
                resultTipo.close();
            }else{
                respuesta = "NoOk";
                
                psConsultaTipo.close();
                resultTipo.close();
                connection.close();
                
                return respuesta;
            }
                    
            
            //INSERTAMOS LA IMAGEN VINCULADA A LA INCIDENCIA
            String insertImagen = "INSERT INTO `imagen`(`id`, `imagen`) VALUES (NULL,?)";
            PreparedStatement psInsertarImagen = (PreparedStatement) connection.prepareStatement(insertImagen);
            psInsertarImagen.setString(1,imagen);
            long nImagen = psInsertarImagen.executeUpdate();
            
            if(nImagen>0){
                
                //OBTENEMOS EL ID DE DICHA IMAGEN
                String selectImagen = "SELECT id FROM `imagen` WHERE `imagen` = ?";
                ResultSet resultImagen;
                try (PreparedStatement psConsultaImagen = (PreparedStatement) connection.prepareStatement(selectImagen)) {
                    psConsultaImagen.setString(1,imagen);
                    resultImagen = psConsultaImagen.executeQuery();
                    if(resultImagen.next()){
                        idImagenIncidencia = resultImagen.getInt(1);
                    }   psInsertarImagen.close();
                }
                resultImagen.close();     
            }else{
                respuesta = "NoOk"; 
                psInsertarImagen.close();
                connection.close();
                return respuesta;
            }
            
            //REGISTRAMOS LA INCIDENCIA
            String insertIncidencia = "INSERT INTO `incidencia`(`id`, `ubicacion`, `direccion`, `descripcion`, `idTipo`, `usuarioCiudadano`, `usuarioAdministrador`, `usuarioSupervisor`, `usuarioEmpleado`) VALUES (NULL,?,?,?,?,?,NULL,NULL,NULL)";
            PreparedStatement psInsertarIncidencia = (PreparedStatement) connection.prepareStatement(insertIncidencia);
            psInsertarIncidencia.setString(1,ubicacion);
            psInsertarIncidencia.setString(2,direccion);
            psInsertarIncidencia.setString(3,descripcion);
            psInsertarIncidencia.setInt(4,idTipoIncidencia);
            psInsertarIncidencia.setString(5,correoUsuario);   
            
            long nIncidencia = psInsertarIncidencia.executeUpdate();
            if(nIncidencia>0){
                
                //OBTENEMOS ID DE LA INCIDENCIA
                String selectIncidencia = "SELECT `id` FROM `incidencia` WHERE `ubicacion` = ? AND `descripcion` = ? AND `idTipo` = ? AND `usuarioCiudadano` = ?";
                ResultSet resultIncidencia;
                try (PreparedStatement psConsultaIncidencia = (PreparedStatement) connection.prepareStatement(selectIncidencia)) {
                    psConsultaIncidencia.setString(1,ubicacion);
                    psConsultaIncidencia.setString(2,descripcion);
                    psConsultaIncidencia.setInt(3,idTipoIncidencia);
                    psConsultaIncidencia.setString(4,correoUsuario);
                    resultIncidencia = psConsultaIncidencia.executeQuery();
                    if(resultIncidencia.next()){
                        idIncidencia = resultIncidencia.getInt(1);
                    }   psInsertarIncidencia.close();
                }
                resultIncidencia.close();    
            }else{                
                respuesta = "NoOk";
                psInsertarIncidencia.close();
                connection.close();
                return respuesta;
            }
            
            
            //REGISTRAMOS ESTADO INCIDENCIA
            //idEstado es 1 porque su estado inicial es NuevaRegistrada
            String insertEstadoIncidencia = "INSERT INTO `estadoincidencia`(`id`, `fecha`, `descripcion`, `idIncidencia`, `idEstado`, `codImagen`) VALUES (NULL,?,NULL,?,1,?)";
            PreparedStatement psInsertarEstadoIncidencia = (PreparedStatement) connection.prepareStatement(insertEstadoIncidencia);
            
            //Obtenemos la fechaDatosIncidenciaTabla
            myDate = new Date();
            fecha = new SimpleDateFormat("yyyy-MM-dd").format(myDate);
            
            psInsertarEstadoIncidencia.setString(1,fecha);
            psInsertarEstadoIncidencia.setInt(2,idIncidencia);
            psInsertarEstadoIncidencia.setInt(3,idImagenIncidencia);
            
            long nEstadoIncidencia = psInsertarEstadoIncidencia.executeUpdate();
            if(nEstadoIncidencia>0){
                respuesta = "Ok";
                psInsertarEstadoIncidencia.close();
                connection.close();
            }else{
                respuesta = "NoOk";
                psInsertarEstadoIncidencia.close();
                connection.close();
                return respuesta;
            }
            
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return respuesta;
    
    }

    //      COMPARTIDO POR AMBAS APPS
    
    public synchronized boolean registrarUsuario(String correo, String contrasena, String nombre, String apellido, String dni, int tlf, String departamento, int tipoUsu){
        boolean registrarUsuario = false;
        try {
            
            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                String consulta="SELECT correo, contrasena FROM usuario WHERE correo=?";
                ResultSet result;
                try (PreparedStatement psConsulta = (PreparedStatement) connection.prepareStatement(consulta)) {
                    psConsulta.setString(1,correo);
                    result = psConsulta.executeQuery();
                    //comprobamos si hay algun usuario con ese correo
                    //si es así informamos que ya existe alguien con ese correo
                    //si no es así lo registraremos en la base de datos
                    if(!result.next()){
                        
                        String insertar="INSERT INTO `usuario`(`correo`, `contrasena`, `nombre`, `apellido`, `dni`, `tlf`, `idDepartamento`, `tipoUsuario`) VALUES (?,?,?,?,?,?,?,?)";
                        try (PreparedStatement psInsertar = (PreparedStatement) connection.prepareStatement(insertar)) {
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
                        }
                        
                    }
                }
                result.close();
            }
            
            
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
        return registrarUsuario;   
    }    


    //      METODO REDIMENSIONAR IMAGEN 
        
    public static BufferedImage resize(BufferedImage bufferedImage, int newW, int newH) {

        int w = bufferedImage.getWidth();
        int h = bufferedImage.getHeight();
        BufferedImage bufim = new BufferedImage(newW, newH, bufferedImage.getType());
        Graphics2D g = bufim.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(bufferedImage, 0, 0, newW, newH, 0, 0, w, h, null);
        g.dispose();

        return bufim;
    }
}