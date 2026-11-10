package org.lasalle.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Conexion a MySQL.
 *
 * Los datos de conexion se leen de variables de entorno, asi la misma aplicacion
 * funciona en local (NetBeans) y en la nube (Render + Aiven) sin recompilar nada.
 *
 * Variables que entiende:
 *   DB_HOST      host del servidor MySQL          (local: 127.0.0.1)
 *   DB_PORT      puerto                           (local: 3306, Aiven: el de tu servicio)
 *   DB_NAME      nombre de la base                (local: healthblood)
 *   DB_USER      usuario                          (local: root, Aiven: avnadmin)
 *   DB_PASSWORD  password                         (local: root)
 *   DB_PARAMS    parametros extra de la URL JDBC  (Aiven: ssl-mode=REQUIRED)
 */
public class ConnectionMysql {

    private Connection conn;

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    /**
     * Arma los parametros de la URL JDBC.
     *
     * Los parametros base sirven en cualquier entorno. DB_PARAMS los agrega o
     * los sobreescribe (separados por "&"), por ejemplo:
     *   DB_PARAMS=ssl-mode=REQUIRED
     *
     * Se usa un Map para que no queden parametros duplicados en la URL, cosa
     * que Connector/J rechaza cuando entran en conflicto.
     */
    private static String buildParams() {
        Map<String, String> params = new LinkedHashMap<>();

        // Base: compatibles con MySQL local y con cualquier proveedor cloud.
        params.put("allowPublicKeyRetrieval", "true");
        params.put("useUnicode", "true");
        params.put("characterEncoding", "utf-8");

        // Timeouts: en la nube la base esta al otro lado de internet.
        // Sin esto, una red lenta deja la peticion HTTP colgada.
        params.put("connectTimeout", "10000");  // 10 s para conectar
        params.put("socketTimeout", "30000");   // 30 s maximo por consulta

        // Overrides / extras. En Aiven: ssl-mode=REQUIRED
        String extra = env("DB_PARAMS", "");
        if (!extra.isBlank()) {
            for (String pair : extra.replaceFirst("^\\?", "").split("&")) {
                if (pair.isBlank()) {
                    continue;
                }
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    params.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
                } else {
                    params.put(pair.trim(), "");
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(e.getKey());
            if (!e.getValue().isEmpty()) {
                sb.append('=').append(e.getValue());
            }
        }
        return sb.toString();
    }

    public Connection open() {
        String host     = env("DB_HOST", "127.0.0.1");
        String port     = env("DB_PORT", "3306");
        String dbName   = env("DB_NAME", "healthblood");
        String user     = env("DB_USER", "root");
        String password = env("DB_PASSWORD", "root");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                   + "?" + buildParams();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(url, user, password);
            return conn;
        } catch (Exception e) {
            // Loguea la URL sin credenciales para poder diagnosticar en Render
            System.err.println("[ConnectionMysql] Fallo la conexion a " + host + ":" + port + "/" + dbName);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void close() {
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
