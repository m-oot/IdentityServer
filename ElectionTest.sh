rm -rf identity-515*
java -jar target/DebugServer.jar &
java -jar target/IdServer.jar --numport 5156 --dbsh localhost --dbsp 5170 --verbose &
sleep 1
java -jar target/IdServer.jar --numport 5157 --dbsh localhost --dbsp 5170 --verbose &
sleep 1
java -jar target/IdServer.jar --numport 5158 --dbsh localhost --dbsp 5170 --verbose &
sleep 1
java -jar target/IdClient.jar --dbsh localhost --dbsp 5170 --create John
java -jar target/IdClient.jar --dbsh localhost --dbsp 5170 -l John
java -jar target/IdClient.jar --dbsh localhost --dbsp 5170 -d John
java -jar target/IdClient.jar --dbsh localhost --dbsp 5170 --numport 5156 --kill
java -jar target/IdClient.jar --dbsh localhost --dbsp 5170 -l John



