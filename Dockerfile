# API HealthBlood - Tomcat 11 + WAR (Jakarta EE 11 / Jersey 4)
# Se usa Tomcat 11 porque web.xml declara Servlet 6.1 y Jersey 4 no arranca en Tomcat 10.

FROM tomcat:11.0-jdk17-temurin

# Limpiar las aplicaciones de ejemplo que trae la imagen oficial
RUN rm -rf /usr/local/tomcat/webapps/*

# El WAR se despliega como aplicacion raiz:
#   https://TU-SERVICIO.onrender.com/api/glucosa/saludar
COPY dist/API_HealthBlood.war /usr/local/tomcat/webapps/ROOT.war

# En Render (y en la mayoria de PaaS) el puerto llega en la variable PORT.
# Tomcat trae 8080 fijo en server.xml, asi que al arrancar lo reemplazamos
# por el valor de PORT. Si PORT no existe (Docker en tu PC), queda 8080.
EXPOSE 8080

CMD ["sh", "-c", "sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT:-8080}\\\"/\" /usr/local/tomcat/conf/server.xml && exec catalina.sh run"]
