# Use the official Tomcat image as a base
FROM tomcat:9.0

# Copy the WAR file to the webapps directory of Tomcat
COPY target/HFCustomerPortalServer-0.0.1-SNAPSHOT.war /usr/local/tomcat/webapps/HFCustomerPortalServer.war

# Expose the default Tomcat port
EXPOSE 8080

RUN mkdir -p /var/www/images/document_picture/

# Start Tomcat server
CMD ["catalina.sh", "run"]