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
import java.util.Base64;
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
    static String mysql_jdbd_driver = "com.mysql.jdbc.Driver";
    static String prefix = "jdbc:" + "mysql:";
    static String hostName = "//localhost:3306/";
    static String urlFolder = "";
    static String dbName = "inciappdatabase";

    static String url = prefix + hostName + urlFolder + dbName;

    static String driver = mysql_jdbd_driver;
    static String user = "root";
    static String password = "";

    private Date myDate;
    private String fecha;
    private final String pathPrincipal;
    private final String pathImagenes;
    
    BASE64Encoder encoder;

    ControlGestion() {
        pathPrincipal = System.getProperty("user.dir");
        pathImagenes = pathPrincipal + "\\imagenes_incidencias";

    }

    //      APP ESCRITORIO
    public synchronized String logAdmin(String correo, String contrasena) {
        String resultado = null;

        if (usuariosAdminLog.contains(correo)) {
            resultado = "sesionIniciadaEnOtroDispositivo||";
            return resultado;
        }

        try {
            Class.forName(driver);
            PreparedStatement psConsulta;
            ResultSet result;
            try (Connection connection = (Connection) DriverManager.getConnection(url, user, password)) {
                String consulta = "SELECT nombre, apellido, tipoUsuario FROM usuario WHERE correo=? AND contrasena=? AND activo=true";
                psConsulta = (PreparedStatement) connection.prepareStatement(consulta);
                psConsulta.setString(1, correo);
                psConsulta.setString(2, contrasena);
                result = psConsulta.executeQuery();
                if (result.next()) {
                    resultado = Integer.toString(result.getInt(3)) + "||" + result.getString(1) + " " + result.getString(2) + "||";
                    usuariosAdminLog.add(correo);
                }
                psConsulta.close();
                result.close();
                connection.close();
            }            
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }

        return resultado;
    }

    public void logOutAdmin(String correo) {

        if (usuariosAdminLog.contains(correo)) {
            usuariosAdminLog.remove(correo);
        }

    }

    public JSONArray datosIncidenciaTabla(String correo, String tipoIncidencia) {
        JSONArray ar = null;

        try {
            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url, user, password)) {
                String consulta = null;
                switch (tipoIncidencia) {
                    //INCIDENCIAS
                    case "0":
                        consulta = "SELECT MAX(ei.idEstado) as \"idEstado\", i.id, (SELECT titulo FROM estado WHERE id=MAX(ei.idEstado)) as \"estado\", ei.fecha, t.nombre, i.descripcion, i.direccion, i.usuarioAdministrador\n" +
                            "FROM incidencia i LEFT JOIN estadoincidencia ei ON(i.id=ei.idIncidencia) LEFT JOIN tipoincidencia t ON(i.idTipo=t.id) LEFT JOIN estado e ON(ei.idEstado=e.id) LEFT JOIN usuario u ON(i.usuarioAdministrador=u.correo)\n" +
                            "GROUP BY(i.id)\n" +
                            "HAVING MAX(ei.idEstado)>1 AND i.usuarioAdministrador=?\n" +
                            "OR MAX(ei.idEstado)=1\n" +
                            "ORDER BY i.id";
                        break;

                    //HISTORIAL_INCIDENCIAS
                    case "1":
                        consulta = "SELECT MAX(ei.idEstado) as \"idEstado\", i.id, (SELECT titulo FROM estado WHERE id=MAX(ei.idEstado)) as \"estado\", ei.fecha, t.nombre, i.descripcion, i.direccion, i.usuarioAdministrador\n" +
                            "FROM incidencia i LEFT JOIN estadoincidencia ei ON(i.id=ei.idIncidencia) LEFT JOIN tipoincidencia t ON(i.idTipo=t.id) LEFT JOIN estado e ON(ei.idEstado=e.id) LEFT JOIN usuario u ON(i.usuarioAdministrador=u.correo)\n" +
                            "GROUP BY(i.id)\n" +
                            "ORDER BY i.id";
                        break;

                }
                ResultSet resultConsultaIncidencias;
                try (PreparedStatement psConsultaIncidencias = (PreparedStatement) connection.prepareStatement(consulta)) {
                    if (tipoIncidencia.equals("0")) {
                        psConsultaIncidencias.setString(1, correo);
                    }
                    
                    resultConsultaIncidencias = psConsultaIncidencias.executeQuery();
                    int id;
                    String fechaDatosIncidenciaTabla, estado, tipo, descripcion, ubicacion;
                    ar = new JSONArray();
                    JSONObject obj;
                    while (resultConsultaIncidencias.next()) {
                        obj = new JSONObject();
                        id = resultConsultaIncidencias.getInt(2);
                        obj.put("id", id);
                        estado = resultConsultaIncidencias.getString(3);
                        obj.put("estado", estado);
                        fechaDatosIncidenciaTabla = resultConsultaIncidencias.getString(4);
                        obj.put("fecha", fechaDatosIncidenciaTabla);
                        tipo = resultConsultaIncidencias.getString(5);
                        obj.put("tipo", tipo);
                        descripcion = resultConsultaIncidencias.getString(6);
                        obj.put("descripcion", descripcion);
                        ubicacion = resultConsultaIncidencias.getString(7);
                        obj.put("direccion", ubicacion);
                        ar.add(obj);
                    }
                }
                resultConsultaIncidencias.close();
                connection.close();
            }        
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ar;
    }

    public JSONArray obtenerDetallesIncidenciaEstado(int idIncidencia, String estado) {
        JSONArray ar = new JSONArray();
        JSONObject obj;

        try {

            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url, user, password)) {
                String consulta1, consulta2, consulta3;
                //  primera consulta para botener la mayoria de datos sobre la incidencia
                //  y guardarlo en el primer object del json
                
                consulta1 = "SELECT u.nombre, u.apellido, i.usuarioCiudadano, ei.fecha, ti.nombre, i.descripcion, i.direccion, d.nombre\n"
                        + "FROM incidencia i LEFT JOIN usuario u ON (i.usuarioCiudadano=u.correo) LEFT JOIN estadoincidencia ei ON (i.id=ei.idIncidencia) LEFT JOIN tipoincidencia ti ON (i.idTipo=ti.id) LEFT JOIN departamento d ON (ti.idDepartamento=d.id)\n"
                        + "WHERE i.id=? AND ei.idEstado=1";
                ResultSet resultConsulta1;
                String departamento = null;
                try (PreparedStatement psConsulta1 = (PreparedStatement) connection.prepareStatement(consulta1)) {
                    psConsulta1.setInt(1, idIncidencia);
                    resultConsulta1 = psConsulta1.executeQuery();
                    String nombreApellido, correo, obtenerDetallesNuevaRegistrada, tipo, descripcion, direccion;
                    while (resultConsulta1.next()) {
                        obj = new JSONObject();
                        nombreApellido = resultConsulta1.getString(1) + " " + resultConsulta1.getString(2);
                        obj.put("nombreApellido", nombreApellido);
                        correo = resultConsulta1.getString(3);
                        obj.put("correo", correo);
                        obtenerDetallesNuevaRegistrada = resultConsulta1.getString(4);
                        obj.put("fecha", obtenerDetallesNuevaRegistrada);
                        tipo = resultConsulta1.getString(5);
                        obj.put("tipo", tipo);
                        descripcion = resultConsulta1.getString(6);
                        obj.put("descripcion", descripcion);
                        direccion = resultConsulta1.getString(7);
                        obj.put("direccion", direccion);
                        departamento = resultConsulta1.getString(8);
                        obj.put("departamento", departamento);
                        ar.add(obj);
                    }
                }
                resultConsulta1.close();
                
                //  segunda consulta para obtener el nombre y correo de los supervisores
                //  que pertenecen al mismo departamente que está destinado a cubrir la incidencia
                PreparedStatement psConsulta2;
                ResultSet resultConsulta2;
                obj = new JSONObject();
                String nombre_supervisor = null, nombre_empleado = null;
                switch(estado){                    
                    case "NuevaRegistrada":
                        consulta2 = "SELECT u.nombre, u.apellido, u.correo \n"
                                + "FROM departamento d LEFT JOIN usuario u ON (d.id = u.idDepartamento)\n"
                                + "WHERE d.nombre=? AND u.tipoUsuario=2 AND u.activo=true";
                        
                        boolean listaSupervisoresOk = false;
                        psConsulta2 = (PreparedStatement) connection.prepareStatement(consulta2);
                        psConsulta2.setString(1, departamento);
                        resultConsulta2 = psConsulta2.executeQuery();
                        String nombreApellidoS, correoS, supervisores = "";
                        while (resultConsulta2.next()) {

                            nombreApellidoS = resultConsulta2.getString(1) + " " + resultConsulta2.getString(2);
                            correoS = resultConsulta2.getString(3);

                            supervisores += nombreApellidoS + ":" + correoS + ";";

                            if (resultConsulta2.isLast()){
                                listaSupervisoresOk = true;
                            }

                        }

                        if(!listaSupervisoresOk){
                            supervisores = "null";
                        }

                        obj.put("supervisores", supervisores);
                        ar.add(obj);
                        
                        resultConsulta2.close();
                        break;
                        
                    case "Validada":
                        consulta2 = "SELECT u.nombre, u.apellido, u.correo \n"
                                + "FROM departamento d LEFT JOIN usuario u ON (d.id = u.idDepartamento)\n"
                                + "WHERE d.nombre=? AND u.tipoUsuario=3 AND u.activo=true";
                        
                        boolean listaEmpleadosOk = false;
                        psConsulta2 = (PreparedStatement) connection.prepareStatement(consulta2);
                        psConsulta2.setString(1, departamento);
                        resultConsulta2 = psConsulta2.executeQuery();
                        String nombreApellidoE, correoE, empleados = "";
                        while (resultConsulta2.next()) {
                            System.out.println("entra");
                            nombreApellidoE = resultConsulta2.getString(1) + " " + resultConsulta2.getString(2);
                            correoE = resultConsulta2.getString(3);

                            empleados += nombreApellidoE + ":" + correoE + ";";

                            if (resultConsulta2.isLast()){
                                listaEmpleadosOk = true;
                            }

                        }

                        if(!listaEmpleadosOk){
                            empleados = "null";
                        }

                        obj.put("empleados", empleados);                      
                        resultConsulta2.close();
                     // no ponemos break para que pase al siguiente case y así obtener el supervisor asignado a la incidencia   
                    case "EnTramite":
                        consulta2 = "SELECT u.nombre, u.apellido \n" +
                                "FROM usuario u LEFT JOIN incidencia i ON (u.correo=i.usuarioSupervisor)\n" +
                                "WHERE i.id=?;";
                        psConsulta2 = (PreparedStatement) connection.prepareStatement(consulta2);
                        psConsulta2.setInt(1, idIncidencia);
                        resultConsulta2 = psConsulta2.executeQuery();                        
                        while (resultConsulta2.next()) {
                            nombre_supervisor = resultConsulta2.getString(1) + " " + resultConsulta2.getString(2);
                        }

                        obj.put("supervisores", nombre_supervisor);
                        ar.add(obj);
                        
                        resultConsulta2.close();
                        break;
                       
                        
                    case "EnArreglo":
                    case "ValidarArreglo":
                    case "Arreglada":
                        consulta2 = "SELECT u.nombre, u.apellido, u.tipoUsuario\n" +
                                        "FROM usuario u LEFT JOIN incidencia i ON (u.correo=i.usuarioSupervisor) OR (u.correo = i.usuarioEmpleado)\n" +
                                        "WHERE i.id=?";
                        
                        psConsulta2 = (PreparedStatement) connection.prepareStatement(consulta2);
                        psConsulta2.setInt(1, idIncidencia);
                        resultConsulta2 = psConsulta2.executeQuery();                        
                        while (resultConsulta2.next()) {
                            //comprobamos si el tipoUsuario es 2, significa que el nombre pertenece al del supervisor
                            if(resultConsulta2.getInt(3)==2){
                                nombre_supervisor = resultConsulta2.getString(1) + " " + resultConsulta2.getString(2);
                            }else{
                                nombre_empleado = resultConsulta2.getString(1) + " " + resultConsulta2.getString(2);
                            }  
                        }

                        obj.put("supervisores", nombre_supervisor);
                        obj.put("empleados", nombre_empleado);
                        ar.add(obj);
                        
                        resultConsulta2.close();
                        break;
                }
                
                //  tercera consulta para obtener la imagen de la incidencia

                consulta3 = "SELECT ii.imagen \n"
                        + "FROM incidencia i LEFT JOIN estadoincidencia ei ON (i.id = ei.idIncidencia) LEFT JOIN imagen ii ON (ei.codImagen = ii.id)\n"
                        + "WHERE i.id=? AND ei.idEstado=1";
                ResultSet resultConsulta3;
                try (PreparedStatement psConsulta3 = (PreparedStatement) connection.prepareStatement(consulta3)) {
                    psConsulta3.setInt(1, idIncidencia);
                    resultConsulta3 = psConsulta3.executeQuery();
                    File file;
                    String imagen, base64;
                    while (resultConsulta3.next()) {
                        obj = new JSONObject();
                        imagen = resultConsulta3.getString(1);
                        file = new File(pathImagenes + "\\" + imagen);
                        if (file.exists()) {

                            BufferedImage img = ImageIO.read(file);
                            //BufferedImage image = resize(img, 390, 450);
                            BufferedImage image = resize(img, 400, 500);

                            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                                ImageIO.write(image, "jpg", bos);
                                byte[] imageBytes = bos.toByteArray();

                                encoder = new BASE64Encoder();
                                base64 = encoder.encode(imageBytes);
                            }

                            obj.put("imagen", base64);
                            ar.add(obj);
                        }

                    }
                }
                resultConsulta3.close();
                connection.close();
            } catch (IOException ex) {
                Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return ar;
    }

    public synchronized boolean asignarIncidenciaSupervisor(int idIncidencia, String correoAdmin, String correoSupervisor) {
        boolean asignarSupervisor = false;
        try {
            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url, user, password)) {
                String consulta = "SELECT `id` \n"
                        + "FROM `estadoincidencia` \n"
                        + "WHERE idIncidencia=? AND idEstado=2";
                try (PreparedStatement psConsulta = (PreparedStatement) connection.prepareStatement(consulta)) {
                    psConsulta.setInt(1, idIncidencia);
                    //comprobamos que aún no ha sido asignado ningun supervisor a dicha incidencia
                    try (ResultSet result = psConsulta.executeQuery()) {
                        //comprobamos que aún no ha sido asignado ningun supervisor a dicha incidencia
                        if (!result.next()) {

                            //obtenemos el codigo de la imagen de la incidencia
                            String consultaCodImagen = "SELECT `codImagen` \n"
                                    + "FROM `estadoincidencia` \n"
                                    + "WHERE idIncidencia=? AND idEstado=1";
                            int codImagen;
                            try (PreparedStatement psConsultaCodImagen = (PreparedStatement) connection.prepareStatement(consultaCodImagen)) {
                                psConsultaCodImagen.setInt(1, idIncidencia);
                                try (ResultSet resultConsultaCodImagen = psConsultaCodImagen.executeQuery()) {
                                    codImagen = 0;
                                    if (resultConsultaCodImagen.next()) {
                                        codImagen = resultConsultaCodImagen.getInt(1);
                                    }
                                }
                            }

                            //comenzamos actualizando la incidencia donde dejaremos reflejado quien es el administrador y el supervisor encargados de ella
                            String update = "UPDATE `incidencia` \n"
                                    + "SET `usuarioAdministrador`=?,`usuarioSupervisor`=? \n"
                                    + "WHERE id=?";
                            try (PreparedStatement psUpdate = (PreparedStatement) connection.prepareStatement(update)) {
                                psUpdate.setString(1, correoAdmin);
                                psUpdate.setString(2, correoSupervisor);
                                psUpdate.setInt(3, idIncidencia);
                                long nUpdate = psUpdate.executeUpdate();

                                if (nUpdate > 0) {
                                    //una vez actualizada la incidencia procedemos a insertar el nuevo estado de la incidencia
                                    String insert = "INSERT INTO `estadoincidencia`(`id`, `fecha`, `descripcion`, `idIncidencia`, `idEstado`, `codImagen`) VALUES (NULL,?,NULL,?,2,?)";

                                    myDate = new Date();
                                    fecha = new SimpleDateFormat("yyyy-MM-dd").format(myDate);

                                    try (PreparedStatement psInsert = (PreparedStatement) connection.prepareStatement(insert)) {
                                        psInsert.setString(1, fecha);
                                        psInsert.setInt(2, idIncidencia);
                                        psInsert.setInt(3, codImagen);
                                        long nInsert = psInsert.executeUpdate();

                                        if (nInsert > 0) {
                                            asignarSupervisor = true;
                                        }
                                    }

                                }
                            }

                        }
                    }
                }
                connection.close();
            }
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }

        return asignarSupervisor;
    }

    public synchronized boolean asignarIncidenciaEmpleado(int idIncidencia, String correoEmpleado) {
        boolean asignarEmpleado = false;
        try {
            Class.forName(driver);
            //obtenemos el codigo de la imagen de la incidencia
            try (Connection connection = (Connection) DriverManager.getConnection(url, user, password)) {
                //obtenemos el codigo de la imagen de la incidencia
                String consultaCodImagen = "SELECT `codImagen` \n"
                        + "FROM `estadoincidencia` \n"
                        + "WHERE idIncidencia=? AND idEstado=1";
                int codImagen;
                try (PreparedStatement psConsultaCodImagen = (PreparedStatement) connection.prepareStatement(consultaCodImagen)) {
                    psConsultaCodImagen.setInt(1, idIncidencia);
                    try (ResultSet resultConsultaCodImagen = psConsultaCodImagen.executeQuery()) {
                        codImagen = 0;
                        if (resultConsultaCodImagen.next()) {
                            codImagen = resultConsultaCodImagen.getInt(1);
                        }
                    }
                }

                //comenzamos actualizando la incidencia donde dejaremos reflejado quien es el empleado encargados de ella
                String update = "UPDATE `incidencia` \n"
                        + "SET `usuarioEmpleado`=? \n"
                        + "WHERE id=?";
                try (PreparedStatement psUpdate = (PreparedStatement) connection.prepareStatement(update)) {
                    psUpdate.setString(1, correoEmpleado);
                    psUpdate.setInt(2, idIncidencia);
                    long nUpdate = psUpdate.executeUpdate();
                    
                    if (nUpdate > 0) {
                        //una vez actualizada la incidencia procedemos a insertar el nuevo estado de la incidencia
                        String insert = "INSERT INTO `estadoincidencia`(`id`, `fecha`, `descripcion`, `idIncidencia`, `idEstado`, `codImagen`) VALUES (NULL,?,NULL,?,4,?)";
                        
                        myDate = new Date();
                        fecha = new SimpleDateFormat("yyyy-MM-dd").format(myDate);
                        
                        try (PreparedStatement psInsert = (PreparedStatement) connection.prepareStatement(insert)) {
                            psInsert.setString(1, fecha);
                            psInsert.setInt(2, idIncidencia);
                            psInsert.setInt(3, codImagen);
                            long nInsert = psInsert.executeUpdate();
                            
                            if (nInsert > 0) {
                                asignarEmpleado = true;
                            }
                        }
                        
                    }
                } 
                connection.close();
            }

        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }

        return asignarEmpleado;
    }

    public JSONArray obtenerDetallesIncidenciaArreglada(int id){
        JSONArray ar = new JSONArray();
        JSONObject obj;
        encoder = new BASE64Encoder();

        try {
            
            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                String consulta1,consulta2,consulta3;
                
                
                //  consulta para obtener detalles de la incidencia
                consulta1 = "SELECT u.nombre, u.apellido, i.usuarioCiudadano, ei.fecha, ti.nombre, i.descripcion, i.direccion, d.nombre, (SELECT concat_ws(' ', nombre, apellido) FROM usuario WHERE correo = i.usuarioSupervisor) as \"Supervisor\", (SELECT concat_ws(' ', nombre, apellido) FROM usuario WHERE correo = i.usuarioEmpleado) as \"Empleado\", ei.codImagen \n" +
                        "FROM incidencia i LEFT JOIN usuario u ON (i.usuarioCiudadano=u.correo) LEFT JOIN estadoincidencia ei ON (i.id=ei.idIncidencia) LEFT JOIN tipoincidencia ti ON (i.idTipo=ti.id) LEFT JOIN departamento d ON (ti.idDepartamento=d.id)\n" +
                        "WHERE i.id=? AND ei.idEstado=1;";
                ResultSet resultConsulta1;
                String departamento = null;
                int codigoImagen = 0;
                try (PreparedStatement psConsulta1 = (PreparedStatement) connection.prepareStatement(consulta1)) {
                    psConsulta1.setInt(1, id);
                    resultConsulta1 = psConsulta1.executeQuery();
                    String nombreApellido, correo, obtenerDetallesNuevaRegistrada, tipo, descripcion, direccion, supervisor, empleado;                    
                    while (resultConsulta1.next()) {
                        obj = new JSONObject();
                        nombreApellido = resultConsulta1.getString(1) + " " + resultConsulta1.getString(2);
                        obj.put("nombreApellido", nombreApellido);
                        correo = resultConsulta1.getString(3);
                        obj.put("correo", correo);
                        obtenerDetallesNuevaRegistrada = resultConsulta1.getString(4);
                        obj.put("fecha", obtenerDetallesNuevaRegistrada);
                        tipo = resultConsulta1.getString(5);
                        obj.put("tipo", tipo);
                        descripcion = resultConsulta1.getString(6);
                        obj.put("descripcion", descripcion);
                        direccion = resultConsulta1.getString(7);
                        obj.put("direccion", direccion);
                        departamento = resultConsulta1.getString(8);
                        obj.put("departamento", departamento);
                        supervisor = resultConsulta1.getString(9);
                        obj.put("supervisor", supervisor);
                        empleado = resultConsulta1.getString(10);
                        obj.put("empleado", empleado);
                        codigoImagen = resultConsulta1.getInt(11);
                        ar.add(obj);
                    }
                }
                resultConsulta1.close();    
                
                //  consulta para obtener la imagen de la incidencia
                consulta2 = "SELECT imagen\n" +
                                "FROM imagen WHERE\n" +
                                "id = ?;";
                ResultSet resultConsulta2;
                try (PreparedStatement psConsulta2 = (PreparedStatement) connection.prepareStatement(consulta2)) {
                    psConsulta2.setInt(1, codigoImagen);
                    resultConsulta2 = psConsulta2.executeQuery();
                    File file;
                    String imagen, base64;
                    while (resultConsulta2.next()) {
                        obj = new JSONObject();
                        imagen = resultConsulta2.getString(1);
                        file = new File(pathImagenes + "\\" + imagen);
                        if (file.exists()) {

                            BufferedImage img = ImageIO.read(file);
                            //BufferedImage image = resize(img, 390, 450);
                            BufferedImage image = resize(img, 400, 500);

                            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                                ImageIO.write(image, "jpg", bos);
                                byte[] imageBytes = bos.toByteArray();
                                base64 = encoder.encode(imageBytes);
                            }

                            obj.put("imagen", base64);
                            ar.add(obj);
                        }

                    }
                }
                resultConsulta2.close();
                
                //  consulta para obtener detalles del arreglo
                consulta3 = "SELECT ei.fecha, (SELECT ei2.descripcion FROM estadoincidencia ei2 WHERE ei2.idIncidencia=i.id AND ei2.idEstado=5) as \"descripcion arreglo\", ei.codImagen\n" +
                                "FROM incidencia i LEFT JOIN estadoincidencia ei ON (i.id=ei.idIncidencia) LEFT JOIN tipoincidencia ti ON (i.idTipo=ti.id) LEFT JOIN departamento d ON (ti.idDepartamento=d.id)\n" +
                                "WHERE i.id=? AND ei.idEstado=6";
                ResultSet resultConsulta3;
                int codigoImagenArreglo;
                try (PreparedStatement psConsulta3 = (PreparedStatement) connection.prepareStatement(consulta3)) {
                    psConsulta3.setInt(1,id);
                    resultConsulta3 = psConsulta3.executeQuery();
                    String fechaArreglo, descripcionArreglo;
                    codigoImagenArreglo = 0;
                    while(resultConsulta3.next()){
                        obj = new JSONObject();
                        fechaArreglo = resultConsulta3.getString(1);
                        obj.put("fechaArreglo", fechaArreglo);
                        descripcionArreglo = resultConsulta3.getString(2);
                        obj.put("descripcionArreglo", descripcionArreglo);
                        codigoImagenArreglo = resultConsulta3.getInt(3);
                        ar.add(obj);                         
                    }
                }
                resultConsulta3.close();
                
                
                //  consulta para obtener la imagen del arreglo usaremos la misma consulta2                
                ResultSet resultConsulta4;
                try (PreparedStatement psConsulta4 = (PreparedStatement) connection.prepareStatement(consulta2)) {
                    psConsulta4.setInt(1,codigoImagenArreglo);
                    resultConsulta4 = psConsulta4.executeQuery();
                    File file;
                    String imagen, base64;
                    while(resultConsulta4.next()){
                        obj = new JSONObject();
                        imagen = resultConsulta4.getString(1);
                        file = new File(pathImagenes+"\\"+imagen);
                        if(file.exists()){
                            
                            BufferedImage img = ImageIO.read(file);
                            BufferedImage image = resize(img, 400, 500);
                            
                            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                                ImageIO.write(image, "jpg", bos);
                                byte[] imageBytes = bos.toByteArray();
                                
                                
                                base64 = encoder.encode(imageBytes);
                            }
                            
                            obj.put("imagenArreglo",base64);
                            ar.add(obj);
                        }
                        
                    }
                }
                resultConsulta4.close();
                connection.close();
            }            
        } catch (ClassNotFoundException | SQLException | IOException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return ar;
    }    
    
    public boolean solucionarIncidencia(int idIncidencia){
        boolean solucionada = false;
        
        try {
            Class.forName(driver);  
            //obtenemos el codigo de la imagen de la incidencia
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                //obtenemos el codigo de la imagen de la incidencia
                String consultaCodImagen="SELECT `codImagen` \n" +
                        "FROM `estadoincidencia` \n" +
                        "WHERE idIncidencia=? AND idEstado=5";
                
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
                
                //insertamos en la tabla estadoincidencia reflejando que ya se encuentra en estado "solucionada" la incidencia
                String insert = "INSERT INTO `estadoincidencia`(`id`, `fecha`, `descripcion`, `idIncidencia`, `idEstado`, `codImagen`) VALUES (NULL,?,NULL,?,7,?)";
                
                myDate = new Date();
                fecha = new SimpleDateFormat("yyyy-MM-dd").format(myDate);
                
                try (PreparedStatement psInsert = (PreparedStatement) connection.prepareStatement(insert)) {
                    psInsert.setString(1, fecha);
                    psInsert.setInt(2, idIncidencia);
                    psInsert.setInt(3, codImagen);
                    long nInsert = psInsert.executeUpdate();
                    
                    if(nInsert>0){
                        solucionada=true;
                    }
                }
                connection.close();
            }            
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return solucionada;
    }
    
    public boolean denegarSolucionIncidencia(int idIncidencia, String detalleDenegacion){
        boolean denegada = false;
        
        try {            
            
            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                String consultUpdate = "UPDATE estadoincidencia\n" +
                        "SET descripcion=?" +
                        "WHERE idIncidencia=? AND idEstado=4";
                PreparedStatement psConsultDelete;
                try (PreparedStatement psConsultUpdate = (PreparedStatement) connection.prepareStatement(consultUpdate)) {
                    psConsultUpdate.setString(1,detalleDenegacion);
                    psConsultUpdate.setInt(2,idIncidencia);
                    
                    String consultDelete = "DELETE FROM estadoincidencia\n" +
                            "WHERE idIncidencia = ? AND idEstado = 5 || idEstado = 6";
                    psConsultDelete = (PreparedStatement) connection.prepareStatement(consultDelete);
                    psConsultDelete.setInt(1,idIncidencia);
                    int resultUpdate = psConsultUpdate.executeUpdate();
                    int resultDelete = psConsultDelete.executeUpdate();
                    if(resultUpdate>0 && resultDelete>0){
                        denegada=true;
                    }
                }
                psConsultDelete.close();
                connection.close();
            }
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }        
        return denegada;
    }
    
    public JSONArray obtenerHistorialIncidencia(int id){
        JSONArray ar = new JSONArray();
        JSONObject obj;
        
        try {
            
            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                String consulta1,consulta2;
                //primera consulta para obtener los datos del primer estado la incidencia
                consulta1 = "SELECT e.titulo,\n" +
                        "(SELECT concat_ws(' ', u2.nombre, u2.apellido) FROM incidencia i2 LEFT JOIN usuario u2 ON(i2.usuarioCiudadano=u2.correo) WHERE i2.id=i.id) as \"ciudadano\",\n" +
                        "i.usuarioCiudadano, ti.nombre, ei.fecha, i.descripcion, i.direccion, \n" +
                        "(SELECT concat_ws(' ', u3.nombre, u3.apellido) FROM incidencia i3 LEFT JOIN usuario u3 ON(i3.usuarioAdministrador=u3.correo) WHERE i3.id=i.id) as \"administrador\",\n" +
                        "d.nombre as \"departamento\",\n" +
                        "(SELECT concat_ws(' ', u4.nombre, u4.apellido) FROM incidencia i4 LEFT JOIN usuario u4 ON(i4.usuarioSupervisor=u4.correo) WHERE i4.id=i.id) as \"supervisor\",\n" +
                        "(SELECT concat_ws(' ', u5.nombre, u5.apellido) FROM incidencia i5 LEFT JOIN usuario u5 ON(i5.usuarioEmpleado=u5.correo) WHERE i5.id=i.id) as \"empleado\",\n" +
                        "ii.imagen\n" +
                        "FROM incidencia i \n" +
                        "LEFT JOIN estadoincidencia ei ON (i.id=ei.idIncidencia) \n" +
                        "LEFT JOIN tipoincidencia ti ON (i.idTipo=ti.id) \n" +
                        "LEFT JOIN departamento d ON (ti.idDepartamento=d.id)\n" +
                        "LEFT JOIN estado e ON (ei.idEstado = e.id)\n" +
                        "LEFT JOIN imagen ii ON (ei.codImagen=ii.id)\n" +
                        "WHERE i.id=? AND ei.idEstado=1";
                ResultSet resultConsulta1;
                String estadoIncidencia, fecha, descripcion, imagen, imagenBase64;
                File file;
                byte[] imageBytes;
                try (PreparedStatement psConsulta1 = (PreparedStatement) connection.prepareStatement(consulta1)) {
                    psConsulta1.setInt(1,id);
                    resultConsulta1 = psConsulta1.executeQuery();
                    String usuario, correoUsuario, tipo, direccion, administrador, departamento, supervisor, empleado;
                    imagenBase64 = null;
                    encoder = new BASE64Encoder();
                    while(resultConsulta1.next()){
                        obj = new JSONObject();
                        estadoIncidencia = resultConsulta1.getString(1);
                        obj.put("estadoIncidencia", estadoIncidencia);
                        
                        usuario = resultConsulta1.getString(2);
                        obj.put("usuario", usuario);
                        
                        correoUsuario = resultConsulta1.getString(3);
                        obj.put("correoUsuario", correoUsuario);
                        
                        tipo = resultConsulta1.getString(4);
                        obj.put("tipo", tipo);
                        
                        fecha = resultConsulta1.getString(5);
                        obj.put("fecha", fecha);
                        
                        descripcion = resultConsulta1.getString(6);
                        obj.put("descripcion", descripcion);
                        
                        direccion = resultConsulta1.getString(7);
                        obj.put("direccion", direccion);
                        
                        administrador = resultConsulta1.getString(8);
                        obj.put("administrador", administrador);
                        
                        departamento = resultConsulta1.getString(9);
                        obj.put("departamento", departamento);
                        
                        supervisor = resultConsulta1.getString(10);
                        obj.put("supervisor", supervisor);
                        
                        empleado = resultConsulta1.getString(11);
                        obj.put("empleado", empleado);
                        
                        imagen = resultConsulta1.getString(12);
                        file = new File(pathImagenes+"\\"+imagen);
                        if(file.exists()){
                            
                            BufferedImage img = ImageIO.read(file);
                            BufferedImage image = resize(img, 390, 450);
                            
                            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                                ImageIO.write(image, "jpg", bos);
                                imageBytes = bos.toByteArray();
                                
                                imagenBase64 = encoder.encode(imageBytes);
                            }
                            
                            obj.put("imagen",imagenBase64);
                        }
                        
                        ar.add(obj);
                    }
                }
                resultConsulta1.close();
                
                consulta2 = "SELECT e.titulo, ei.fecha,	case  \n" +
                                "when ei.idEstado = 7 then (SELECT ei2.descripcion FROM estadoincidencia ei2 WHERE ei2.idIncidencia=i.id AND ei2.idEstado=5)\n" +
                                "else ei.descripcion\n" +
                                "end as \"descripcion\",\n" +
                                "ii.imagen\n" +
                                "FROM incidencia i LEFT JOIN estadoincidencia ei ON (i.id=ei.idIncidencia) LEFT JOIN estado e ON (ei.idEstado=e.id) LEFT JOIN imagen ii ON (ei.codImagen=ii.id)\n" +
                                "WHERE i.id=? AND ei.idEstado>1";
                ResultSet resultConsulta2;
                try (PreparedStatement psConsulta2 = (PreparedStatement) connection.prepareStatement(consulta2)) {
                    psConsulta2.setInt(1,id);
                    resultConsulta2 = psConsulta2.executeQuery();
                    while(resultConsulta2.next()){
                        obj = new JSONObject();
                        estadoIncidencia = resultConsulta2.getString(1);
                        obj.put("estadoIncidencia", estadoIncidencia);
                        switch(estadoIncidencia){
                            
                            case "EnTramite":
                                fecha = resultConsulta2.getString(2);
                                obj.put("fecha", fecha);
                                break;
                                
                            case "Validada":
                                fecha = resultConsulta2.getString(2);
                                obj.put("fecha", fecha);
                                break;
                                
                            case "EnArreglo":
                                fecha = resultConsulta2.getString(2);
                                obj.put("fecha", fecha);
                                break;
                                
                            case "ValidarArreglo":
                                fecha = resultConsulta2.getString(2);
                                obj.put("fecha", fecha);
                                break;
                                
                            case "Arreglada":
                                fecha = resultConsulta2.getString(2);
                                obj.put("fecha", fecha);
                                break;
                                
                            case "Solucionada":
                                fecha = resultConsulta2.getString(2);
                                obj.put("fecha", fecha);
                                descripcion = resultConsulta2.getString(3);
                                obj.put("descripcion", descripcion);
                                imagen = resultConsulta2.getString(4);
                                file = new File(pathImagenes+"\\"+imagen);
                                
                                if(file.exists()){
                                    
                                    BufferedImage img = ImageIO.read(file);
                                    BufferedImage image = resize(img, 390, 450);
                                    
                                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                                        ImageIO.write(image, "jpg", bos);
                                        imageBytes = bos.toByteArray();
                                        
                                        imagenBase64 = encoder.encode(imageBytes);
                                    }
                                    
                                    obj.put("imagen",imagenBase64);
                                }
                                break;
                                
                            case "Denegada":
                                
                                fecha = resultConsulta2.getString(2);
                                obj.put("fecha", fecha);
                                descripcion = resultConsulta2.getString(3);
                                obj.put("descripcion", descripcion);
                                break;
                                
                        }
                        
                        ar.add(obj);
                    }
                }
                resultConsulta2.close();
                connection.close();
            }            
        } catch (ClassNotFoundException | SQLException | IOException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return ar;
    }
    
    public JSONObject obtenerDepartamentos() {
        JSONObject obj = null;
        
        try {
            
            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                String consulta= "SELECT `id`, `nombre` FROM `departamento`";
                ResultSet resultConsulta;
                try (PreparedStatement psConsulta = (PreparedStatement) connection.prepareStatement(consulta)) {
                    resultConsulta = psConsulta.executeQuery();
                    String nombreDep, departamentos="";
                    int idDep;
                    while(resultConsulta.next()){
                        idDep = resultConsulta.getInt(1);
                        nombreDep = resultConsulta.getString(2);
                        departamentos += nombreDep+":"+idDep+";";
                    }   obj = new JSONObject();
                    obj.put("departamentos", departamentos);
                }
                resultConsulta.close();
                connection.close();
            }            
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return obj;
    }
    
    public synchronized String registrarDepartamento(String nombreDepartamento){
        String registrarDep = "denegado";
        try {
            
            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                String consulta="SELECT * FROM `departamento` WHERE nombre = ?";
                ResultSet result;
                try (PreparedStatement psConsulta = (PreparedStatement) connection.prepareStatement(consulta)) {
                    psConsulta.setString(1,nombreDepartamento);
                    result = psConsulta.executeQuery();
                    if(!result.next()){
                        
                        String insertar="INSERT INTO `departamento`(`id`, `nombre`) VALUES (NULL,?)";
                        try (PreparedStatement psInsertar = (PreparedStatement) connection.prepareStatement(insertar)) {
                            psInsertar.setString(1,nombreDepartamento);
                            
                            long n = psInsertar.executeUpdate();
                            
                            if(n>0){
                                registrarDep = "registrado";
                            }
                        }
                        
                    }else{
                        registrarDep = "yaExiste";
                    }
                }
                result.close();
                connection.close();
            }
            
            
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
        return registrarDep;   
    }
    
    public synchronized boolean eliminarDepartamento(int idDepartamento){
        
        boolean eliminarDepartamento = false;
        try {
            Class.forName(driver);  
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                String delete="DELETE FROM `departamento` WHERE id=?";
                
                try (PreparedStatement psDelete = (PreparedStatement) connection.prepareStatement(delete)) {
                    psDelete.setInt(1,idDepartamento);
                    
                    long resultDelete = psDelete.executeUpdate();
                    if(resultDelete>0){
                        eliminarDepartamento=true;
                    }
                }
            connection.close();
            }            
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return eliminarDepartamento;   
    
    }
    
    public JSONArray listadoUsuariosTabla(int tipoUsuario) {
        JSONArray ar = null;

        try {
            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url, user, password)) {
                String consulta = "SELECT correo, nombre, apellido, dni\n" +
                                    "FROM usuario\n" +
                                    "WHERE tipousuario = ? AND activo=true";
                
                ResultSet resultConsultaUsuarios;
                try (PreparedStatement psConsultaIncidencias = (PreparedStatement) connection.prepareStatement(consulta)) {
                    
                    psConsultaIncidencias.setInt(1, tipoUsuario);
                    
                    resultConsultaUsuarios = psConsultaIncidencias.executeQuery();
                    String correo, nombre, apellido, dni;
                    ar = new JSONArray();
                    JSONObject obj;
                    while (resultConsultaUsuarios.next()) {
                        obj = new JSONObject();
                        correo = resultConsultaUsuarios.getString(1);
                        obj.put("correo", correo);
                        nombre = resultConsultaUsuarios.getString(2);
                        obj.put("nombre", nombre);
                        apellido = resultConsultaUsuarios.getString(3);
                        obj.put("apellido", apellido);
                        dni = resultConsultaUsuarios.getString(4);
                        obj.put("dni", dni);
                        ar.add(obj);
                    }
                }
                resultConsultaUsuarios.close();
                connection.close();
            }
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }

        return ar;
    }

    public synchronized boolean eliminarUsuario(String correoUsuario){
        
        boolean eliminarUsuario = false;
        try {
            Class.forName(driver);  
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                String delete="UPDATE `usuario` SET activo = false WHERE correo=?";
                
                try (PreparedStatement psDelete = (PreparedStatement) connection.prepareStatement(delete)) {
                    psDelete.setString(1,correoUsuario);
                    
                    long resultDelete = psDelete.executeUpdate();
                    if(resultDelete>0){
                        eliminarUsuario=true;
                    }
                }
            connection.close();    
            }            
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return eliminarUsuario;   
    
    }
    
    public synchronized boolean modificarUsuarioEscritorio(String correo, String nombre, String apellidos, String dni, int tlf, String idDepartamento){
        boolean modificarUsuario = false;
        try {
            
            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                String update = "UPDATE `usuario` \n" +
                        "SET `nombre`= ?,`apellido`= ?,`dni`= ?,`tlf`= ?,`idDepartamento`= ?\n" +
                        "WHERE `correo` = ?";
                
                try (PreparedStatement psUpdate = (PreparedStatement) connection.prepareStatement(update)) {
                    psUpdate.setString(1,nombre);
                    psUpdate.setString(2,apellidos);
                    psUpdate.setString(3,dni);
                    psUpdate.setInt(4,tlf);
                    
                    if(idDepartamento.equals("NULL")){
                        psUpdate.setNull(5, java.sql.Types.NULL);
                    }else{
                        psUpdate.setInt(5, Integer.parseInt(idDepartamento));
                    }
                    
                    psUpdate.setString(6, correo);
                    
                    long n = psUpdate.executeUpdate();
                    
                    if(n>0){
                        System.out.println("Usuario modificado.");
                        modificarUsuario = true;
                    }
                }
                connection.close();
            }
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
        return modificarUsuario;   
    }
    
    //      APP ANDROID
    public synchronized int logUsuario(String correo, String contrasena, boolean sesionIniciada) {
        int tipoUsuario = 0;
        try {
            Class.forName(driver);
            PreparedStatement psConsulta;
            ResultSet result;
            try (Connection connection = (Connection) DriverManager.getConnection(url, user, password)) {
                String consulta = "SELECT correo, contrasena, nombre, apellido, tipoUsuario FROM usuario WHERE correo=? AND contrasena=? AND activo=true";
                psConsulta = (PreparedStatement) connection.prepareStatement(consulta);
                psConsulta.setString(1, correo);
                psConsulta.setString(2, contrasena);
                result = psConsulta.executeQuery();
                //comprobamos si hay algun usuario con ese correo
                //si es así iniciamos sesion
                //si no es así informamos que no es correcto el correo/contrasena
                if (result.next()) {
                    //comprobamos si es la primera vez que el usuario inicia sesion
                    //si es asi, registramos el correo con valor 1
                    if (!usuariosLog.containsKey(correo)) {
                        usuariosLog.put(correo, 1);
                    } else {
                        //si ya ha iniciado sesion en otro dispositivo, iremos sumando tantas veces lo haga
                        //cuando el valor de sesionInciada es true significa que ya habia iniciado sesion con ese dispositivo
                        //asi llevaremos un control del usuario y el numero de veces que ha inicado sesion en otro dispositivo
                        if (!sesionIniciada) {
                            int n_sesiones_iniciadas = usuariosLog.get(correo);
                            n_sesiones_iniciadas++;
                            usuariosLog.put(correo, n_sesiones_iniciadas);
                        }
                    }
                    tipoUsuario = result.getInt(5);
                }
                psConsulta.close();
                result.close();
                connection.close();
            }
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }

        return tipoUsuario;
    }

    public void logOutUsuario(String correo) {
        int n_sesiones_iniciadas = usuariosLog.get(correo);
        n_sesiones_iniciadas--;

        if (n_sesiones_iniciadas == 0) {
            usuariosLog.remove(correo);
        } else {
            usuariosLog.put(correo, n_sesiones_iniciadas);
        }

    }

    public synchronized String nuevaIncidencia(String ubicacion, String direccion, String descripcion, String tipo, String correoUsuario, String imagen) {
        String respuesta = null;
        int idIncidencia = 0, idTipoIncidencia, idImagenIncidencia = 0;
        String[] ubi = ubicacion.split(";");

        try {
            Class.forName(driver);
            Connection connection = (Connection) DriverManager.getConnection(url, user, password);
            //COMPROBAMOS SI EXISTE LA INCIDENCIA
            String consulta = "SELECT i.id \n" +
                                "FROM incidencia i LEFT JOIN estadoincidencia ei ON (i.id=ei.idIncidencia), tipoincidencia t\n" +
                                "WHERE i.idTipo=t.id AND T.nombre = ?\n" +
                                "AND CAST(SUBSTRING_INDEX(i.ubicacion,';',-1) AS DECIMAL(10,10)) BETWEEN ? AND ?\n" +
                                "AND CAST(SUBSTRING_INDEX(i.ubicacion,';',1) AS DECIMAL(8,6)) BETWEEN ? AND ?\n" +
                                "GROUP BY i.id\n" +
                                "HAVING MAX(ei.idEstado)<7";
            ResultSet result;
            try (PreparedStatement psConsulta = (PreparedStatement) connection.prepareStatement(consulta)) {
                psConsulta.setString(1, tipo);
                psConsulta.setDouble(2, (Double.parseDouble(ubi[1]) - 10));
                psConsulta.setDouble(3, (Double.parseDouble(ubi[1]) + 10));
                psConsulta.setDouble(4, (Double.parseDouble(ubi[0]) - 10));
                psConsulta.setDouble(5, (Double.parseDouble(ubi[0]) + 10));
                result = psConsulta.executeQuery();
                //si hay alguna incidencia con la misma ubicacion en un radio de 10m y que no sea ni nueva/arreglada
                //si es así, significa que estamos hablando de la misma incidencia y no habrá que registrarla
                if (result.next()) {
                    respuesta = "YaExiste";
                    
                    psConsulta.close();
                    result.close();
                    connection.close();
                    
                    return respuesta;
                }
            }
            result.close();
            

            //OBTENEMOS ID DEL TIPO DE INCIDENCIA
            String insertTipo = "SELECT id FROM `tipoincidencia` WHERE nombre = ?";
            ResultSet resultTipo;
            try (PreparedStatement psConsultaTipo = (PreparedStatement) connection.prepareStatement(insertTipo)) {
                psConsultaTipo.setString(1, tipo);
                resultTipo = psConsultaTipo.executeQuery();
                if (resultTipo.next()) {
                    idTipoIncidencia = resultTipo.getInt(1);
                } else {
                    respuesta = "NoOk";
                    
                    psConsultaTipo.close();
                    resultTipo.close();
                    connection.close();
                    return respuesta;
                }
            }
            resultTipo.close();

            //INSERTAMOS LA IMAGEN VINCULADA A LA INCIDENCIA
            String insertImagen = "INSERT INTO `imagen`(`id`, `imagen`) VALUES (NULL,?)";
            PreparedStatement psInsertarImagen = (PreparedStatement) connection.prepareStatement(insertImagen);
            psInsertarImagen.setString(1, imagen);
            long nImagen = psInsertarImagen.executeUpdate();

            if (nImagen > 0) {

                //OBTENEMOS EL ID DE DICHA IMAGEN
                String selectImagen = "SELECT id FROM `imagen` WHERE `imagen` = ?";
                ResultSet resultImagen;
                try (PreparedStatement psConsultaImagen = (PreparedStatement) connection.prepareStatement(selectImagen)) {
                    psConsultaImagen.setString(1, imagen);
                    resultImagen = psConsultaImagen.executeQuery();
                    if (resultImagen.next()) {
                        idImagenIncidencia = resultImagen.getInt(1);
                    }
                    psInsertarImagen.close();
                }
                resultImagen.close();
            } else {
                respuesta = "NoOk";
                psInsertarImagen.close();
                connection.close();
                return respuesta;
            }

            //REGISTRAMOS LA INCIDENCIA
            String insertIncidencia = "INSERT INTO `incidencia`(`id`, `ubicacion`, `direccion`, `descripcion`, `idTipo`, `usuarioCiudadano`, `usuarioAdministrador`, `usuarioSupervisor`, `usuarioEmpleado`) VALUES (NULL,?,?,?,?,?,NULL,NULL,NULL)";
            PreparedStatement psInsertarIncidencia = (PreparedStatement) connection.prepareStatement(insertIncidencia);
            psInsertarIncidencia.setString(1, ubicacion);
            psInsertarIncidencia.setString(2, direccion);
            psInsertarIncidencia.setString(3, descripcion);
            psInsertarIncidencia.setInt(4, idTipoIncidencia);
            psInsertarIncidencia.setString(5, correoUsuario);

            long nIncidencia = psInsertarIncidencia.executeUpdate();
            if (nIncidencia > 0) {

                //OBTENEMOS ID DE LA INCIDENCIA
                String selectIncidencia = "SELECT `id` FROM `incidencia` WHERE `ubicacion` = ? AND `descripcion` = ? AND `idTipo` = ? AND `usuarioCiudadano` = ?";
                ResultSet resultIncidencia;
                try (PreparedStatement psConsultaIncidencia = (PreparedStatement) connection.prepareStatement(selectIncidencia)) {
                    psConsultaIncidencia.setString(1, ubicacion);
                    psConsultaIncidencia.setString(2, descripcion);
                    psConsultaIncidencia.setInt(3, idTipoIncidencia);
                    psConsultaIncidencia.setString(4, correoUsuario);
                    resultIncidencia = psConsultaIncidencia.executeQuery();
                    if (resultIncidencia.next()) {
                        idIncidencia = resultIncidencia.getInt(1);
                    }
                    psInsertarIncidencia.close();
                }
                resultIncidencia.close();
            } else {
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

            psInsertarEstadoIncidencia.setString(1, fecha);
            psInsertarEstadoIncidencia.setInt(2, idIncidencia);
            psInsertarEstadoIncidencia.setInt(3, idImagenIncidencia);

            long nEstadoIncidencia = psInsertarEstadoIncidencia.executeUpdate();
            if (nEstadoIncidencia > 0) {
                respuesta = "Ok";
                psInsertarEstadoIncidencia.close();
                
            } else {
                respuesta = "NoOk";
                psInsertarEstadoIncidencia.close();
                connection.close();
                return respuesta;
            }
        connection.close();
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }

        return respuesta;

    }

    public JSONArray obtenerListadoIncidencias(String correo, String tipo_incidencias) {
        JSONArray ar = null;
        try {

            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url, user, password)) {
                String consulta = null;
                switch (tipo_incidencias) {

                    // usuario ciudadano
                    case "historial":
                        consulta = "SELECT MAX(ei.idEstado), i.id, t.nombre, (SELECT titulo FROM estado WHERE id=MAX(ei.idEstado)) AS \"estado\", i.ubicacion, i.direccion, MAX(ei.fecha)\n" +
                                "FROM incidencia i LEFT JOIN estadoincidencia ei ON(i.id=ei.idIncidencia) LEFT JOIN tipoincidencia t ON(i.idTipo=t.id)\n" +
                                "WHERE i.usuarioCiudadano = ?\n" +
                                "GROUP BY(i.id)\n" +
                                "HAVING MAX(ei.idEstado) > 6\n" +
                                "ORDER BY MAX(ei.fecha)";
                        break;
                    case "activas":
                        consulta = "SELECT MAX(ei.idEstado), i.id, t.nombre, (SELECT titulo FROM estado WHERE id=MAX(ei.idEstado)) AS \"estado\", i.ubicacion, i.direccion\n" +
                                "FROM incidencia i LEFT JOIN estadoincidencia ei ON(i.id=ei.idIncidencia) LEFT JOIN tipoincidencia t ON(i.idTipo=t.id)\n" +
                                "WHERE i.usuarioCiudadano = ?" +
                                "GROUP BY(i.id)\n" +
                                "HAVING MAX(ei.idEstado) < 7\n" +
                                "ORDER BY MAX(ei.idEstado)";
                        break;
                        
                    // supervisor    
                    case "enTramite":
                        consulta="SELECT MAX(ei.idEstado), i.id, t.nombre, (SELECT titulo FROM estado WHERE id=MAX(ei.idEstado)) AS \"estado\", i.ubicacion, i.direccion\n" +
                                "FROM incidencia i LEFT JOIN estadoincidencia ei ON(i.id=ei.idIncidencia) LEFT JOIN tipoincidencia t ON(i.idTipo=t.id)\n" +
                                "WHERE i.usuarioSupervisor = ?" +
                                "GROUP BY(i.id)\n" +
                                "HAVING MAX(ei.idEstado) = 2\n" +
                                "ORDER BY MAX(ei.fecha)";
                        break;  
                        
                    case "validarArreglo":
                        consulta="SELECT MAX(ei.idEstado), i.id, t.nombre, (SELECT titulo FROM estado WHERE id=MAX(ei.idEstado)) AS \"estado\", i.ubicacion, i.direccion\n" +
                                "FROM incidencia i LEFT JOIN estadoincidencia ei ON(i.id=ei.idIncidencia) LEFT JOIN tipoincidencia t ON(i.idTipo=t.id) LEFT JOIN estado e ON(ei.idEstado=e.id)\n" +
                                "WHERE i.usuarioSupervisor = ?\n" +
                                "GROUP BY(i.id)\n" +
                                "HAVING MAX(ei.idEstado) = 5\n" +
                                "ORDER BY MAX(ei.fecha)";
                        break; 
                        
                    // empleado    
                    case "enArreglo":
                        consulta="SELECT MAX(ei.idEstado) as \"idEstado\", i.id, t.nombre, (SELECT titulo FROM estado WHERE id=MAX(ei.idEstado)) AS \"estado\", i.ubicacion, i.direccion\n" +
                                "FROM incidencia i LEFT JOIN estadoincidencia ei ON(i.id=ei.idIncidencia) LEFT JOIN tipoincidencia t ON(i.idTipo=t.id) LEFT JOIN estado e ON(ei.idEstado=e.id)\n" +
                                "WHERE i.usuarioEmpleado = ?\n" +
                                "GROUP BY(i.id)\n" +
                                "HAVING MAX(ei.idEstado) = 4\n" +
                                "ORDER BY MAX(ei.fecha)";
                        break;    

                }
                ResultSet resultTipo;
                try (PreparedStatement psConsulta = (PreparedStatement) connection.prepareStatement(consulta)) {
                    psConsulta.setString(1, correo);
                    resultTipo = psConsulta.executeQuery();
                    int id;
                    String tipo, estado, ubicacion, direccion;
                    ar = new JSONArray();
                    JSONObject obj;
                    while (resultTipo.next()) {
                        obj = new JSONObject();
                        id = resultTipo.getInt(2);
                        obj.put("id", id);
                        tipo = resultTipo.getString(3);
                        obj.put("tipo", tipo);
                        estado = resultTipo.getString(4);
                        System.out.println(estado);
                        obj.put("estado", estado);
                        ubicacion = resultTipo.getString(5);
                        obj.put("ubicacion", ubicacion);
                        direccion = resultTipo.getString(6);
                        obj.put("direccion", direccion);
                        ar.add(obj);
                    }
                }
                resultTipo.close();
                connection.close();
            }
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ar;
    }

    public JSONArray obtenerDetallesIncidencia(int id) {
        JSONArray ar = null;
        try {

            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url, user, password)) {
                String consulta = "SELECT e.titulo, i.ubicacion, i.direccion, i.descripcion, t.nombre, ei.fecha,\n"
                        + "IF(ei.idEstado=7, (SELECT ei2.descripcion FROM estadoincidencia ei2 WHERE ei2.idIncidencia=i.id AND ei2.idEstado=5), ei.descripcion) as \"descripcionestado\",\n"
                        + "m.imagen, e.descripcion\n"
                        + "FROM incidencia i LEFT JOIN tipoincidencia t ON (i.idTipo=t.id) LEFT JOIN estadoincidencia ei ON (i.id=ei.idIncidencia) LEFT JOIN estado e ON (ei.idEstado=e.id) LEFT JOIN imagen m ON (ei.codImagen=m.id)\n"
                        + "WHERE ei.idEstado IN (1,2,3,4,5,7,8) AND i.id = ? ";
                ResultSet resultTipo;
                try (PreparedStatement psConsulta = (PreparedStatement) connection.prepareStatement(consulta)) {
                    psConsulta.setInt(1, id);
                    resultTipo = psConsulta.executeQuery();
                    ar = new JSONArray();
                    JSONObject obj;
                    File file;
                    String base64;
                    String estado, ubicacion, direccion, descripcion, tipo, descripcionEstadoIncidencia, imagen;
                    Date fecha;
                    while (resultTipo.next()) {
                        obj = new JSONObject();
                        estado = resultTipo.getString(1);
                        obj.put("estado", estado);
                        switch (estado) {
                            case "NuevaRegistrada":
                                ubicacion = resultTipo.getString(2);
                                obj.put("ubicacion", ubicacion);
                                direccion = resultTipo.getString(3);
                                obj.put("direccion", direccion);
                                descripcion = resultTipo.getString(4);
                                obj.put("descripcion", descripcion);
                                tipo = resultTipo.getString(5);
                                obj.put("tipo", tipo);
                                fecha = resultTipo.getDate(6);
                                obj.put("fecha", fecha);
                                obj.put("descripcionEstadoIncidencia", "");
                                obj.put("descripcionEstado", "");
                                imagen = resultTipo.getString(8);
                                file = new File(pathImagenes + "\\" + imagen);
                                System.out.println(file);

                                if (file.exists()) {
                                    BufferedImage bImage = ImageIO.read(file);
                                    BufferedImage nImage = resize(bImage, 400, 500);

                                    byte[] imageInByte;
                                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                                        ImageIO.write(nImage, "jpg", baos);
                                        baos.flush();
                                        imageInByte = baos.toByteArray();
                                    }
                                    base64 = Base64.getEncoder().encodeToString(imageInByte);

                                    obj.put("imagen", base64);
                                } else {
                                    obj.put("imagen", "");
                                }
                                ar.add(obj);
                                break;

                            case "EnTramite":
                                obj.put("ubicacion", "");
                                obj.put("direccion", "");
                                obj.put("descripcion", "");
                                obj.put("tipo", "");
                                fecha = resultTipo.getDate(6);
                                obj.put("fecha", fecha);
                                obj.put("descripcionEstadoIncidencia", "");
                                obj.put("descripcionEstado", "");
                                ar.add(obj);
                                break;
                                
                            case "Validada":
                                obj.put("ubicacion", "");
                                obj.put("direccion", "");
                                obj.put("descripcion", "");
                                obj.put("tipo", "");
                                fecha = resultTipo.getDate(6);
                                obj.put("fecha", fecha);
                                obj.put("descripcionEstadoIncidencia", "");
                                obj.put("descripcionEstado", "");
                                ar.add(obj);
                                break;    

                            case "EnArreglo":
                                obj.put("ubicacion", "");
                                obj.put("direccion", "");
                                obj.put("descripcion", "");
                                obj.put("tipo", "");
                                fecha = resultTipo.getDate(6);
                                obj.put("fecha", fecha);
                                obj.put("descripcionEstadoIncidencia", "");
                                obj.put("descripcionEstado", "");
                                ar.add(obj);
                                break;
                                
                            case "Arreglada":
                                obj.put("ubicacion", "");
                                obj.put("direccion", "");
                                obj.put("descripcion", "");
                                obj.put("tipo", "");
                                fecha = resultTipo.getDate(6);
                                obj.put("fecha", fecha);
                                obj.put("descripcionEstadoIncidencia", "");
                                obj.put("descripcionEstado", "");
                                ar.add(obj);
                                break;

                            case "Solucionada":
                                obj.put("ubicacion", "");
                                obj.put("direccion", "");
                                obj.put("descripcion", "");
                                obj.put("tipo", "");
                                fecha = resultTipo.getDate(6);
                                obj.put("fecha", fecha);
                                descripcionEstadoIncidencia = resultTipo.getString(7);
                                obj.put("descripcionEstadoIncidencia", descripcionEstadoIncidencia);
                                obj.put("descripcionEstado", "");
                                imagen = resultTipo.getString(8);
                                file = new File(pathImagenes + "\\" + imagen);
                                System.out.println(file);

                                if (file.exists()) {
                                    BufferedImage bImage = ImageIO.read(file);
                                    BufferedImage nImage = resize(bImage, 500, 700);

                                    byte[] imageInByte;
                            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                                ImageIO.write(nImage, "jpg", baos);
                                baos.flush();
                                imageInByte = baos.toByteArray();
                            }
                                    base64 = Base64.getEncoder().encodeToString(imageInByte);

                                    obj.put("imagen", base64);
                                } else {
                                    obj.put("imagen", "");
                                }
                                ar.add(obj);

                                break;
                                
                            case "Denegada":
                                obj.put("ubicacion", "");
                                obj.put("direccion", "");
                                obj.put("descripcion", "");
                                obj.put("tipo", "");
                                fecha = resultTipo.getDate(6);
                                obj.put("fecha", fecha);
                                descripcionEstadoIncidencia = resultTipo.getString(7);
                                obj.put("descripcionEstadoIncidencia", descripcionEstadoIncidencia);
                                obj.put("descripcionEstado", "");
                                ar.add(obj);
                                break;

                        }
                    }
                }
                resultTipo.close();
                connection.close();
            }
        } catch (ClassNotFoundException | SQLException | IOException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        } 
        return ar;
    }

    public JSONObject obtenerDetallesIncidenciaSupervisor(int id, String tipoEstado) {
        JSONObject object = null;
        try {

            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url, user, password)) {
                String consulta = "";
                if (tipoEstado.equals("enTramite")) {
                    consulta = "SELECT e.titulo, i.ubicacion, i.direccion, i.descripcion, t.nombre, ei.fecha, m.imagen\n" +
                                    "FROM incidencia i LEFT JOIN tipoincidencia t ON (i.idTipo=t.id) LEFT JOIN estadoincidencia ei ON (i.id=ei.idIncidencia) LEFT JOIN estado e ON (ei.idEstado=e.id) LEFT JOIN imagen m ON (ei.codImagen=m.id)\n" +
                                    "WHERE i.id = ? AND ei.idEstado = 2";
                }else{
                consulta="SELECT e.titulo, i.ubicacion, i.direccion, i.descripcion, t.nombre,\n" +
                            "(SELECT eii.fecha FROM estadoincidencia eii WHERE eii.idEstado=1 AND eii.idIncidencia=i.id) as \"Fecha Incidencia\",\n" +
                            "(SELECT mm.imagen FROM imagen mm LEFT JOIN estadoincidencia eeii ON (mm.id=eeii.codImagen)\n" +
                            "WHERE eeii.idEstado=1 AND eeii.idIncidencia=i.id) as \"Imagen Incidencia\",\n" +
                            "ei.descripcion, ei.fecha, m.imagen\n" +
                            "FROM incidencia i LEFT JOIN tipoincidencia t ON (i.idTipo=t.id) LEFT JOIN estadoincidencia ei ON (i.id=ei.idIncidencia) LEFT JOIN estado e ON (ei.idEstado=e.id) LEFT JOIN imagen m ON (ei.codImagen=m.id)\n" +
                            "WHERE i.id = ? AND ei.idEstado = 5";
                }

                ResultSet resultTipo;
                try (PreparedStatement psConsulta = (PreparedStatement) connection.prepareStatement(consulta)) {
                    psConsulta.setInt(1, id);
                    resultTipo = psConsulta.executeQuery();
                    object = new JSONObject();
                    File file, fileArreglo;
                    String base64,base64A;
                    String estado, ubicacion, direccion, descripcion, tipo, imagen, descripcionArreglo, fechaArreglo, imagenArreglo;
                    Date fecha;
                    while (resultTipo.next()) {
                        estado = resultTipo.getString(1);
                        object.put("estado", estado);
                        ubicacion = resultTipo.getString(2);
                        object.put("ubicacion", ubicacion);
                        direccion = resultTipo.getString(3);
                        object.put("direccion", direccion);
                        descripcion = resultTipo.getString(4);
                        object.put("descripcion", descripcion);
                        tipo = resultTipo.getString(5);
                        object.put("tipo", tipo);
                        fecha = resultTipo.getDate(6);
                        object.put("fecha", fecha);
                        imagen = resultTipo.getString(7);
                        file = new File(pathImagenes + "\\" + imagen);
                        if (file.exists()) {
                            BufferedImage bimage = ImageIO.read(file);
                            BufferedImage nImage = resize(bimage, 400, 500);
                            byte[] imageInByte;
                            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                                ImageIO.write(nImage, "jpg", baos);
                                baos.flush();
                                imageInByte = baos.toByteArray();
                            }
                            base64 = Base64.getEncoder().encodeToString(imageInByte);
                            object.put("imagen", base64);
                        } else {
                            object.put("imagen", "");
                        }
                        
                        if(!tipoEstado.equals("enTramite")){
                            descripcionArreglo = resultTipo.getString(8);
                            object.put("descripcionArreglo", descripcionArreglo);
                            fechaArreglo = resultTipo.getString(9);
                            object.put("fechaArreglo", fechaArreglo);
                            imagenArreglo = resultTipo.getString(10);
                            fileArreglo = new File(pathImagenes + "\\" + imagenArreglo);
                            if (fileArreglo.exists()) {
                            BufferedImage bimageA = ImageIO.read(fileArreglo);
                            BufferedImage nImageA = resize(bimageA, 400, 500);
                            byte[] imageInByteA;
                            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                                ImageIO.write(nImageA, "jpg", baos);
                                baos.flush();
                                imageInByteA = baos.toByteArray();
                            }
                            base64A = Base64.getEncoder().encodeToString(imageInByteA);
                            object.put("imagenArreglo", base64A);
                        } else {
                            object.put("imagenArreglo", "");
                        }
                        }
                    }
                }
                resultTipo.close();
                connection.close();
            }
        } catch (ClassNotFoundException | SQLException | IOException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        return object;
    }

    //este metodo no sería synchronized ya que solo puede acceder un supervisor para una sola incidencia
    //nunca se va a dar el caso de que dos supervisores diferentes accedan a una incidencia
    public String validacionIncidenciaSupervisor(int idIncidencia, String opcion, String descripcion){
        String respuesta="";
        int codImagen = 0;
        
        try {
            Class.forName(driver);  
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                String selecCodImg="select codImagen from estadoincidencia where idIncidencia=? and idEstado=1";
                ResultSet resultI;
                try (PreparedStatement psSelectCodImg = (PreparedStatement) connection.prepareStatement(selecCodImg)) {
                    psSelectCodImg.setInt(1,idIncidencia);
                    resultI = psSelectCodImg.executeQuery();
                    if(resultI.next()){
                        
                        codImagen = resultI.getInt(1);
                        
                    }
                }
                resultI.close();
                
                String insert="INSERT INTO `estadoincidencia`(`id`, `fecha`, `descripcion`, `idIncidencia`, `idEstado`, `codImagen`) VALUES (null,?,?,?,?,?)";
                try (PreparedStatement psInsert = (PreparedStatement) connection.prepareStatement(insert)) {
                    myDate = new Date();
                    fecha = new SimpleDateFormat("yyyy-MM-dd").format(myDate);
                    
                    psInsert.setString(1,fecha);
                    if(opcion.equals("validar_incidencia")){
                        psInsert.setNull( 2, java.sql.Types.NULL );
                        psInsert.setInt(3,idIncidencia);
                        psInsert.setInt(4,3);
                        psInsert.setInt(5,codImagen);
                    }else{
                        psInsert.setString(2,descripcion);
                        psInsert.setInt(3,idIncidencia);
                        psInsert.setInt(4,8);
                        psInsert.setNull( 5, java.sql.Types.NULL);
                    }
                    int result = psInsert.executeUpdate();
                    
                    if(result>0){
                        if(opcion.equals("validar_incidencia")){
                            respuesta="validadaOk";
                        }else{
                            respuesta="denegadaOk";
                        }
                    }else{
                        respuesta="error";
                    }
                }
                connection.close();
            }
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        return respuesta;
    } 

    public JSONObject obtenerDetallesIncidenciaEmpleado(int id){
        JSONObject object = null;
        try {
            
            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                String consulta="SELECT e.titulo, i.ubicacion, i.direccion, i.descripcion, t.nombre, ei.fecha, ei.descripcion, m.imagen\n" +
                        "FROM incidencia i LEFT JOIN tipoincidencia t ON (i.idTipo=t.id) LEFT JOIN estadoincidencia ei ON (i.id=ei.idIncidencia) LEFT JOIN estado e ON (ei.idEstado=e.id) LEFT JOIN imagen m ON (ei.codImagen=m.id)\n" +
                        "WHERE i.id = ? AND ei.idEstado = 4";
                ResultSet resultTipo;
                try (PreparedStatement psConsulta = (PreparedStatement) connection.prepareStatement(consulta)) {
                    psConsulta.setInt(1,id);
                    resultTipo = psConsulta.executeQuery();
                    object = new JSONObject();
                    File file;
                    String base64;
                    String estado, ubicacion, direccion, descripcion, tipo, imagen, descripcionEstadoIncidencia;
                    Date fecha;
                    while(resultTipo.next()){
                        estado = resultTipo.getString(1);
                        object.put("estado",estado);
                        ubicacion = resultTipo.getString(2);
                        object.put("ubicacion",ubicacion);
                        direccion = resultTipo.getString(3);
                        object.put("direccion",direccion);
                        descripcion = resultTipo.getString(4);
                        object.put("descripcion",descripcion);
                        tipo = resultTipo.getString(5);
                        object.put("tipo",tipo);
                        fecha = resultTipo.getDate(6);
                        object.put("fecha",fecha);
                        descripcionEstadoIncidencia = resultTipo.getString(7);
                        object.put("descripcionEstadoIncidencia", descripcionEstadoIncidencia);
                        imagen = resultTipo.getString(8);
                        file = new File(pathImagenes+"\\"+imagen);
                        if(file.exists()){
                            BufferedImage bimage = bimage = ImageIO.read(file);
                            BufferedImage nImage = resize(bimage, 400, 600);
                            byte[] imageInByte;
                            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                                ImageIO.write( nImage, "jpg", baos );
                                baos.flush();
                                imageInByte = baos.toByteArray();
                            }
                            base64 = Base64.getEncoder().encodeToString(imageInByte);
                            
                            object.put("imagen",base64);
                        }else{
                            object.put("imagen","");
                        }
                    }
                }
                resultTipo.close(); 
                connection.close();
            }
        } catch (ClassNotFoundException | SQLException | IOException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
            return object;
    }
    
    public boolean arregloIncidenciaEmpleado(int idIncidencia, String descripcionArregloIncidencia, String nombreImagen){
        boolean arreglo_aceptado = true;
        int idImagenArreglo = 0;
        
        try {
            Class.forName(driver);  
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                String insertImagen = "INSERT INTO `imagen`(`id`, `imagen`) VALUES (NULL,?)";
                try (PreparedStatement psInsertarImagen = (PreparedStatement) connection.prepareStatement(insertImagen)) {
                    psInsertarImagen.setString(1,nombreImagen);
                    long nImagen = psInsertarImagen.executeUpdate();
                    
                    if(nImagen>0){
                        
                        String selectImagen = "SELECT id FROM `imagen` WHERE `imagen` = ?";
                        ResultSet resultImagen;
                        try (PreparedStatement psConsultaImagen = (PreparedStatement) connection.prepareStatement(selectImagen)) {
                            psConsultaImagen.setString(1,nombreImagen);
                            resultImagen = psConsultaImagen.executeQuery();
                            if(resultImagen.next()){
                                idImagenArreglo = resultImagen.getInt(1);
                            }else{
                                arreglo_aceptado = false;
                            }
                        }
                        resultImagen.close();
                        
                    }else{
                        arreglo_aceptado = false; 
                    }
                }
                
                if(!arreglo_aceptado){
                    return arreglo_aceptado;
                }
                
                
                
                String consultInsert="INSERT INTO `estadoincidencia`(`id`, `fecha`, `descripcion`, `idIncidencia`, `idEstado`, `codImagen`) \n" +
                        "VALUES (null,?,?,?,?,?);";
                try (PreparedStatement psConsultInsert = (PreparedStatement) connection.prepareStatement(consultInsert)) {
                    myDate = new Date();
                    fecha = new SimpleDateFormat("yyyy-MM-dd").format(myDate);
                    psConsultInsert.setString(1,fecha);
                    psConsultInsert.setString( 2, descripcionArregloIncidencia);
                    psConsultInsert.setInt(3,idIncidencia);
                    psConsultInsert.setInt(4,5);
                    psConsultInsert.setInt(5,idImagenArreglo);
                    
                    int resultInsert = psConsultInsert.executeUpdate();
                    
                    if(resultInsert<1){
                        arreglo_aceptado = false;
                    }
                }
            connection.close();
            }
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        return arreglo_aceptado;
    }
    
    public String validacionArregloIncidenciaSupervisor(int idIncidencia, String opcion, String descripcion){
        String respuesta="error";
        int codImagen = 0;
        
        try {
            Class.forName(driver);  
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                if(opcion.equals("validar_incidencia")){
                    
                    String selecCodImg="select codImagen from estadoincidencia where idIncidencia=? and idEstado=5";
                    ResultSet resultI;
                    try (PreparedStatement psSelectCodImg = (PreparedStatement) connection.prepareStatement(selecCodImg)) {
                        psSelectCodImg.setInt(1,idIncidencia);
                        resultI = psSelectCodImg.executeQuery();
                        if(resultI.next()){
                            codImagen = resultI.getInt(1);
                        }
                    }
                    resultI.close();
                    String consultInsert="INSERT INTO `estadoincidencia`(`id`, `fecha`, `descripcion`, `idIncidencia`, `idEstado`, `codImagen`) VALUES (null,?,?,?,?,?)";
                    try (PreparedStatement psConsultInsert = (PreparedStatement) connection.prepareStatement(consultInsert)) {
                        myDate = new Date();
                        fecha = new SimpleDateFormat("yyyy-MM-dd").format(myDate);
                        psConsultInsert.setString(1,fecha);
                        psConsultInsert.setNull( 2, java.sql.Types.NULL );
                        psConsultInsert.setInt(3,idIncidencia);
                        psConsultInsert.setInt(4,6);
                        
                        psConsultInsert.setInt(5,codImagen);
                        
                        int resultInsert = psConsultInsert.executeUpdate();
                        
                        if(resultInsert>0){
                            respuesta="validadaArregloOk";
                        }
                    }
                    
                }else{
                    String consultUpdate = "UPDATE estadoincidencia\n" +
                            "SET descripcion=?" +
                            "WHERE idIncidencia=? AND idEstado=4";
                    PreparedStatement psConsultDelete;
                    try (PreparedStatement psConsultUpdate = (PreparedStatement) connection.prepareStatement(consultUpdate)) {
                        psConsultUpdate.setString(1,descripcion);
                        psConsultUpdate.setInt(2,idIncidencia);
                        String consultDelete = "DELETE FROM estadoincidencia\n" +
                                "WHERE idIncidencia = ? AND idEstado = 5";
                        psConsultDelete = (PreparedStatement) connection.prepareStatement(consultDelete);
                        psConsultDelete.setInt(1,idIncidencia);
                        int resultUpdate = psConsultUpdate.executeUpdate();
                        int resultDelete = psConsultDelete.executeUpdate();
                        if(resultUpdate>0 && resultDelete>0){
                            respuesta="denegadaArregloOk";
                        }
                    }
                    psConsultDelete.close();
                }
                connection.close();
            }
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        return respuesta;
    } 
    
    public synchronized boolean modificarUsuarioMovil(String correo, String contrasena, String nombre, String apellidos, String dni, int tlf){
        boolean modificarUsuario = false;
        try {
            
            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url,user,password)) {
                String update = "UPDATE `usuario` \n" +
                        "SET `contrasena`= ?, `nombre`= ?,`apellido`= ?,`dni`= ?,`tlf`= ?\n" +
                        "WHERE `correo` = ?";
                
                try (PreparedStatement psUpdate = (PreparedStatement) connection.prepareStatement(update)) {
                    psUpdate.setString(1,contrasena);
                    psUpdate.setString(2,nombre);
                    psUpdate.setString(3,apellidos);
                    psUpdate.setString(4,dni);
                    psUpdate.setInt(5,tlf);
                    psUpdate.setString(6, correo);
                    
                    long n = psUpdate.executeUpdate();
                    
                    if(n>0){
                        modificarUsuario = true;
                    }
                }
                connection.close();
            }            
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
        return modificarUsuario;   
    }
    
    //      COMPARTIDO POR AMBAS APPS
    public synchronized boolean registrarUsuario(String correo, String contrasena, String nombre, String apellido, String dni, int tlf, String departamento, int tipoUsu) {
        boolean registrarUsuario = false;
        try {

            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url, user, password)) {
                
                //con esta consulta comprobaremos si este usuario ha sido registrado previamente y esta dado de baja
                //si es así, solo debemos volver a darle de alta y actualizar sus datos
                String consultaUsuarioBaja = "SELECT correo, dni FROM usuario WHERE correo=? AND dni=? AND tipoUsuario=? AND activo=false";
                ResultSet resultConsultaUsuarioBaja;
                try (PreparedStatement psConsultaUsuarioBaja = (PreparedStatement) connection.prepareStatement(consultaUsuarioBaja)){
                    psConsultaUsuarioBaja.setString(1, correo);
                    psConsultaUsuarioBaja.setString(2, dni);
                    psConsultaUsuarioBaja.setInt(3, tipoUsu);
                    resultConsultaUsuarioBaja = psConsultaUsuarioBaja.executeQuery();
                    //si encontramos un usuario ya registrado y dado de baja
                    //volvemos a darle de alta
                    if(resultConsultaUsuarioBaja.next()){
                        String darAltaUsuario = "UPDATE `usuario` \n" +
                                                    "SET `contrasena`=?, `nombre`=?,`apellido`=?,`tlf`=?,`idDepartamento`=?,`activo`=true \n" +
                                                    "WHERE correo=?";
                        
                        try(PreparedStatement psDarAltaUsuario = (PreparedStatement) connection.prepareStatement(darAltaUsuario)){
                            psDarAltaUsuario.setString(1, contrasena);
                            psDarAltaUsuario.setString(2, nombre);
                            psDarAltaUsuario.setString(3, apellido);
                            psDarAltaUsuario.setInt(4, tlf);
                            if (departamento.equals("NULL")) {
                                psDarAltaUsuario.setNull(5, java.sql.Types.NULL);
                            } else {
                                psDarAltaUsuario.setInt(5, Integer.parseInt(departamento));
                            }
                            psDarAltaUsuario.setString(6, correo);
                            
                            long z = psDarAltaUsuario.executeUpdate();

                            if (z > 0) {
                                System.out.println("Usuario dado de alta.");
                                registrarUsuario = true;
                            } else {
                                System.out.println("Usuario no dado de alta.");
                            }
                            
                        }
                    }
                }
                resultConsultaUsuarioBaja.close();
                
                if(registrarUsuario)
                    return registrarUsuario;
                    
                
                String consulta = "SELECT correo, contrasena FROM usuario WHERE correo=?";
                ResultSet result;
                try (PreparedStatement psConsulta = (PreparedStatement) connection.prepareStatement(consulta)) {
                    psConsulta.setString(1, correo);
                    result = psConsulta.executeQuery();
                    //comprobamos si hay algun usuario con ese correo
                    //si es así informamos que ya existe alguien con ese correo
                    //si no es así lo registraremos en la base de datos
                    if (!result.next()) {

                        String insertar = "INSERT INTO `usuario`(`correo`, `contrasena`, `nombre`, `apellido`, `dni`, `tlf`, `idDepartamento`, `tipoUsuario`, `activo`) VALUES (?,?,?,?,?,?,?,?,true)";
                        try (PreparedStatement psInsertar = (PreparedStatement) connection.prepareStatement(insertar)) {
                            psInsertar.setString(1, correo);
                            psInsertar.setString(2, contrasena);
                            psInsertar.setString(3, nombre);
                            psInsertar.setString(4, apellido);
                            psInsertar.setString(5, dni);
                            psInsertar.setInt(6, tlf);

                            if (departamento.equals("NULL")) {
                                psInsertar.setNull(7, java.sql.Types.NULL);
                            } else {
                                psInsertar.setInt(7, Integer.parseInt(departamento));
                            }

                            psInsertar.setInt(8, tipoUsu);
                            long n = psInsertar.executeUpdate();

                            if (n > 0) {
                                System.out.println("Usuario registrado.");
                                registrarUsuario = true;
                            } else {
                                System.out.println("Usuario no registrado.");
                            }
                        }

                    }
                }
                result.close();
                connection.close();
            }

        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }

        return registrarUsuario;
    }

    public JSONObject obtenerDetallesUsuario(String correoUsuario){
        JSONObject obj = null;

        try {
            Class.forName(driver);
            try (Connection connection = (Connection) DriverManager.getConnection(url, user, password)) {
                String consulta = "SELECT u.correo, u.contrasena, u.nombre, u.apellido, u.dni, u.tlf, d.nombre \n" +
                            "FROM usuario u LEFT JOIN departamento d ON (u.idDepartamento=d.id)\n" +
                            "WHERE correo = ?;";
                
                ResultSet resultConsultaUsuario;
                try (PreparedStatement psConsultaIncidencias = (PreparedStatement) connection.prepareStatement(consulta)) {
                    
                    psConsultaIncidencias.setString(1, correoUsuario);
                    
                    resultConsultaUsuario = psConsultaIncidencias.executeQuery();
                    String correo, contrasena, nombre, apellido, dni, departamento;
                    int tlf;
                    while (resultConsultaUsuario.next()) {
                        obj = new JSONObject();
                        correo = resultConsultaUsuario.getString(1);
                        obj.put("correo", correo);
                        contrasena = resultConsultaUsuario.getString(2);
                        obj.put("contrasena", contrasena);
                        nombre = resultConsultaUsuario.getString(3);
                        obj.put("nombre", nombre);
                        apellido = resultConsultaUsuario.getString(4);
                        obj.put("apellido", apellido);
                        dni = resultConsultaUsuario.getString(5);
                        obj.put("dni", dni);
                        tlf = resultConsultaUsuario.getInt(6);
                        obj.put("tlf", tlf);
                        departamento = resultConsultaUsuario.getString(7);
                        obj.put("departamento", departamento);
                    }
                }
                resultConsultaUsuario.close();
                connection.close();
            }

        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ControlGestion.class.getName()).log(Level.SEVERE, null, ex);
        }

        return obj;
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
