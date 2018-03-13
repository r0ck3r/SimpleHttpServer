# SimpleHttpServer

Is a tiny HTTP-server, that supports file uploading and have user-friendly interface to access to GET and POST parameters and uploaded files

#### Using SSL certificates from letsencrypt
1) Obtain certificates with acme.sh (availavle here: https://github.com/Neilpang/acme.sh)
2) Go to <home_folder>/.acme.sh/<your_domain>
3) Run this:
```bash
openssl pkcs12 -export -in fullchain.cer -inkey <your_domain>.key -out keystore.p12 -name tomcat -CAfile ca.cer -caname root
```
4) Enter password for keystore
5) Set path to created keystore.p12 and keystore password to setJKSKey method:
```java
ServerSettings serverSettings = ServerSettings.createDefaultConfig();
serverSettings.setJKSKey("/home/user/.acme.sh/example.com/keystore.p12", "storepass")
```
