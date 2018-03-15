package Client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Sender {

    private static Socket client;
    private static String serverName;
    private static int portNum;
    private static DataOutputStream out;
    private static DataInputStream in;

    public static void main(String[] args) {
        serverName = args[0];
        portNum = Integer.parseInt(args[1]);


        connect();
    }
    public static void connect(){
        String command, reply, line, userName;
        int pass;
        StringBuilder message = new StringBuilder();

        try{

            System.out.println("Trying to connect to " + serverName);
            client = new Socket(serverName, portNum);

            if(client.isConnected()){
                System.out.println("Connection successful!");
            }

            OutputStream serverOut = client.getOutputStream();
            out = new DataOutputStream(serverOut);

            InputStream serverIn = client.getInputStream();
            in = new DataInputStream(serverIn);

            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Enter a command: ");
            command = userInput.readLine();
            out.writeUTF(command);


            while((reply = in.readUTF()) != null){
                System.out.println("Server: " + reply);

                if(command.equals("DATA")) {
                    if(!reply.contains("503") && !reply.contains("337")) {
                        String subject = userInput.readLine();
                        out.writeUTF(subject);
                        System.out.println("Server: " + in.readUTF());
                        while (!(line = userInput.readLine()).equals(".")) {
                            message.append(line);
                            message.append("\n");
                        }
                        System.out.println(message.toString());
                        out.writeUTF(message.toString());
                    }else{
                        System.out.println("Enter another command: ");
                        command = userInput.readLine();
                        out.writeUTF(command);
                        continue;
                    }
                }
                if(command.equals("GET")) {
                    String filePath = userInput.readLine();
                    out.writeUTF(filePath);
                    System.out.println("Server: " + in.readUTF());
                }
                if(command.equals("AUTH")){
                    System.out.print(in.readUTF());
                    userName = userInput.readLine();
                    out.writeUTF(userName);
                    String auth = in.readUTF();
                    if(auth.contains("330")){
                        System.out.println(auth);
                        clientReconnect();
                        continue;
                    }else {
                        System.out.print(auth);
                        pass = Integer.parseInt(userInput.readLine());
                        out.writeInt(pass);
                        auth = in.readUTF();
                        while (auth.contains("337")) {
                            System.out.println(auth);
                            pass = Integer.parseInt(userInput.readLine());
                            out.writeInt(pass);
                            auth = in.readUTF();
                        }
                        System.out.println(auth);
                    }
                }
                if(command.contains("QUIT")){
                    break;
                }
                System.out.println("Enter another command: ");
                command = userInput.readLine();
                out.writeUTF(command);

            }
            client.close();
            System.exit(0);
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

    public static void clientReconnect(){
        try {
            System.out.println("Disconnecting...");
            client.close();
            TimeUnit.SECONDS.sleep(5);
            System.out.println("Reconnecting to " + serverName + "...");
            connect();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
