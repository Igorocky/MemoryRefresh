export KEYSTORE_FILE_NAME='tagged-notes-ktor'
export JKS_KEYSTORE_PATH="./$KEYSTORE_FILE_NAME.jks"
export BKS_KEYSTORE_PATH="./$KEYSTORE_FILE_NAME.bks"
export ALIAS='ktor'
export PASSWD=''

#generate a JKS
keytool -genkey -keyalg RSA -alias $ALIAS -keystore $JKS_KEYSTORE_PATH -validity 365 -keysize 2048

#convert the JKS to BKS format
keytool -importkeystore -alias $ALIAS -srckeystore $JKS_KEYSTORE_PATH -srcstoretype JKS \
  -srcstorepass $PASSWD -storepass $PASSWD \
  -deststoretype BKS -providerpath './bcprov-jdk15on-169.jar' \
  -provider org.bouncycastle.jce.provider.BouncyCastleProvider -destkeystore $BKS_KEYSTORE_PATH

#check the BKS store
keytool -list -v -keystore $BKS_KEYSTORE_PATH \
  -provider org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath './bcprov-jdk15on-169.jar' \
  -storetype BKS -storepass $PASSWD