rm -rvf ~/Documents/apache-tomcat-9.0.70/webapps/image-converter-service-1.0.2
ps -aef | grep cata | grep -v grep | awk '{print $2}' | xargs kill -9
cp target/image-converter-service-1.0.2.war ~/Documents/apache-tomcat-9.0.70/webapps/
~/Documents/apache-tomcat-9.0.70/bin/catalina.sh jpda start
