package com.example.test2;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class filmDetails extends AppCompatActivity {
    String filmID;
    ImageView poster;
    TextView filmName;
    TextView filmRating;
    TextView minorDetails;
    TextView filmTagLine;
    TextView filmOverview;
    int picHeight,picWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_film_details2);
        poster=findViewById(R.id.detailsPoster);
        filmName=findViewById(R.id.detailsTitle);
        filmRating=findViewById(R.id.rating);
        minorDetails=findViewById(R.id.details);
        filmTagLine=findViewById(R.id.tagLine);
        filmOverview=findViewById(R.id.overview);

        Display display = this.getDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        picHeight=screenHeight/2;
        picWidth=screenWidth/2;

        poster.setMinimumHeight(picHeight);
        poster.setMinimumWidth(picWidth);
        filmID=getIntent().getStringExtra("message");

        new filmDetailsRequestThread().start();
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

    class filmDetailsRequestThread extends Thread {
        filmDetailsRequestThread() {

        }

        public void run() {
            String popularFilmsUrl = "https://api.themoviedb.org/3/movie/"+filmID+"?api_key=b198ed6c9d8d74f8abc995f6be50dafc&language=ru";

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
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            catch (ProtocolException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (Exception  e) {
                e.printStackTrace();
            }

            if (!jsonStr.equals("")) {
                try {
                    JSONObject filmDetails = new JSONObject(jsonStr);
                    Message message = new Message();
                    message.obj = filmDetails;
                    message.what = 0;
                    myHandler.sendMessage(message);

                    String posterPath=filmDetails.getString("poster_path");

                    Drawable pic;
                    pic = LoadImageFromWebOperations("https://image.tmdb.org/t/p/w500"+posterPath);
                    message = new Message();
                    message.obj = pic;
                    message.what = 1;
                    myHandler.sendMessage(message);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.print("Couldn't get json from server.");
            }
        }

        Handler myHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case(0):
                        JSONObject filmDetails = (JSONObject) msg.obj;
                        try {
                            String temp;
                            filmName.setText(filmDetails.getString("title"));
                            temp=Double.parseDouble(filmDetails.getString("vote_average"))*10+"%";
                            filmRating.setText(temp);
                            temp=filmDetails.getString("title")+" ("+filmDetails.getString("release_date").substring(0,4)+") \n"+
                                    filmDetails.getString("release_date")+" • ";
                            JSONArray genres=filmDetails.getJSONArray("genres");
                            for(int i=0;i<genres.length();i++)
                            {
                                temp=temp.concat(genres.getJSONObject(i).getString("name"));
                                if(i!=genres.length()-1)
                                    temp=temp.concat(", ");
                            }
                            int duration=Integer.parseInt(filmDetails.getString("runtime"));
                            temp+=" • "+duration/60+"h "+duration%60+"m";
                            minorDetails.setText(temp);
                            filmTagLine.setText(filmDetails.getString("tagline"));
                            filmOverview.setText(filmDetails.getString("overview"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case(1):
                        Drawable pic=(Drawable) msg.obj;
                        poster.setImageDrawable(pic);
                        break;
                }
            }
        };
    }
}