package com.example.jsdemo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "========";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = (WebView) findViewById(R.id.webView);
        webView.addJavascriptInterface(new JavaScriptObj(), "android");
        initSetting();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.toString());
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {

                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.e(TAG, "onPageFinished: ------------------------------------------ " + url);
                if (isAutoInput) {

                    //只有等待webview加载完自己写入的js方法后才能调用
                    Message message = mHandler.obtainMessage();
                    message.what = 2;
                    mHandler.sendMessage(message);
                    isAutoInput = false;
                }
            }
        });
        webView.loadUrl("https://www.baidu.com");
    }

    private void initSetting() {

        WebSettings setting = webView.getSettings();
        setting.setJavaScriptEnabled(true);
        setting.setAllowFileAccess(true);
        setting.setSupportZoom(true);
        setting.setBuiltInZoomControls(false);
        setting.setAppCacheEnabled(false);
        setting.setDefaultTextEncodingName("UTF-8");
        setting.setLoadsImagesAutomatically(true);
        setting.setNeedInitialFocus(true);
    }

    private boolean isAutoInput = false;

    public void testPay(View view) {
        isAutoInput = true;
        //获取网页的内容
        webView.loadUrl("javascript:window.android.getContent('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
    }

    class JavaScriptObj {

        @JavascriptInterface
        public void getContent(String content) {

            Log.e(TAG, "getContent: " + content);
            Message msg = new Message();
            msg.obj = content;
            msg.what = 1;
            mHandler.sendMessage(msg);
        }
    }


    StaticHandler mHandler = new StaticHandler(this);

    private static class StaticHandler extends Handler {
        WeakReference<MainActivity> activityReference;

        StaticHandler(MainActivity activity) {
            activityReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = activityReference.get();
            if (activity != null) {
                if (msg.what == 1) {
                    //将jSFunction字符串写入获取到的网页内容中
                    StringBuilder stringBuidler = new StringBuilder();
                    String content = (String) msg.obj;
                    stringBuidler.append("\n<html>\n");

                    stringBuidler.append(activity.jSFunction("index-kw", "android自动填充的数据"));

                    stringBuidler.append(content.split("<html>")[1] + "\n");
                    activity.webView.loadDataWithBaseURL("https://www.baidu.com", stringBuidler.toString(), "text/html", "utf-8", null);
                } else if (msg.what == 2) {
                    activity.webView.loadUrl("javascript:setInput()");

                }
            }
        }
    }


    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            mHandler.removeCallbacksAndMessages(null);
            super.onBackPressed();
        }
    }

    /**
     *
     * @param id 网页中输入框的id
     * @param value 要填充的内容
     * @return 返回一个jSFunction的字符串
     */
    private String jSFunction(String id, String value) {

        StringBuilder s = new StringBuilder();
        s.append("<script type=\"text/javascript\">\n");

        s.append("function setInput(){\n");
        s.append("document.getElementById('" + id + "').value='" + value + "';");
        s.append("\n}\n");

        s.append("</script>");

        return s.toString();
    }


    public byte[] readStream(InputStream inputStream) throws Exception {
        byte[] buffer = new byte[1024];
        int len = -1;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        while ((len = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, len);
        }

        inputStream.close();
        byteArrayOutputStream.close();
        return byteArrayOutputStream.toByteArray();
    }

    public String getHtml(final String urlpath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlpath);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(6 * 1000);
                    conn.setRequestMethod("GET");

                    if (conn.getResponseCode() == 200) {
                        InputStream inputStream = conn.getInputStream();
                        byte[] data = readStream(inputStream);
                        Message msg = new Message();
                        msg.obj = new String(data);
                        msg.what = 1;
                        mHandler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return "";
    }
}
