# API HealthBlood - Tomcat 11 + WAR (Jakarta EE 11 / Jersey 4)
# Se usa Tomcat 11 porque web.xml declara Servlet 6.1 y Jersey 4 no arranca en Tomcat 10.

FROM tomcat:11.0-jdk17-temurin

# Limpiar las aplicaciones de ejemplo que trae la imagen oficial
RUN rm -rf /usr/local/tomcat/webapps/*

# El WAR se despliega como aplicacion raiz:
#   https://TU-SERVICIO.onrender.com/api/glucosa/saludar
COPY dist/API_HealthBlood.war /usr/local/tomcat/webapps/ROOT.war

# Tomcat escucha en 8080. En Render hay que crear la variable de entorno PORT=8080
# porque Render espera el puerto 10000 por defecto.
EXPOSE 8080

CMD ["catalina.sh", "run"]
