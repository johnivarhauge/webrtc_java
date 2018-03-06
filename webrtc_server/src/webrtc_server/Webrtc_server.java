/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc_server;

import java.io.IOException;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author Student
 */


public class Webrtc_server {
    
    static void handshake(InputStream in, OutputStream out) throws NoSuchAlgorithmException, UnsupportedEncodingException, IOException{
        String data = new Scanner(in,"UTF-8").useDelimiter("\\r\\n\\r\\n").next();
        Matcher get = Pattern.compile("^GET").matcher(data);

        if (get.find()) {
            Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
            match.find();
            byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Sec-WebSocket-Accept: "
                    + DatatypeConverter
                    .printBase64Binary(
                            MessageDigest
                            .getInstance("SHA-1")
                            .digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                    .getBytes("UTF-8")))
                    + "\r\n\r\n")
                    .getBytes("UTF-8");

            out.write(response);    
        }
    }
    
        static void sendMessage(String text, OutputStream out) throws IOException{
        String reply = text;
        byte[] rawData = reply.getBytes();

        int frameCount  = 0;
        byte[] frame = new byte[10];

        frame[0] = (byte) 129;

        if(rawData.length <= 125){
            frame[1] = (byte) rawData.length;
            frameCount = 2;
        }else if(rawData.length >= 126 && rawData.length <= 65535){
            frame[1] = (byte) 126;
            int length = rawData.length;
            frame[2] = (byte)((length >> 8 ) & (byte)255);
            frame[3] = (byte)(length & (byte)255); 
            frameCount = 4;
        }else{
            frame[1] = (byte) 127;
            int length = rawData.length;
            frame[2] = (byte)((length >> 56 ) & (byte)255);
            frame[3] = (byte)((length >> 48 ) & (byte)255);
            frame[4] = (byte)((length >> 40 ) & (byte)255);
            frame[5] = (byte)((length >> 32 ) & (byte)255);
            frame[6] = (byte)((length >> 24 ) & (byte)255);
            frame[7] = (byte)((length >> 16 ) & (byte)255);
            frame[8] = (byte)((length >> 8 ) & (byte)255);
            frame[9] = (byte)(length & (byte)255);
            frameCount = 10;
        }

        int bLength = frameCount + rawData.length;

        byte[] message = new byte[bLength];

        int bLim = 0;
        for(int i=0; i<frameCount;i++){
            message[bLim] = frame[i];
            bLim++;
        }
        for(int i=0; i<rawData.length;i++){
            message[bLim] = rawData[i];
            bLim++;
        }

        out.write(message, 0, message.length);
        out.flush();

    }
    
    static void recieveMessage(InputStream in) throws IOException{
        int len = 0;            
        byte[] b = new byte[256];
        //rawIn is a Socket.getInputStream();
        while(true){
            len = in.read(b);
            if(len!=-1){

                byte rLength = 0;
                int rMaskIndex = 2;
                int rDataStart = 0;
                //b[0] is always text in my case so no need to check;
                byte databytes = b[1];
                byte op = (byte) 127;
                rLength = (byte) (databytes & op);

                if(rLength==(byte)126) rMaskIndex=4;
                if(rLength==(byte)127) rMaskIndex=10;

                byte[] masks = new byte[4];

                int j=0;
                int i=0;
                for(i=rMaskIndex;i<(rMaskIndex+4);i++){
                    masks[j] = b[i];
                    j++;
                }

                rDataStart = rMaskIndex + 4;

                int messLen = len - rDataStart;

                byte[] message = new byte[messLen];

                for(i=rDataStart, j=0; i<len; i++, j++){
                    message[j] = (byte) (b[i] ^ masks[j % 4]);
                }
                
                System.out.println(new String(message, "UTF-8"));
            }
            break;
        }
    }
    
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException{
        
        try (ServerSocket server = new ServerSocket(9876)) {
            System.out.println("Server has started on 127.0.0.1:9876.\r\nWaiting for a connection...");
            
            while(true) {
                Socket client = server.accept();
                System.out.println("A client connected.");
                InputStream in = client.getInputStream();
                OutputStream out = client.getOutputStream();
                handshake(in, out);         
                recieveMessage(in);
                sendMessage("masse deilig text", out);
                sendMessage("andre melding", out);
                Thread.sleep(100);
            }
        }
    }
    
}

