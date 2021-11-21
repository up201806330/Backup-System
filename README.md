# Distributed Backup Service

- **Project name:** Distributed Backup Service
- **Short description:** Simple distributed Backup Service across local network peers
- **Environment:** Unix / Windows Console
- **Tools:** Java, RMI
- **Institution:** [FEUP](https://sigarra.up.pt/feup/en/web_page.Inicial)
- **Course:** [SDIS](https://sigarra.up.pt/feup/en/UCURR_GERAL.FICHA_UC_VIEW?pv_ocorrencia_id=459489) (Distributed Systems)
- **Project grade:** 18.6/20
- **Group members:**
    - [João António Cardoso Vieira e Basto de Sousa](https://github.com/JoaoASousa) ([up201806613@fe.up.pt](up201806613@fe.up.pt))
    - [Rafael Soares Ribeiro](https://github.com/up201806330) ([up201806330@fe.up.pt](mailto:up201806330@fe.up.pt))

## Compile

```
cd build
javac ../src/*.java ../src/sdis/*.java ../src/sdis/*/*.java -cp ../src -d .
```

## Run

```
cd build
java PeerDriver VERSION PEER_ID SERVICE_ACCESS_POINT MC MC_PORT MDB MDB_PORT MDR MDR_PORT
```

For info on arguments, run `java PeerDriver`

If the PeerDriver process is killed, it will exit gracefully and unregister itself from RMI.
