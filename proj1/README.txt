dentro da pasta bin:

1º terminal:
rmiregistry

2º terminal:
javac -d . ../src/*.java <- para compilar

java Peer 1.0 1 Peer1 230.0.0.1 8888 230.0.0.2 8888 230.0.0.3 8888 <- para iniciar Peer1

3º terminal:
java Peer 1.0 2 Peer2 230.0.0.1 8888 230.0.0.2 8888 230.0.0.3 8888 <- para iniciar Peer2

4º terminal:
java TestApp Peer1 BACKUP testing.txt 1
ou
java TestApp Peer1 BACKUP test.pdf 1


------------------------

./peer.sh 1.0 1 Peer1 230.0.0.1 8888 230.0.0.2 8888 230.0.0.3 8888
./peer.sh 1.0 2 Peer2 230.0.0.1 8888 230.0.0.2 8888 230.0.0.3 8888
./test.sh Peer1 BACKUP ../files/testing.txt 1
./test.sh Peer1 DELETE ../files/testing.txt


