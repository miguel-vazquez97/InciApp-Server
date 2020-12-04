--
-- Base de datos: `inciappdatabase`
--
CREATE DATABASE IF NOT EXISTS inciappdatabase CHARACTER SET utf8 COLLATE utf8_general_ci;

USE inciappdatabase;
-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `departamento`
--

CREATE TABLE `departamento` (
  `id` int(2) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `nombre` varchar(50) NOT NULL
);

--
-- Volcado de datos para la tabla `departamento`
--

INSERT INTO `departamento` (`id`, `nombre`) VALUES
(1, 'Electricidad'),
(2, 'Medio Ambiente'),
(3, 'Fontanería'),
(4, 'Urbanismo'),
(5, 'Tráfico');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `tipoincidencia`
--

CREATE TABLE `tipoincidencia` (
  `id` int(2) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `nombre` varchar(50) NOT NULL,
  `idDepartamento` int(2) NOT NULL,
  CONSTRAINT `ti_dep_fk` FOREIGN KEY (`idDepartamento`) REFERENCES `departamento` (`id`) ON UPDATE CASCADE ON DELETE CASCADE
);

--
-- Volcado de datos para la tabla `tipoincidencia`
--

INSERT INTO `tipoincidencia` (`id`, `nombre`, `idDepartamento`) VALUES
(1, 'Alumbrado', 1),
(2, 'Parques y Jardines', 2),
(3, 'Fuentes de Agua', 3),
(4, 'Alcantarillado', 3),
(5, 'Mobiliario Urbano', 4),
(6, 'Calzado y Acera', 4),
(7, 'Señales y Semáforos', 5);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `estado`
--

CREATE TABLE `estado` (
  `id` int(2) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `titulo` varchar(50) NOT NULL,
  `descripcion` varchar(250) NOT NULL
);

--
-- Volcado de datos para la tabla `estado`
--

INSERT INTO `estado` (`id`, `titulo`, `descripcion`) VALUES
(1, 'NuevaRegistrada', 'Una nueva incidencia ha sido registrada por un ciudadano. '),
(2, 'EnTramite', 'Se ha enviado un Supervisor para verificar la incidencia.'),
(3, 'Validada', 'La incidencia ha sido validada por un Supervisor y pasará a ser arreglada.'),
(4, 'EnArreglo', 'Se han enviado empleados para que sea arreglada la incidencia.'),
(5, 'ValidarArreglo', 'El Supervisor se desplazará para verificar que ha sido arreglada correctamente la incidencia.'),
(6, 'Arreglada', 'Supervisor ha dado su aprobación al arreglo de la incidencia.'),
(7, 'Solucionada', 'El Administrados ha dado por solucionada la incidencia.'),
(8, 'Denegada', 'El Supervisor ha estimado que no existe tal incidencia.');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `imagen`
--

CREATE TABLE `imagen` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `imagen` varchar(50) NOT NULL
);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `tipousuario`
--

CREATE TABLE `tipousuario` (
  `id` int(3) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `tipo` varchar(25) NOT NULL
);

--
-- Volcado de datos para la tabla `tipousuario`
--

INSERT INTO `tipousuario` (`id`, `tipo`) VALUES
(1, 'Administrador'),
(2, 'Supervisor'),
(3, 'Empleado'),
(4, 'Ciudadano');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `usuario`
--

CREATE TABLE `usuario` (
  `correo` varchar(100) NOT NULL PRIMARY KEY,
  `contrasena` varchar(50) NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `apellido` varchar(100) NOT NULL,
  `dni` varchar(9) NOT NULL,
  `tlf` int(9) NOT NULL,
  `idDepartamento` int(2) DEFAULT NULL,
  `tipoUsuario` int(3),
  `activo` boolean,
  CONSTRAINT `us_dep_fk` FOREIGN KEY (`idDepartamento`) REFERENCES `departamento` (`id`) ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT `us_tu_fk` FOREIGN KEY (`tipoUsuario`) REFERENCES `tipousuario` (`id`) ON UPDATE CASCADE ON DELETE SET NULL
);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `incidencia`
--

CREATE TABLE `incidencia` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `ubicacion` varchar(50) NOT NULL,
  `direccion` varchar(250) NOT NULL,
  `descripcion` varchar(250) NOT NULL,
  `idTipo` int(3),
  `usuarioCiudadano` varchar(100),
  `usuarioAdministrador` varchar(100) DEFAULT NULL,
  `usuarioSupervisor` varchar(100) DEFAULT NULL,
  `usuarioEmpleado` varchar(100) DEFAULT NULL,
  CONSTRAINT `in_ti_fk` FOREIGN KEY (`idTipo`) REFERENCES `tipoincidencia` (`id`) ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT `in_uciud_fk` FOREIGN KEY (`usuarioCiudadano`) REFERENCES `usuario` (`correo`) ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT `in_uadmin_fk` FOREIGN KEY (`usuarioAdministrador`) REFERENCES `usuario` (`correo`) ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT `in_usuper_fk` FOREIGN KEY (`usuarioSupervisor`) REFERENCES `usuario` (`correo`) ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT `in_uemple_fk` FOREIGN KEY (`usuarioEmpleado`) REFERENCES `usuario` (`correo`) ON UPDATE CASCADE ON DELETE SET NULL
);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `estadoincidencia`
--

CREATE TABLE `estadoincidencia` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `fecha` date NOT NULL,
  `descripcion` varchar(250) DEFAULT NULL,
  `idIncidencia` int(11) NOT NULL,
  `idEstado` int(2) NOT NULL,
  `codImagen` int(11) DEFAULT NULL,
  CONSTRAINT `ei_in_fk` FOREIGN KEY (`idIncidencia`) REFERENCES `incidencia` (`id`) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `ei_es_fk` FOREIGN KEY (`idEstado`) REFERENCES `estado` (`id`) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `ei_im_fk` FOREIGN KEY (`codImagen`) REFERENCES `imagen` (`id`) ON UPDATE CASCADE ON DELETE SET NULL
);