package Server;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.nio.file.FileAlreadyExistsException;

/**
 * Created by amattis
 */
public class Server extends Thread {
    private final int GET_OK = 200;
    private final int AUTH_SUCCESS = 235;
    private final int SYSTEM_READY = 250;
    private final int MSG_FAIL = 265;
    private final int MSG_FAIL_SUBJ = 266;
    private final int MSG_FAIL_EMPTY = 267;
    private final int MSG_SUCCESS = 275;
    private final int MSG_SAVED = 280;
    private final int AUTH_OK = 330;
    private final int AUTH_RESPONSE = 334;
    private final int AUTH_NO_USER = 335;
    private final int AUTH_NEW_SUCCESS = 337;
    private final int AUTH_NEW_FAIL = 340;
    private final int AUTH_REQUIRED = 530;
    private final int AUTH_DENIED = 535;
    private final int INVALID_RCPT = 550;
    private final int INVALID_PASS = 555;
    private final int INVALID_USER = 560;
    private final int CMD_SYNTAX_ERR = 500;
    private final int ARG_SYNTAX_ERR = 501;
    private final int BAD_CMD_SEQUENCE = 503;

    private String userName;
    private String toAddr;
    private String subject;
    private String clientAddr;
    private String serverAddr;
    private int outboxLength;
    private int inboxLength;

    private ServerSocket serverSock;
    private DatagramSocket udpSock;
    private static Server s;
    private static Date date = new Date();

    public static void main(String [] args) throws InterruptedException, IOException {
        int portNumTCP = 9099;
        int portNumUDP = 9098;

        try{
            s = new Server(portNumTCP, portNumUDP);
        }catch (Exception e) {
            System.err.print("Unable to establish connection on port #" + portNumTCP);
        }

        while(true){
            try{
                Thread t = new Thread(s);
                t.start();
                sleep(10000);
            }
            catch (Exception e){
                System.err.print("Thread failed");
            }
        }
    }

    private Server(int tcp, int udp) throws Exception{
        serverSock = new ServerSocket(tcp);
        udpSock = new DatagramSocket(udp);
        serverSock.setSoTimeout(10000);
    }

    private void writePassToFile(byte[] encodedPass, String user, String fout) throws IOException {
        String newUser = user.replace("@447.edu", "");
        Files.createDirectories(Paths.get("db/" + newUser));
        Files.createDirectories(Paths.get("db/" + newUser + "/inbox"));
        Files.createDirectories(Paths.get("db/" + newUser + "/outbox"));
        Files.createDirectories(Paths.get("db/" + newUser + "/drafts"));
        Files.createDirectories(Paths.get("db/" + newUser + "/contacts"));
        String path = "db/" + newUser + "/";

        try {
            FileOutputStream fos = new FileOutputStream(path + fout);
            fos.write(encodedPass);
            fos.close();
        } catch (FileAlreadyExistsException e) {
            System.out.printf("File '%s%s' already exists.%n", path, fout);
            System.exit(0);
        } catch (IOException ex) {
            System.out.printf("Error writing file '%s%s'%n", path, fout);
            System.exit(0);
        }
    }

    private boolean readPassFile(String fout, byte[] pass, String user) throws IOException {
        Path path = Paths.get(String.format("db/%s/%s", user, fout));
        byte[] check = Files.readAllBytes(path);
        return Arrays.equals(pass, check);
    }

//    private void writeToInbox(StringBuilder message, String fout) throws IOException {
//        String path = String.format("db/%s/inbox/", toAddr);
//        path = path.concat(subject + fout);
//
//        try (FileWriter fWriter = new FileWriter(path);
//             BufferedWriter bWriter = new BufferedWriter(fWriter))
//        {
//            bWriter.write(String.format("Date: %s", date.toString()));
//            bWriter.newLine();
//
//            bWriter.write(String.format("From: %s", userName));
//            bWriter.newLine();
//
//            bWriter.write(String.format("To: %s", toAddr));
//            bWriter.newLine();
//
//            bWriter.write(String.format("Subject: %s", subject));
//            bWriter.newLine();
//
//            bWriter.write(message.toString());
//            bWriter.newLine();
//
//            bWriter.flush();
//            bWriter.close();
//        } catch (FileAlreadyExistsException e) {
//            System.out.printf("File '%s%s' already exists.%n", path, fout);
//            System.exit(0);
//        } catch (IOException ex) {
//            System.out.printf("Error writing file '%s%s'%n", path, fout);
//            System.exit(0);
//        }
//    }
//
//    private void writeToOutbox(StringBuilder message, String fout){
//        String path = String.format("db/%s/outbox/", userName);
//        int logCount = new File(path).listFiles().length;
//        logCount++;
//        path = path.concat(subject + fout);
//
//        try (FileWriter fWriter = new FileWriter(path);
//             BufferedWriter bWriter = new BufferedWriter(fWriter))
//        {
//            bWriter.write(String.format("Date: %s", date.toString()));
//            bWriter.newLine();
//
//            bWriter.write(String.format("From: %s", userName));
//            bWriter.newLine();
//
//            bWriter.write(String.format("To: %s", toAddr));
//            bWriter.newLine();
//
//            bWriter.write(String.format("Subject: %s", subject));
//            bWriter.newLine();
//
//            bWriter.write(message.toString());
//            bWriter.newLine();
//
//            bWriter.flush();
//            bWriter.close();
//        } catch (FileAlreadyExistsException e) {
//            System.out.printf("File '%s%s' already exists.%n", path, fout);
//            System.exit(0);
//        } catch (IOException ex) {
//            System.out.printf("Error writing file '%s%s'%n", path, fout);
//            System.exit(0);
//        }
//    }

    private void writeToDestination(StringBuilder message, String fout, String user, String dest){
        String path = String.format("db/%s/%s/", user, dest);
        path = path.concat(subject + fout);

        switch (dest) {
            case "inbox":
                inboxLength++;
                break;
            case "outbox":
                outboxLength++;
                break;
            default:
                ;
                break;
        }

        try (FileWriter fWriter = new FileWriter(path);
             BufferedWriter bWriter = new BufferedWriter(fWriter))
        {
            bWriter.write(String.format("Date: %s", date.toString()));
            bWriter.newLine();

            bWriter.write(String.format("From: %s", userName));
            bWriter.newLine();

            bWriter.write(String.format("To: %s", toAddr));
            bWriter.newLine();

            bWriter.write(String.format("Subject: %s", subject));
            bWriter.newLine();

            bWriter.write(message.toString());
            bWriter.newLine();

            bWriter.flush();
            bWriter.close();
        } catch (FileAlreadyExistsException e) {
            System.out.printf("File '%s' already exists.%n", path);
            System.exit(0);
        } catch (IOException ex) {
            System.out.printf("Error writing file '%s'%n", path);
            System.exit(0);
        }
    }

    private void saveAsDraft(Socket server, String cmd){
        try{
            DataInputStream dis = new DataInputStream(server.getInputStream());
            DataOutputStream dos = new DataOutputStream(server.getOutputStream());
            StringBuilder message = new StringBuilder();
            toAddr = dis.readUTF();
            subject = dis.readUTF();
            message.append(dis.readUTF());
            String fout = ".draft";
            writeToDestination(message, fout, userName, "drafts");
            message.setLength(0);
            dos.writeUTF(String.format("%d Message saved.", MSG_SAVED));
            dos.flush();
            logger(clientAddr, serverAddr, cmd, MSG_SAVED, "Message Saved");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessage(Socket server, String cmd) throws IOException {
        DataInputStream dis = new DataInputStream(server.getInputStream());
        DataOutputStream dos = new DataOutputStream(server.getOutputStream());
        StringBuilder message = new StringBuilder();
        try{
            toAddr = dis.readUTF();
            System.out.printf("Address: %s%n", toAddr);
            if(verifyRecipient(toAddr) && !toAddr.isEmpty()){
                subject = dis.readUTF();
                System.out.printf("Subject: %s%n", subject);
                message.append(dis.readUTF());
                System.out.printf("Message: %s%n", message);
                String fout = ".email";
                if(!subject.isEmpty()){
                    if(!message.toString().isEmpty()){
                        //writeToInbox(message, fout);
                        writeToDestination(message, fout, toAddr, "inbox");
                        //writeToOutbox(message, fout);
                        writeToDestination(message, fout, userName, "outbox");
                        message.setLength(0);
                        dos.writeUTF(String.format("%d Message sent.", MSG_SUCCESS));
                        dos.flush();
                        logger(clientAddr, serverAddr, cmd, MSG_SUCCESS, "Message Received");
                    }else{
                        dos.writeUTF(String.format("%d Message empty.", MSG_FAIL_EMPTY));
                        dos.flush();
                        message.delete(0, message.length());
                        logger(clientAddr, serverAddr, cmd, MSG_FAIL, "Message Failed");
                    }

                }else{
                    dos.writeUTF(String.format("%d Subject line empty.", MSG_FAIL_SUBJ));
                    dos.flush();
                    message.delete(0, message.length());
                    logger(clientAddr, serverAddr, cmd, MSG_FAIL, "Message Failed");
                }
            }else{
                dis.readUTF();
                dis.readUTF();
                dos.writeUTF(String.format("%d Recipient does not exist.", INVALID_RCPT));
                dos.flush();
                logger(clientAddr, serverAddr, cmd, INVALID_RCPT, "Invalid recipient");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logger(String from, String to, String cmd, int code, String desc) throws IOException {
        Files.createDirectories(Paths.get("db/log"));
        String path = String.format("db/log/%s", "log");

        try (FileWriter fWriter = new FileWriter(path, true);
             BufferedWriter bWriter = new BufferedWriter(fWriter))
        {
            bWriter.write(String.format("%s %s %s %s %d %s\n", date.toString(), from, to, cmd, code, desc));
            if(cmd.equals("QUIT")){
                bWriter.write("**************************************************END OF SESSION**************************************************\n");
            }

            bWriter.flush();
            bWriter.close();
        } catch (FileAlreadyExistsException e) {
            System.out.printf("File '%s' already exists.%n", path);
            System.exit(0);
        } catch (IOException ex) {
            System.out.printf("Error writing file '%s'%n", path);
            System.exit(0);
        }
    }

    private boolean validatePassword(String pass){
        String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%&*])(?=\\S+$).{8,}$";
        return pass.matches(pattern);
    }

    private boolean validateUser(String user){
        Path path = Paths.get(String.format("db/%s", user));
        return Files.exists(path);
    }

    private void createAccount(DataInputStream in, DataOutputStream out, String cmd){
        try{
            String newUser, newPass, confirmPass;
            out.writeUTF(String.format("%d server ready.", AUTH_RESPONSE));
            out.flush();
            String passNumAppend;
            logger(clientAddr,
                    serverAddr, cmd, AUTH_RESPONSE, "Request for new User");
            newUser = in.readUTF();
            newPass = in.readUTF();
            confirmPass = in.readUTF();
            if(!validateUser(newUser)){
                if(validatePassword(newPass) && validatePassword(confirmPass)){
                    if (!newPass.equals(confirmPass)) {
                        out.writeUTF(String.format("%d Passwords do not match.", AUTH_NEW_FAIL));
                        out.flush();
                        logger(clientAddr,
                                serverAddr, cmd, AUTH_NEW_FAIL, "Passwords did not match");
                    } else {
                        out.writeUTF(String.format("%d New User Created Successfully!", AUTH_NEW_SUCCESS));
                        out.flush();
                        passNumAppend = newPass.concat(String.valueOf(1024));
                        String passFile = String.format(".%s", newUser.concat(".user_pass"));
                        byte[] encodedPass = Base64.getEncoder().encode(passNumAppend.getBytes());
                        writePassToFile(encodedPass, newUser, passFile);
                        logger(clientAddr,
                                serverAddr, cmd, AUTH_RESPONSE, "New User Created Successfully");
                    }
                }else{
                    out.writeUTF(String.format("%d Password does not meet requirements.", INVALID_PASS));
                    out.flush();
                    logger(clientAddr,
                            serverAddr, cmd, INVALID_PASS, "Passwords did not meet requirements");
                }

            }else{
                out.writeUTF(String.format("%d User already exists", INVALID_USER));
                out.flush();
                logger(clientAddr,
                        serverAddr, cmd, INVALID_USER, "User already exists.");
            }

        }catch(IOException e){
            e.printStackTrace();
        }

    }

    private boolean verifyRecipient(String rec){
        Path path = Paths.get(String.format("db/%s", rec));
        boolean exists = false;
        if(Files.exists(path) && !rec.isEmpty()){
            exists = true;
        }
        return exists;
    }

    private boolean authenticateAccount(DataInputStream in, DataOutputStream out, String cmd){
        String password;
        try{
            logger(clientAddr,
                    serverAddr, cmd, AUTH_RESPONSE, "Request for login");
            out.writeUTF(String.format("%d Server ready.", AUTH_RESPONSE));
            out.flush();
            userName = in.readUTF();
            password = in.readUTF();
            String passAuth;
            String passFile = String.format(".%s", userName.concat(".user_pass"));
            Path path = Paths.get(String.format("db/%s", userName));
            Path passPath = Paths.get(String.format("db/%s/%s", userName, passFile));

            if (Files.exists(path)) {
                passAuth = password.concat(String.valueOf(1024));
                byte[] encodedPass2 = Base64.getEncoder().encode(String.valueOf(passAuth).getBytes());
                boolean authenticated = readPassFile(passFile, encodedPass2, userName);
                if (authenticated) {
                    logger(clientAddr, serverAddr, cmd, AUTH_SUCCESS, "User authenticated");
                    out.writeUTF(String.format("%d Welcome, %s!", AUTH_SUCCESS, userName));
                    out.flush();
                } else {
                    out.writeUTF(String.format("%d The username/password you have entered is incorrect.", AUTH_DENIED));
                    out.flush();
                    logger(clientAddr, serverAddr, cmd, AUTH_DENIED, "Incorrect password");
                    return false;
                }
            } else {
                out.writeUTF(String.format("%d Username not valid!", AUTH_NO_USER));
                out.flush();
                logger(clientAddr, serverAddr, cmd, AUTH_NO_USER, "Invalid user");
                return false;
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        return true;
    }

    private void getMail(Socket server, String path, String cmd){
        try {
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(server.getOutputStream()));

            File[] files = new File(path).listFiles();
            System.out.println(userName);

            assert files != null;
            System.out.printf("Number of Files to be sent: %d%n", files.length);
            dos.writeInt(files.length);

            int n = 0;
            byte[] buf = new byte[4092];

            for(File file : files){
                String fName = file.getName();
                long fSize = file.length();
                System.out.printf("Sending file: %s of size: %d%n", file.getName(), fSize);
                dos.writeUTF(fName);
                dos.writeLong(fSize);

                FileInputStream fis = new FileInputStream(path + file.getName());
                //read file
                while(fSize > 0 && (n = fis.read(buf, 0, (int)Math.min(buf.length, fSize))) != -1){
                    dos.write(buf,0,n);
                    fSize -= n;
                    dos.flush();
                }
                fis.close();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readDrafts(Socket server, String path, String cmd){
        try{
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(server.getOutputStream()));
            File[] files = new File(path).listFiles();
            int numFiles = files.length;
            System.out.printf("Number of Files in inbox: %d%n", numFiles);
            dos.writeInt(numFiles);
            for(File file : files){
                dos.writeUTF(file.getName());
//                dos.writeUTF(userName);
//                dos.writeUTF(toAddr);
                long fSize = file.length();
                int n;
                byte[] buf = new byte[8192];

                dos.writeLong(fSize);
                FileInputStream fis = new FileInputStream(file);
                //read file
                while(fSize > 0 && (n = fis.read(buf, 0, (int)Math.min(buf.length, fSize))) != -1){
                    dos.write(buf,0,n);
                    fSize -= n;
                    dos.flush();
                }
                fis.close();
            }
            dos.flush();
            logger(clientAddr,
                    serverAddr, cmd, GET_OK, "Request for inbox contents.");

        } catch(FileNotFoundException f){
            f.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readMail(Socket server, String path, String cmd){
        try{
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(server.getOutputStream()));
            File[] files = new File(path).listFiles();
            int numFiles = files.length;
            if (path.contains("inbox")){
                inboxLength = numFiles;
                logger(clientAddr,
                        serverAddr, cmd, GET_OK, "Request for inbox contents.");
            }else{
                outboxLength = numFiles;
                logger(clientAddr,
                        serverAddr, cmd, GET_OK, "Request for outbox contents.");
            }
            System.out.printf("Number of Files: %d%n", numFiles);
            dos.writeInt(numFiles);
            for(File file : files){
                dos.writeUTF(file.getName());
                long fSize = file.length();
                int n;
                byte[] buf = new byte[8192];

                dos.writeLong(fSize);
                FileInputStream fis = new FileInputStream(file);
                //read file
                while(fSize > 0 && (n = fis.read(buf, 0, (int)Math.min(buf.length, fSize))) != -1){
                    dos.write(buf,0,n);
                    fSize -= n;
                    dos.flush();
                }
                fis.close();
            }
            dos.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean checkNewMail(Socket server) throws IOException {
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(server.getOutputStream()));



        return true;
    }



    public void run(){
        while(true) {
            Boolean helloFlag, authFlag;
            authFlag = helloFlag = false;
            String cmd;
            StringBuilder message = new StringBuilder();

            try {
                Socket server = serverSock.accept();

                System.out.printf("Connection to %s established...%n", server.getRemoteSocketAddress());
                System.out.println("Waiting for command...");

                InputStream clientIn = server.getInputStream();
                DataInputStream in = new DataInputStream(clientIn);

                OutputStream clientOut = server.getOutputStream();
                DataOutputStream out = new DataOutputStream(clientOut);

                clientAddr = String.valueOf(server.getRemoteSocketAddress());
                serverAddr = String.valueOf(server.getInetAddress());

                while (!(cmd = in.readUTF()).isEmpty()) {

                    if (cmd.contains("HELO") || cmd.contains("DATA") || cmd.contains("QUIT")
                            || cmd.contains("AUTH") || cmd.contains("CREA") || cmd.contains("GET")
                            || cmd.contains("INBX") || cmd.contains("OTBX") || cmd.contains("SAVE")
                            || cmd.contains("DRFT")){

                        System.out.println("Command received: " + cmd);

                        if (cmd.contains("HELO")) {
                            out.writeUTF(String.format("%d Hello %s please enter a command...", SYSTEM_READY, serverAddr));
                            out.flush();
                            helloFlag = true;
                            logger(clientAddr, serverAddr, cmd, SYSTEM_READY, "Initial handshake");
                            continue;
                        }

                        if (cmd.contains("CREA") && helloFlag) {
                            createAccount(in, out, cmd);
                            continue;
                        }

                        if (cmd.contains("AUTH") && helloFlag) {
                            authFlag = authenticateAccount(in, out, cmd);
                            continue;
                        }

                        if (cmd.equals("GET")) {
                            getMail(server, String.format("db/%s/inbox/", userName), cmd);
                            continue;
                        }
                        if(cmd.equals("INBX")){
                            readMail(server, String.format("db/%s/inbox/", userName), cmd);
                            continue;
                        }

                        if(cmd.equals("OTBX")){
                            readMail(server, String.format("db/%s/outbox/", userName), cmd);
                            continue;
                        }

                        if (cmd.equals("DATA") && authFlag) {
                            receiveMessage(server, cmd);
                            continue;
                        }

                        if(cmd.equals("SAVE")){
                            saveAsDraft(server, cmd);
                        }

                        if(cmd.equals("DRFT")){
                            readDrafts(server, String.format("db/%s/drafts/", userName), cmd);
                        }

                        if (cmd.contains("QUIT")) {
                            out.writeUTF(String.format("%d %s Goodbye!", SYSTEM_READY, cmd));
                            logger(clientAddr, serverAddr, cmd, SYSTEM_READY, "Session terminated");
                            server.close();
                            break;
                        }
                    }else {
                        out.writeUTF(String.format("%d command not recognized!", CMD_SYNTAX_ERR));
                        logger(clientAddr, serverAddr, cmd, CMD_SYNTAX_ERR, "Command not recognized");
                    }
                }
            } catch (SocketTimeoutException s) {
                //System.out.println("No connections found");
            } catch (SocketException x) {
                System.out.println("Socket closed");
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
