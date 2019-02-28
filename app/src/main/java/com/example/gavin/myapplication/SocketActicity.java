package com.example.gavin.myapplication;

/*
 * Copyright (c) 2010-2018 Nathan Rajlich
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;


/**
 * A simple WebSocketServer implementation. Keeps track of a "chatroom".
 */
public class SocketActicity extends WebSocketServer {

    private int cport;

    public SocketActicity( int port ) throws UnknownHostException {
        super( new InetSocketAddress( port ) );
        System.out.println("constructor:" + port);
        cport = port;
    }

    public SocketActicity( InetSocketAddress address ) {
        super( address );
    }

    @Override
    public void onOpen( WebSocket conn, ClientHandshake handshake ) {
        conn.send("Welcome to the server!"); //This method sends a message to the new client
        //broadcast( "new connection: " + handshake.getResourceDescriptor() ); //This method sends a message to all clients connected
        System.out.println( conn.getRemoteSocketAddress().getAddress().getHostAddress()+":"+ cport + " entered the room!" );
    }

    @Override
    public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
        //broadcast( conn + " has left the room!" );
        System.out.println( conn + " has left the room!" );
    }



    @Override
    public void onMessage( WebSocket conn, String message ) {
        //broadcast( message );
        System.out.println( "ty1:"+conn + ": " + message );
        /*scatter协议规则就是：
            网页给我们发过来的消息格式为："42/scatter,[json数据]" 的字符串
            我们第一步解析42/scatter是否正确
            接着解析后面的json数据是想要登陆还是交易
        */
        String a = "42/scatter";
        System.out.println(message.substring(0,10).equals(a));
        //暂时没把URL插进去，后面正式部署时，改了需要手动调一下
        String URL = "3.17.163.147:3000";

        //broadcast("[\"paired\",true]");
        if (message.substring(0,10).equals(a)){
            //System.out.println(message.substring(11));
            String JsonString = message.substring(11);
            JSONArray jsonArray = JSON.parseArray(JsonString);
            String type = jsonArray.getString(0);
            //System.out.println(type);
            if (type.equals("pair")){
                System.out.println("(pair)***");
                conn.send("42/scatter,[\"paired\",true]");
            }
            else if(type.equals("api")){
                JSONObject request = jsonArray.getJSONObject(1);
                //System.out.println(request.getJSONObject("data").getString("id"));
                //request.getJSONObject("data").getJSONObject("id");
                String id = request.getJSONObject("data").getString("id");
                String api_type = request.getJSONObject("data").getString("type");


                if (api_type.equals("requestAddNetwork")){
                    List<Object> list = new ArrayList<>();
                    list.add("api");
                    Map<String, Object> map = new HashMap<>();
                    map.put("id",id);
                    map.put("result",true);
                    JSONObject jsonObj = new JSONObject(map);
                    list.add(jsonObj);
                    JSONArray jsonArr = JSONArray.parseArray(JSON.toJSONString(list));
                    System.out.println("(api)***requestAddNetwork:" + jsonArr);
                    conn.send("42/scatter,"+jsonArr);
                }
                else if(api_type.equals("forgetIdentity")){
                    //初始化放入"id"
                    List<Object> list = new ArrayList<>();
                    list.add("api");
                    Map<String, Object> map = new HashMap<>();
                    map.put("id",id);
                    map.put("result",true);
                    JSONObject jsonObj = new JSONObject(map);
                    list.add(jsonObj);
                    JSONArray jsonArr = JSONArray.parseArray(JSON.toJSONString(list));
                    System.out.println("(api)***forgetIdentity:" + "42/scatter,"+jsonArr);
                    conn.send("42/scatter,"+jsonArr);
                }
                else if(api_type.equals("getOrRequestIdentity")) {
                    //初始化放入"id"
                    List<Object> list = new ArrayList<>();
                    list.add("api");
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", id);

                    /*
                      result 值为http传回的值
                     */
                    //发送http请求
                    String path = "http://3.17.163.147:3000/eos/scatter/getOrRequestIdentity";
                    //参数为UID，后续前端不需要输入参数，只需要传cookie即可
                    String sr = HttpRequest.sendPost(path, "UID=SuperUID");
                    System.out.println("POST response:" + sr);

                    //解析回包
                    JSONObject jsonResponse = JSON.parseObject(sr);
                    System.out.println(jsonResponse.getString("code"));
                    System.out.println(jsonResponse.getJSONObject("data").getString("result"));
                    // 处理出result包
                    JSONObject resultData = jsonResponse.getJSONObject("data").getJSONObject("result");

                    //生成socket回包
                    map.put("result", resultData);
                    JSONObject jsonObj = new JSONObject(map);
                    list.add(jsonObj);
                    JSONArray jsonArr = JSONArray.parseArray(JSON.toJSONString(list));
                    System.out.println("(api)***getOrRequestIdentity:" + "42/scatter," + jsonArr);
                    conn.send("42/scatter," + jsonArr);
                }
                else if(api_type.equals("identityFromPermissions")){
                    //初始化放入"id"
                    List<Object> list = new ArrayList<>();
                    list.add("api");
                    Map<String, Object> map = new HashMap<>();
                    map.put("id",id);

                    /*
                      result 值为http传回的值
                     */

                    //发送http请求
                    String path="http://3.17.163.147:3000/eos/scatter/identityFromPermissions";
                    //参数为UID，后续前端不需要输入参数，只需要传cookie即可
                    String sr=HttpRequest.sendPost(path, "UID=SuperUID");
                    System.out.println("POST response:"+sr);

                    //解析回包
                    JSONObject jsonResponse = JSON.parseObject(sr);
                    System.out.println(jsonResponse.getString("code"));
                    System.out.println(jsonResponse.getJSONObject("data").getString("result"));
                    // 处理出result包
                    JSONObject resultData = jsonResponse.getJSONObject("data").getJSONObject("result");

                    //生成socket回包
                    map.put("result",resultData);
                    JSONObject jsonObj = new JSONObject(map);
                    list.add(jsonObj);
                    JSONArray jsonArr = JSONArray.parseArray(JSON.toJSONString(list));
                    System.out.println("(api)***identityFromPermissions:" + "42/scatter,"+jsonArr);
                    conn.send("42/scatter,"+jsonArr);
                }
                else if(api_type.equals("requestSignature")){
                    //初始化放入"id"
                    List<Object> list = new ArrayList<>();
                    list.add("api");
                    Map<String, Object> map = new HashMap<>();
                    map.put("id",id);


                    //获取交易信息
                    JSONObject txData = request.getJSONObject("data").getJSONObject("payload").getJSONObject("transaction");

                    //请求原始交易信息
                    String path_originData="http://3.17.163.147:3000/eos/scatter/getOriginData";
                    //参数为UID，后续前端不需要输入参数，只需要传cookie即可
                    String txDataStr=HttpRequest.sendPost(path_originData, "UID=SuperUID&data="+txData);
                    System.out.println(txDataStr);

                    //解析原始交易信息回包
                    JSONObject originDataResponse = JSON.parseObject(txDataStr);
                    System.out.println(originDataResponse.getString("code"));

                    //**********************************************
                    //四大交易信息 需要做一个弹窗给用户看
                    //这里是唯一一处我没有写完整的代码
                    //需要你自己填写一下，做一个用户授权的界面
                    //**********************************************
                    //交易发起方
                    String from = originDataResponse.getJSONObject("data").getString("from");
                    //交易接收方
                    String to = originDataResponse.getJSONObject("data").getString("to");
                    //数量
                    String quantity = originDataResponse.getJSONObject("data").getString("quantity");
                    //交易信息
                    String memo = originDataResponse.getJSONObject("data").getString("memo");

                    System.out.println("您确定交易信息为："+"\nfrom:"+from+"\nto:"+to+"\nquantity:"+quantity+"\nmemo"+memo);



                    /*
                      上述用户信息确定后，下文生成websocket回包
                     */

                    //读出data.payload.buf
                    System.out.println(request.getJSONObject("data").getJSONObject("payload").getJSONObject("buf"));
                    JSONObject buf = request.getJSONObject("data").getJSONObject("payload").getJSONObject("buf");





                    //发送http请求
                    String path_sign="http://3.17.163.147:3000/eos/scatter/requestSignature";
                    //参数为UID，后续前端不需要输入参数，只需要传cookie即可
                    String sr=HttpRequest.sendPost(path_sign, "UID=SuperUID&buf="+buf);
                    System.out.println("POST response:"+sr);

                    //解析回包
                    JSONObject jsonResponse = JSON.parseObject(sr);
                    System.out.println(jsonResponse.getString("code"));
                    System.out.println(jsonResponse.getJSONObject("data").getString("result"));
                    // 处理出result包
                    JSONObject resultData = jsonResponse.getJSONObject("data").getJSONObject("result");

                    //生成socket回包
                    map.put("result",resultData);
                    JSONObject jsonObj = new JSONObject(map);
                    list.add(jsonObj);
                    JSONArray jsonArr = JSONArray.parseArray(JSON.toJSONString(list));
                    System.out.println("(api)***identityFromPermissions:" + "42/scatter,"+jsonArr);
                    conn.send("42/scatter,"+jsonArr);


                }
            }


        }

        //System.out.println(message.substring(11));
        //conn.send("42/scatter,[\"paired\",true]");
    }



    @Override
    public void onMessage( WebSocket conn, ByteBuffer message ) {
        //broadcast( message.array() );
        System.out.println( "ty2:"+conn + ": " + message );
        conn.send("true");

    }


    public static void main( String[] args ) throws InterruptedException , IOException, Exception {
        //
        /*
        int port = 50005; // 843 flash policy port
        int portSSL = 50006; // 843 flash policy port
        try {
            port = Integer.parseInt( args[ 0 ] );
            portSSL = Integer.parseInt( args[ 0 ] );
        } catch ( Exception ex ) {

        }
        SocketActicity server = new SocketActicity( port );
        server.setReuseAddr(true);
        server.start();
        //System.out.println( "Let's rock in : " + s.getPort() );

        SocketActicity sslServer = new SocketActicity( portSSL); // Firefox does allow multible ssl connection only via port 443 //tested on FF16
        sslServer.setReuseAddr(true);
        // load up the key store
        String STORETYPE = "JKS";
        String KEYSTORE = "/Users/Gavin/Documents/android-project/myapplication/app/src/main/java/com/example/gavin/myapplication/scatter.jks";
        String STOREPASSWORD = "password";
        String KEYPASSWORD = "password";

        KeyStore ks = KeyStore.getInstance( STORETYPE );
        File kf = new File( KEYSTORE );
        ks.load( new FileInputStream( kf ), STOREPASSWORD.toCharArray() );

        KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
        kmf.init( ks, KEYPASSWORD.toCharArray() );
        TrustManagerFactory tmf = TrustManagerFactory.getInstance( "SunX509" );
        tmf.init( ks );

        SSLContext sslContext = null;
        sslContext = SSLContext.getInstance( "TLS" );
        sslContext.init( kmf.getKeyManagers(), tmf.getTrustManagers(), null );

        sslServer.setWebSocketFactory( new DefaultSSLWebSocketServerFactory( sslContext ) );

        sslServer.start();
        */
        String path = "http://127.0.0.1:4000/eth/web3/signPersonalMessage";
        //参数为UID，后续前端不需要输入参数，只需要传cookie即可
        String sr = HttpRequest.sendPost(path,"Message="+"{\"from\":\"0x47B9Be7A0FC74Be3fccdECfC6d41d21D24D4a672\",\"data\":\"0xe6aca2e8bf8ee69da5e588b0e4ba91e69697e9be99efbc81\"}");
        System.out.println("POST response:" + sr);


    }

    @Override
    public void onError( WebSocket conn, Exception ex ) {
        ex.printStackTrace();
        if( conn != null ) {
            // some errors like port binding failed may not be assignable to a specific websocket
        }
    }

    @Override
    public void onStart() {
        System.out.println("Server started!");
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }

}