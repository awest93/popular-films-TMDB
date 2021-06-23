package com.example.test2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements ViewTreeObserver.OnScrollChangedListener {
    int pageNum;
    ScrollView filmList;
    LinearLayout rowLayout;
    LinearLayout baseLayout;
    int count;
    int max;
    int cane;
    int picHeight,picWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        pageNum=1;
        picHeight=600;
        picWidth=400;
        count=0;
        cane=0;

        Display display = this.getDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        int screenWidth = metrics.widthPixels;

        max=screenWidth/picWidth;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        filmList=findViewById(R.id.scrollView);
        filmList.getViewTreeObserver().addOnScrollChangedListener(this);

        rowLayout = new LinearLayout(getApplicationContext());
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);

        baseLayout = findViewById(R.id.filmSpace);
        baseLayout.addView(rowLayout);

        new filmRequestThread().start();
    }

    public static Drawable LoadImageFromWebOperations(String url) {
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            return Drawable.createFromStream(is, "src name");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openFilmDetails(String filmID) {
        Intent filmDetails = new Intent(this, filmDetails.class);
        filmDetails.putExtra("message", filmID);
        startActivity(filmDetails);
    }

    class filmRequestThread extends Thread {
        filmRequestThread() {

        }

        public void run() {
            cane=1;
            String popularFilmsUrl = "https://api.themoviedb.org/3/movie/popular?api_key=b198ed6c9d8d74f8abc995f6be50dafc&language=ru&page="+pageNum;

            String jsonStr="";
            InputStream in=null;
            try {
                URL url = new URL(popularFilmsUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.connect();
                in = (conn.getInputStream()); //new BufferedInputStream
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonStr=jsonStr.concat(line).concat("\n");
                }

                in.close();
                conn.disconnect();
            }
            catch (Exception  e) {
                e.printStackTrace();
            }

            if (!jsonStr.equals("")) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);
                    JSONArray filmArr = jsonObj.getJSONArray("results");

                    for(int i=0;i<filmArr.length();i++)
                    {
                        LinearLayout.LayoutParams layoutLP = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        layoutLP.gravity = Gravity.CENTER;
                        layoutLP.weight = 1;
                        LinearLayout filmContainer=new LinearLayout(getApplicationContext());
                        filmContainer.setLayoutParams(layoutLP);
                        filmContainer.setOrientation(LinearLayout.VERTICAL);
//                        filmContainer.setBackgroundResource(R.drawable.custom_border);

                        JSONObject film = filmArr.getJSONObject(i);
                        String posterPath=film.getString("poster_path");
                        String filmID=film.getString("id");

                        ImageView poster = new ImageView(getApplicationContext());
                        LinearLayout.LayoutParams posterParam = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                        posterParam.weight = 1;
                        posterParam.gravity = Gravity.CENTER;
                        poster.setLayoutParams(posterParam);
                        Drawable pic;
                        pic = LoadImageFromWebOperations("https://image.tmdb.org/t/p/w500"+posterPath);

                        poster.setImageDrawable(pic);
                        poster.setPadding(5, 5, 5, 5);
                        poster.setMinimumHeight(picHeight);
                        poster.setMinimumWidth(picWidth);
                        poster.setOnClickListener(arg0 -> openFilmDetails(filmID));
                        filmContainer.addView(poster);

                        TextView filmName=new TextView(getApplicationContext());
                        LinearLayout.LayoutParams titleParam = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        titleParam.weight = 2;
                        titleParam.gravity = Gravity.CENTER;
                        filmName.setLayoutParams(titleParam);
                        filmName.setLines(4);
                        filmName.setText(film.getString("title"));
                        filmName.setPadding(5, 5, 5, 5);
                        filmName.setOnClickListener(arg0 -> openFilmDetails(filmID));
                        filmContainer.addView(filmName);
                        if(count<max)
                        {
                            Message message = new Message();
                            message.obj = filmContainer;
                            message.what = 1;
                            myHandler.sendMessage(message);
                            count++;
                        }
                        if(count>=max)
                        {
                            myHandler.sendEmptyMessage(0);
                            count=0;
                        }
                    }
                    pageNum++;
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.print("Couldn't get json from server.");
            }
            cane=0;
        }

        Handler myHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == 0)
                {
                    rowLayout=new LinearLayout(getApplicationContext());
                    rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                    baseLayout.addView(rowLayout);
                }
                if(msg.what == 1)
                {
                    rowLayout.addView((View) msg.obj);
                }
            }
        };
    }

    public void onScrollChanged(){
        View view = filmList.getChildAt(filmList.getChildCount() - 1);
        int topDetector = filmList.getScrollY();
        int bottomDetector = view.getBottom() - (filmList.getHeight() + filmList.getScrollY());
//        System.out.print("topDetector-"+topDetector+"\nbottomDetector"+bottomDetector);
        if(bottomDetector <= 200 ){
            if(cane==0)
                new filmRequestThread().start();
//            System.out.print("BOTTOM");
        }
        if(topDetector <= 0){
//            System.out.print("TOP");
        }
    }
}