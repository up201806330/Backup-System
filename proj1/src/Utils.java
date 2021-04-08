public class Utils {

    public static void printSplitHeader(String[] splitHeader) {
        System.out.println("Length of split Header: " + splitHeader.length);
        System.out.println("Version : " + splitHeader[0]);
        System.out.println("Command : " + splitHeader[1]);
        System.out.println("SenderId: " + splitHeader[2]);
        System.out.println("FileId  : " + splitHeader[3]);
        System.out.println("ChunkNr : " + splitHeader[4]);
        System.out.println("RepDegr : " + splitHeader[5]);
    }
}
