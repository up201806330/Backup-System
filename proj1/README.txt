**Built with Java 14**

./peer.sh 1.0 1 Peer1 230.0.0.1 8888 230.0.0.2 8888 230.0.0.3 8888
./peer.sh 1.0 2 Peer2 230.0.0.1 8888 230.0.0.2 8888 230.0.0.3 8888
./test.sh Peer1 BACKUP ../../files/testing.txt 1
./test.sh Peer1 DELETE ../../files/testing.txt
