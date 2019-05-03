import javax.xml.crypto.Data;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Resub {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        String website = "ftp.dlptest.com";

        Socket sock = new Socket(website, 21);

        DataOutputStream os = new DataOutputStream(sock.getOutputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));

        getReturnCode(br);

        // Connects to server
        send(os, br, "USER dlpuser@dlptest.com");
        send(os, br, "PASS 5p2tvn92R0di8FdiLCfzeeT0b");
        Socket dataSock = createDataSocket(os, br, website);

        boolean active = true;
        String input, replycode;
        do {

            System.out.println(
                    "\n1. Show current directory\t" + "5. Upload file\n" +
                            "2. Change directory\t\t\t" + "6. Set file transfer type (BETA)\n" +
                            "3. Create directory\t" + "\t\t7. QUIT\n" +
                            "4. Download file"
            );
            input = sc.nextLine();

            if (dataSock == null){
                dataSock = createDataSocket(os, br, website);
            }
            switch (input) {
                case "1":
                    showDirectory(os, br, dataSock);
                    dataSock = null;
                    break;
                case "2":
                    System.out.println("Name of directory (leave blank to go back):");
                    input = sc.nextLine();
                    changeDirectory(os, br, input);
                    break;

                case "3":
                    System.out.println("Name of new directory:");
                    input = sc.nextLine();
                    createDirectory(os, br, input);
                    break;

                case "4":
                    System.out.println("Name of file to download:");
                    input = sc.nextLine();
                    downloadFile(os, br,  dataSock, input);
                    dataSock = null;
                    break;

                case "5":
                    System.out.println("Name of file to upload:");
                    input = sc.nextLine();
                    uploadFile(os, br, dataSock, input);
                    dataSock = null;
                    break;

                case "6":
                    System.out.println("Choose between ASCII (A) or image (I):");
                    input = sc.nextLine();
                    changeTransferType(os, br, input);
                    break;

                case "7":
                    System.out.println("Quitting...");
                    active = false;
                    break;
            }


        } while(active);
    }

    public static void changeTransferType(DataOutputStream os, BufferedReader br, String input) throws IOException {
        if (!input.equalsIgnoreCase("A") && !input.equalsIgnoreCase("I"))
            System.out.println("Incorrect type");
        else
            send(os, br, "TYPE " + input);
    }

    public static Socket createDataSocket(DataOutputStream os, BufferedReader br, String web) throws IOException {
        send(os, "PASV");
        String response = br.readLine();
        System.out.println(response);

        while (!response.substring(0,3).equals("227")){
            response = br.readLine();
            //System.out.println(response);
        }

        String[] ipPort = response.substring(27,response.length()-1).split(",");
        int port = Integer.parseInt(ipPort[4]) * 256 + Integer.parseInt(ipPort[5]);
        return new Socket(web, port);
    }

    public static void uploadFile(DataOutputStream os, BufferedReader br, Socket dataSock, String fileName) throws IOException {
        File file = new File(fileName);
        if(file.exists()) {

            DataOutputStream dataOS = new DataOutputStream(dataSock.getOutputStream());
            String replycode = send(os, br, "STOR " + fileName);
            if (replycode.substring(0, 3).equals("150")) {
                FileInputStream fileIS = new FileInputStream(file);
                BufferedReader fileBR = new BufferedReader(new InputStreamReader(fileIS));


                String line = fileBR.readLine();
                while (line != null) {
                    dataOS.writeBytes(line + "\n");
                    line = fileBR.readLine();
                }

            /*
            int next = fileIS.read();
            while(next != -1) {
                dataOS.writeBoolean(Integer.toBinaryString(next));
                next = fileIS.read();
            }
            */

                dataOS.close();
                getReturnCode(br);
            }
        } else{
            System.out.println("Error: No such file exists");
        }
    }

    public static void downloadFile(DataOutputStream os, BufferedReader br, Socket dataSock, String fileName) throws IOException {
        String replycode = send(os, br, "RETR " + fileName);
        StringBuilder sb = new StringBuilder();

        if (replycode.equals("150")) {
            InputStreamReader dataReader = new InputStreamReader(dataSock.getInputStream());
            File file = new File(fileName);
            FileOutputStream fileOS = new FileOutputStream(file);
            int counter = 0;
            char next;
            do {
                next = (char) dataReader.read();
                if (counter < 125){
                    sb.append(next);
                    counter++;
                }
                fileOS.write(next);
            } while (dataReader.ready());
            fileOS.close();
            getReturnCode(br);

            System.out.println("\nFile downloaded. Preview of the first Kb:");
            System.out.println(sb.toString());
        }
    }

    public static void createDirectory(DataOutputStream os, BufferedReader br, String name) throws IOException {
        send(os, br, "MKD " + name);
    }

    public static void showDirectory(DataOutputStream os, BufferedReader br, Socket dataSock) throws IOException {
        send(os, "LIST");
        BufferedReader dataBR = new BufferedReader(new InputStreamReader(dataSock.getInputStream()));
        getReturnCode(dataBR);
    }

    public static void changeDirectory(DataOutputStream os, BufferedReader br, String name) throws IOException {
        send(os, br, "CWD " + name);
    }

    public static String getReturnCode(BufferedReader br) throws IOException {
        String reply = null;
        do{
            reply = br.readLine();
            System.out.println(reply);
        }while(br.ready());
        if (reply == null){
            return null;
        }
        return reply.substring(0,3);
    }

    public static int getFileSize(DataOutputStream os, BufferedReader br, String fileName) throws IOException {
        send(os, "SIZE " + fileName);
        String reply = br.readLine();
        String size = "-1";
        if (reply.substring(0,3).equals("213")){
            size = reply.substring(4,7);
        }

        while(br.ready()){
            System.out.println(br.readLine());
        }

        return Integer.parseInt(size);
    }

    public static String send(DataOutputStream os, BufferedReader br, String msg) throws IOException {
        os.writeBytes(msg + "\r\n");
        return getReturnCode(br);
    }

    public static void send(DataOutputStream os, String msg) throws IOException {
        os.writeBytes(msg + "\r\n");
    }
}
