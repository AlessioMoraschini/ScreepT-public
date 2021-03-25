Utilitu updater.jar -> Usage:

1A- [serve FTP.properties] java -jar updater.jar -generateHashes-mainRelease true manifest.properties
-> questo comando serve per aggiornare nel file manifest.properties gli hash del file zip specificato nel file di properties, 
con chiave "RELEASE_FOLDER-OR-ZIP_TO_ELABORATE". se il primo parametro è true effettua anche la creazione dello zip, comprimendo la cartella specificato nel FTP.properties alla stessa chiave, e poi aggiorna l'hash con quello del file generato.

1B- [serve FTP.properties]  java -jar updater.jar -generateHashes-uploadFtpUpdates true manifest.properties
Come 1A, ma in più effettua il caricamento tramite FTPS su server dei file (updates.zip + updater.jar + manifest.properties). Parametri di connessione specificati nel file FTP.properties.



2A- java -jar updater.jar -generateHashes false manifest.properties
-> Questo comando serve per aggiornare gli hash dei file da caricare (updates.zip + updater.jar + manifest.properties) nel file manifest.properties

2B- java -jar updater.jar -generateHashes true manifest.properties
Come 2A, ma il parametro "true" indica di creare un file updates.zip con il contenuto della cartella updates prima di effettuare l'aggiornamento degli hash nel file di properties. 

2C- [serve FTP.properties]  java -jar updater.jar -generateHashes-uploadFtpUpdates true manifest.properties
Come 2A/2B a seconda del parametro true/false, ma in più effettua il caricamento tramite FTPS su server dei file (updates.zip + updater.jar + manifest.properties). Parametri di connessione specificati nel file FTP.properties.








################ ESEMPIO - FTP.properties  ################

HOST=ftp.yourwebsite.com
USER=XXXXXX@aruba.it
PSW=XXX
PORT=22
FOLDER_ON_SERVER=/www.am-design-development.com/ScreepT/Release
RELEASE_FOLDER-OR-ZIP_TO_ELABORATE=C\:/Users/xx/Desktop/Documenti&Utili/Informatica varie/ScreepT_Build/ScreepT_V0.9.32