package com.example.gavin.myapplication;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.gavin.myapplication.SocketActicity;

import org.java_websocket.server.DefaultSSLWebSocketServerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;


public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    public SocketActicity server;
    public SocketActicity sslServer;


/*
    private Socket mSocket;

    {
        System.out.println("i am in 1");
        IO.Options opts = new IO.Options();
        opts.port = 50005;

        try {
            mSocket = IO.socket("http://127.0.0.1", opts);
        } catch (URISyntaxException e) {
            System.out.println("i am in 2");
            throw new RuntimeException(e);

        }
    }
*/
/*
    public void connectSocketIO(){
        try {
            System.out.println("i am in");
            mSocket.on("connection", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    System.out.println("tyDebug:"+args.toString());
                }
            }).on("api", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                }
            }).on("pair", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    mSocket.emit("paired",true);
                    System.out.println("ty:"+args.toString());
                }
            });

        } catch(Exception e){

        }
    }


*/

    private SSLContext getSSLConextFromAndroidKeystore() {
        // load up the key store

        String storePassword = "password";
        String keyPassword = "password";

        KeyStore ks;
        SSLContext sslContext;
        try {

            KeyStore keystore = KeyStore.getInstance("BKS");
            InputStream in = getResources().openRawResource(R.raw.keystore);

            try {
                keystore.load(in, storePassword.toCharArray());
            } finally {
                in.close();
            }

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
            keyManagerFactory.init(keystore, keyPassword .toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(keystore);

            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), tmf.getTrustManagers(), null);
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | KeyManagementException | UnrecoverableKeyException e) {
            throw new IllegalArgumentException();
        }
        return sslContext;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //当要打开网页时，先打开本地websocket
        //可以选择每次都重新监听50005端口，也可以一直监听
        //需要你们做一下测试，看一下哪种稳定
        //int port = 50005; // 843 flash policy port
        try {

            //设置websocket监听

            //server = new SocketActicity(50005);

            //设置地址重用 这个很关键/

            //server.setReuseAddr(true);

            //开启监听

            //server.start();

            sslServer = new SocketActicity( 50006 ); // Firefox does allow multible ssl connection only via port 443 //tested on FF16

            sslServer.setReuseAddr(true);

            // load up the key store
            /*
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
            */

            //SSLContext sslContext = null;
            //sslContext = SSLContext.getInstance( "TLS" );
            //sslContext.init( kmf.getKeyManagers(), tmf.getTrustManagers(), null );
            SSLContext sslContext = getSSLConextFromAndroidKeystore();
            sslServer.setWebSocketFactory( new DefaultSSLWebSocketServerFactory( sslContext ) );

            sslServer.start();

            //可有可无的进度条
            progressBar= (ProgressBar)findViewById(R.id.progressbar);//进度条

            //开启webview
            webView = (WebView) findViewById(R.id.webview);
            //"https://www.baidu.com/"
            //"https://betdice.one"
            //"https://developer.mathwallet.org/sample01/"
            webView.loadUrl("https://betdice.one/");//加载url

            //使用webview显示html代码
            //webView.loadDataWithBaseURL(null,"<html><head><title> 欢迎您 </title></head>" +
            //"<body><h2>使用webview显示 html代码</h2></body></html>", "text/html" , "utf-8", null);

            //webView.addJavascriptInterface(this,"android");//添加js监听 这样html就能调用客户端

            //设置两个Client，必须设置，本次其实没有用到两个Client的太多功能
            //可以看一下两个Client与webview之间的事件循环周期，增加断点调试
            webView.setWebChromeClient(webChromeClient);
            webView.setWebViewClient(webViewClient);

            //无关紧要的一些设置
            WebSettings webSettings=webView.getSettings();
            webSettings.setJavaScriptEnabled(true);//允许使用js
            webSettings.setJavaScriptCanOpenWindowsAutomatically(true);//允许使用弹窗

            //设置缓存，不然会有奇怪bug
            webSettings.setDomStorageEnabled(true);
            webSettings.setAppCacheMaxSize(1024*1024*64);
        } catch (Exception e) {
            e.printStackTrace();
        }


        //无关紧要的一些尝试，后续eth游戏接入会用到，你们直接删掉就好
        //String appCachePath = getApplicationContext().getCacheDir().getAbsolutePath();
        //webSettings.setAppCachePath(appCachePath);
        //webSettings.setAllowFileAccess(true);
        //webSettings.setAppCacheEnabled(true);
        /**
         * LOAD_CACHE_ONLY: 不使用网络，只读取本地缓存数据
         * LOAD_DEFAULT: （默认）根据cache-control决定是否从网络上取数据。
         * LOAD_NO_CACHE: 不使用缓存，只从网络获取数据.
         * LOAD_CACHE_ELSE_NETWORK，只要本地有，无论是否过期，或者no-cache，都使用缓存中的数据。
         */
        //webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);//不使用缓存，只从网络获取数据.

        //支持屏幕缩放
        //webSettings.setSupportZoom(true);
        //webSettings.setBuiltInZoomControls(true);

        //不显示webview缩放按钮
//        webSettings.setDisplayZoomControls(false);

    }

    private WebViewClient webViewClient=new WebViewClient(){
        @Override
        public void onPageFinished(WebView view, String url) {//页面加载完成
            //通过onPageFinished的事件，实现滚动条的移动，你们根据前端需求自行添加人性化交互
            progressBar.setVisibility(View.GONE);

            //view.loadUrl("javascript:" + "window.scatter = {iden : 666};" );

            //view.loadUrl("javascript:" + "console.log(\"gtygty \" + window.scatter)");
            super.onPageFinished(view, url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {//页面开始加载
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error){
            handler.proceed();
        }
    };

    //WebChromeClient主要辅助WebView处理Javascript的对话框、网站图标、网站title、加载进度等
    private WebChromeClient webChromeClient=new WebChromeClient(){
        //不支持js的alert弹窗，需要自己监听然后通过dialog弹窗
        @Override
        public boolean onJsAlert(WebView webView, String url, String message, JsResult result) {
            //可做可不做的一些拦截，具体可以参考麦子钱包的界面
            //网页的一些原生alert弹出就是通过此方法拦截的
            AlertDialog.Builder localBuilder = new AlertDialog.Builder(webView.getContext());
            localBuilder.setMessage(message).setPositiveButton("确定",null);
            localBuilder.setCancelable(false);
            localBuilder.create().show();

            //注意:
            //必须要这一句代码:result.confirm()表示:
            //处理结果为确定状态同时唤醒WebCore线程
            //否则不能继续点击按钮
            result.confirm();
            return true;
        }

        //获取网页标题
        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            Log.i("ty","网页标题:"+title);
        }

        //加载进度回调
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
        }
    };
    /*
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i("ansen","是否有上一个页面:"+webView.canGoBack());
        if (webView.canGoBack() && keyCode == KeyEvent.KEYCODE_BACK){//点击返回按钮的时候判断有没有上一页
            webView.goBack(); // goBack()表示返回webView的上一页面
            return true;
        }
        return super.onKeyDown(keyCode,event);
    }
    */

    /**
     * JS调用android的方法
     * @param str
     * @return
     */
    //和你们没关系
    @JavascriptInterface //仍然必不可少
    public void  getClient(String str){
        Log.i("ty","html调用客户端:"+str);
    }

    //资源释放，由于这里socket.setReuseAddr(true); 所以这里socket是否释放也无关紧要。
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //释放资源
        webView.destroy();
        webView=null;

    }
}

