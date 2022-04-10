package vip.mannheim.radio;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import vip.mannheim.radio.views.CoverView;

public class MainActivity extends AppCompatActivity implements StationAdapter.OnStationListener{

    ConstraintLayout constraintLayoutVolume;

    String SERVER_IP = "192.168.1.113";
    int SERVER_PORT = 5555;

    HashMap<String, HashMap<Integer, String> > cityMap = new HashMap();

    String pathToImagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getParent() + "/Pictures/";
    File myDirectory = new File(pathToImagesDir);
    File[] directories = myDirectory.listFiles(new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    });

    String locationCity = directories[0].getName();

    private final static int MAX_FRQ = 10800; // 10800 == 108.00
    private final static int MIN_FRQ = 6800; // 6800 == 108.00
    int lastFREQ = 6800; // Последняя станция

    int currentVolume = 10;
    private final static int MAX_VOL = 15;
    private final static int MIN_VOL = 0;
    boolean muteVolume = true;

    private Timer volumeTimer;
    TextView watch;
    ImageView imageViewMute;
    SeekBar seekBar;

    TextView stationName;
    TextView stationRDSText;
    TextView stationFREQ;

    RecyclerView StationRecyclerview;
    StationAdapter StationAdapter;
    List<StationItem> mData;
    EditText searchInput;
    CharSequence search = "";

    RelativeLayout relativeLayout;
    View dialogView;
    ConstraintLayout dialogContainer;
    TextView message_safe_dialog;
    Button button_yes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);


        for(int i = 0; i < directories.length; i++) {
            // Загружаем иконки станция
            HashMap<Integer, String> stations = new HashMap();

            File[] files = new File(pathToImagesDir + directories[i].getName()).listFiles();

            for (File file : files) {
                if (file.isFile()) {

                    String[] fullFileName;
                    String[] splitName;

                    fullFileName = file.getName().split("_");
                    Log.v("Инфо загрузки файла: ", "Локация (" + fullFileName[0] + "), Файл (" + fullFileName[1] + ")");
                    splitName = fullFileName[1].split("\\.");
                    stations.put(Integer.parseInt(splitName[0]), file.getName());
                    Log.v("", String.valueOf(splitName[0]));
                }
            }
            //
            cityMap.put(directories[i].getName(), stations);
        }
        // информация RDS
        stationName = findViewById(R.id.stationName);
        stationRDSText = findViewById(R.id.stationRDS);
        stationFREQ = findViewById(R.id.stationFREQ);

        // диалоговое окно
        dialogContainer = findViewById(R.id.dialog_container);

        openConnection(SERVER_IP, SERVER_PORT);


        searchInput = findViewById(R.id.searchInput);
        StationRecyclerview = findViewById(R.id.savedStationsList);

        // Массив
        mData = new ArrayList<>();
        mData.add(new StationItem("Дача"));
        mData.add(new StationItem("Rock fm"));

        StationAdapter = new StationAdapter(this, mData, this);
        StationRecyclerview.setAdapter(StationAdapter);
        StationRecyclerview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(StationRecyclerview);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                StationAdapter.getFilter().filter(s);
                search = s;
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        watch = findViewById(R.id.watch);
        timeUpdateTimer();

    }

    public void timeUpdateTimer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(200);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Calendar calendar = Calendar.getInstance();
                                //SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMMM-yyyy");
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                                String dateTime = simpleDateFormat.format(calendar.getTime());
                                watch.setText(dateTime);
                            }});
                    } catch (Exception e) {
                    }
                }
            }
        }).start();
    }

    ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.DOWN | ItemTouchHelper.ACTION_STATE_DRAG) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            mData.remove(viewHolder.getLayoutPosition());
            StationAdapter.notifyDataSetChanged();
        }
    };

    private Button mBtnSend  = null;
    private Button mBtnClose = null;

    private vip.mannheim.radio.Connection mConnect = null;

    public void openConnection(String host, int port)
    {
        // Создание подключения
        String HOST = host;
        int PORT = port;
        mConnect = new Connection(HOST, PORT);

        // Открытие сокета в отдельном потоке
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    mConnect.openConnection();
                    Log.d(Connection.LOG_TAG, "Соединение установлено");
                    Log.d(Connection.LOG_TAG, "(mConnect != null) = " + (mConnect != null));

                    mConnect.getData(getApplicationContext(), handler);

                } catch (Exception e) {
                    Log.e(Connection.LOG_TAG, e.getMessage());
                    mConnect = null;
                }
            }
        }).start();
    }

    public void send(final String text)
    {
        if (mConnect == null) {
            Log.d(Connection.LOG_TAG, "Соединение не установлено");
            openConnection(SERVER_IP, SERVER_PORT);
        }  else {
            Log.d(Connection.LOG_TAG, "Отправка сообщения");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        /* отправляем на сервер данные */
                        mConnect.sendData(text);
                    } catch (Exception e) {
                        Log.e(Connection.LOG_TAG, Objects.requireNonNull(e.getMessage()));
                        runOnUiThread (new Thread(new Runnable() {
                            public void run() {
                                openConnection(SERVER_IP, SERVER_PORT);
                            }
                        }));
                    }
                }
            }).start();
        }
    }

    public void closeConnection()
    {
        // Закрытие соединения
        mConnect.closeConnection();
        // Блокирование кнопок
        mBtnSend .setEnabled(false);
        mBtnClose.setEnabled(false);
        Log.d(Connection.LOG_TAG, "Соединение закрыто");
    }

    @SuppressLint("HandlerLeak")
    public Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String bundleMsg = bundle.getString("MSG"); // rdsn>MOSKVA

            String[] strings = bundleMsg.split(">");
            if(strings[0].equals("rdsn")){ // msg:rdsn>MOSKVA
                stationName.setText(strings[1]);
                Log.d(Connection.LOG_TAG, strings[1]);
            }else if(strings[0].equals("rdst")){ // rdst>Sinedown - Through the Ghost
                stationRDSText.setText(strings[1]);
                Log.d(Connection.LOG_TAG, strings[1]);
            }else if(strings[0].equals("frq")){ // frq>8700
                int frqInt = Integer.parseInt(strings[1]);
                Float frqFloat = Float.parseFloat(strings[1])/100;
                if(frqInt >= MIN_FRQ && frqInt <= MAX_FRQ){
                    lastFREQ = frqInt;
                }
                stationFREQ.setText(String.valueOf( frqFloat + " МГц"));
                Log.d(Connection.LOG_TAG, frqFloat + " МГц");
            }else if(strings[0].equals("vol")){ // vol>15
                int vol = Integer.parseInt(strings[1]);
                if(vol >= MIN_VOL && vol <= MAX_VOL){
                    currentVolume = vol;
                }
                Log.d(Connection.LOG_TAG, strings[1]);
            }else if(strings[0].equals("mute")){ // vol>15

                if(strings[1] == "muted"){
                    muteVolume = true;
                }else if(strings[1] == "unMuted"){
                    muteVolume = false;
                }
                Log.d(Connection.LOG_TAG, strings[1]);
            }
        }
    };

    // Кнопки:
    public void buttonSeekUpOnClick(View view) {
        logoStationsStartAnimation(view, true);
    }

    public void buttonSeekDownOnClick(View view) {
        logoStationsStartAnimation(view, false);
    }

    public void buttonStationsOnClick(View view) {

    }

    void logoStationsStartAnimation(final View view, final boolean seekUp){
        relativeLayout = findViewById(R.id.forCustomView);
        Animation animation;

        // Направление анимации. Зависит от SeekUp/SeekDown
        if(seekUp) {
            animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.station_anim_up_out);
        }else{
            animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.station_amin_down_out);
        }

        relativeLayout.startAnimation(animation);
        relativeLayout.setVisibility(View.INVISIBLE);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

                Animation textAnimation = AnimationUtils.loadAnimation(view.getContext(), R.anim.text_out);

                stationName.startAnimation(textAnimation);
                stationName.setVisibility(View.INVISIBLE);
                stationRDSText.startAnimation(textAnimation);
                stationRDSText.setVisibility(View.INVISIBLE);
                stationFREQ.startAnimation(textAnimation);
                stationFREQ.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                relativeLayout.removeAllViews(); // Удаляем views
                logoStationsStopAnimation(view, seekUp);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }
    void logoStationsStopAnimation(final View view, boolean seekUp){

        int wrapContent = RelativeLayout.LayoutParams.WRAP_CONTENT;
        ConstraintLayout.LayoutParams lParams = new ConstraintLayout.LayoutParams(
                wrapContent, wrapContent);
        lParams.setMargins(530, 380,0,0);

        CoverView coverView = new CoverView(this);
        relativeLayout.addView(coverView, lParams);

        String fileName;
        //Log.v("", Objects.requireNonNull(cityMap.get("msk").get(10370)));
        if(seekUp) {
            for (int i = lastFREQ; i <= MAX_FRQ; i +=10) {
                if (((fileName = cityMap.get(locationCity).get(i)) != null) && i != lastFREQ){
                    lastFREQ = i;
                    coverView.setVariable(BitmapFactory.decodeFile(pathToImagesDir + locationCity + "/" + fileName));
                    Log.v("Инфо загрузки файла", "File: " + fileName);

                    Float frqFloat = Float.valueOf(lastFREQ)/100;
                    stationFREQ.setText(String.format("%s МГц", frqFloat));
                    break;
                }
                if(i == MAX_FRQ){
                    lastFREQ = i;
                    Float frqFloat = Float.valueOf(lastFREQ)/100;
                    stationFREQ.setText(String.format("%s МГц", frqFloat));
                }
            }
        }else if(!seekUp) {
            for (int i = lastFREQ; i >= MIN_FRQ; i -=10) {
                if (((fileName = cityMap.get(locationCity).get(i)) != null) && i != lastFREQ) {
                    lastFREQ = i;
                    coverView.setVariable(BitmapFactory.decodeFile(pathToImagesDir + locationCity + "/" + fileName));
                    Log.v("Инфо загрузки файла", "File: " + fileName);

                    Float frqFloat = Float.valueOf(lastFREQ)/100;
                    stationFREQ.setText(String.format("%s МГц", frqFloat));
                    break;
                }
                if(i == MIN_FRQ){
                    lastFREQ = i;
                    Float frqFloat = Float.valueOf(lastFREQ)/100;
                    stationFREQ.setText(String.format("%s МГц", frqFloat));
                }
            }
        }

        send("f:" + String.valueOf(lastFREQ));
        Log.v("Частота", String.valueOf(lastFREQ));
        Log.v("Путь", pathToImagesDir + locationCity + "/");

        Animation animation;

        if(seekUp) {
            animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.station_anim_up_in);
        }else{
            animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.station_amin_down_in);
        }

        relativeLayout.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                Animation textAnimation = AnimationUtils.loadAnimation(view.getContext(), R.anim.text_in);

                stationName.startAnimation(textAnimation);
                stationName.setVisibility(View.VISIBLE);
                stationRDSText.startAnimation(textAnimation);
                stationRDSText.setVisibility(View.VISIBLE);
                stationFREQ.startAnimation(textAnimation);
                stationFREQ.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                relativeLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

    }

    public void buttonVolumeOnClick(final View view) {

        dialogContainer.removeAllViews(); // Удаляем views
        // диалог
        dialogView = getLayoutInflater()
                .inflate(R.layout.volume_set, null);
        int wrapContent = RelativeLayout.LayoutParams.MATCH_PARENT;
        ConstraintLayout.LayoutParams lParams = new ConstraintLayout.LayoutParams(
                wrapContent, wrapContent);

        dialogContainer.addView(dialogView, lParams);
        dialogContainer.setVisibility(View.VISIBLE);

        constraintLayoutVolume = findViewById(R.id.volume_widget);

        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.volume_anim_in);
        constraintLayoutVolume.startAnimation(animation);
        constraintLayoutVolume.setVisibility(View.VISIBLE);

        imageViewMute = (ImageView) findViewById(R.id.buttonMute);
        if(muteVolume) {
            imageViewMute.setImageResource(R.drawable.icon_mute_active);
        }else {
            imageViewMute.setImageResource(R.drawable.icon_mute);
        }

        imageViewMute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(muteVolume) {
                    imageViewMute.setImageResource(R.drawable.icon_mute);
                    muteVolume = false;
                    send("c:mute");
                }else {
                    imageViewMute.setImageResource(R.drawable.icon_mute_active);
                    muteVolume = true;
                    send("c:unMute");
                }
                volumeTimer.cancel();
                volumeTimer(view);
            }
        });

        seekBar = (SeekBar) findViewById(R.id.seekBarBrightness);
        seekBar.setProgress(currentVolume);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int volume = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volumeTimer.cancel();
                volumeTimer(getWindow().getDecorView());
                volume = progress;
                Log.v("Volume: ", String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                volumeTimer.cancel();
                volumeTimer(getWindow().getDecorView());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                volumeTimer.cancel();
                volumeTimer(getWindow().getDecorView());
                send("c:v:" + volume);
            }
        });

        volumeTimer(view);
    }

    void volumeTimer(final View view){

        //Log.d(TAG, "Restarting timer");
        if (volumeTimer != null) {
            volumeTimer.cancel();
        }
        volumeTimer = new Timer();
        volumeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.volume_anim_out);
                        constraintLayoutVolume.startAnimation(animation);
                        constraintLayoutVolume.setVisibility(View.INVISIBLE);

                    }});
            }
        }, 3000);
    }

    public void buttonTAOnClick(View view) {

    }

    public void buttonSoundSettingsOnClick(View view) {
        //startActivity(new Intent(MainActivity.this, SoundSettings.class));
        //overridePendingTransition(R.anim.activity_anim_in, R.anim.activity_anim_out);
    }

    public void buttonStationPlusOnClick(View view) {
        dialogContainer.removeAllViews(); // Удаляем views
        // диалог
        dialogView = getLayoutInflater()
                .inflate(R.layout.dialog, null);
        int wrapContent = RelativeLayout.LayoutParams.MATCH_PARENT;
        ConstraintLayout.LayoutParams lParams = new ConstraintLayout.LayoutParams(
                wrapContent, wrapContent);

        dialogContainer.addView(dialogView, lParams);

        button_yes = findViewById(R.id.button_yes);
        dialogContainer.setVisibility(View.VISIBLE);

        message_safe_dialog = findViewById(R.id.message_safe_dialog);

        if(stationName.getText() == ""){
            message_safe_dialog.setText("Данные не обновлены");
        }

        button_yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Название станции берём с верхней стоки
                if(stationName.getText() != ""){
                    addItem((String) stationName.getText());
                }
                dialogContainer.setVisibility(View.GONE);
            }
        });

        dialogContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogContainer.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(),
                        "container",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Добавляем новую запись в избранные станции
    private void addItem(String item) {
        mData.add(new StationItem(item));
        StationAdapter.notifyDataSetChanged();
    }

    public void stationsListBackwardOnClick(View view) {
        StationRecyclerview.post(new Runnable() {
            @Override
            public void run() {
                StationRecyclerview.smoothScrollBy(-630, 0);
                StationRecyclerview.getChildCount();
            }
        });
    }

    public void stationsListForwardOnClick(View view) {
        StationRecyclerview.post(new Runnable() {
            @Override
            public void run() {
                StationRecyclerview.smoothScrollBy(630, 0);
                StationRecyclerview.getChildCount();
            }
        });
    }

    public void iconSettingsOnClick(View view) {
        //startActivity(new Intent(MainActivity.this, GlobalSettings.class));
        //overridePendingTransition(R.anim.activity_anim_in, R.anim.activity_anim_out);
    }

    @Override
    public void onStationClick(int position, View view) {
        Toast.makeText(getApplicationContext(),
                "Станция: " + mData.get(position).getContent(),
                Toast.LENGTH_SHORT).show();
        // Скрыть клавиатуру
        InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        // Отчистить ввод
        searchInput.getText().clear();
        // убрать фокус
        searchInput.clearFocus();
    }
}