# SDIS Project

SDIS Project for group T7G01

Group members:

1. João Sousa (up201806613@edu.fe.up.pt)
2. Rafael Ribeiro (up201806330@edu.fe.up.pt)

## Compiling
Simply run `./compile.sh` inside the root of the directory to compile the project into the `build` folder.

To start a **Peer**, run `./peer.sh <version> <peer_id> <svc_access_point> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port>` inside the `build`folder.

To run a **protocol command**, run `./test <peer_ap> BACKUP|RESTORE|DELETE|RECLAIM|STATE [<opnd_1> [<optnd_2]]` inside the `build`folder.

To cleanup the directory tree for storing both the chunks and the restored files of either a single peer or all peers, run `./cleanup [<peer_id>]` inside the `build`folder.

These last three scripts can also be ran inside the root directory using the same command, to facilitate testing.