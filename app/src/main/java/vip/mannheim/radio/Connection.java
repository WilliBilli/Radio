package vip.mannheim.radio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Connection extends AppCompatActivity
{

    private Socket mSocket = null;
    private PrintWriter output;
    private BufferedReader input;
    private String mHost;

    private int mPort;

    public static final String LOG_TAG = "SOCKET LOG";

    public Connection(final String host, final int port)
    {
        this.mHost = host;
        this.mPort = port;
    }

    /**
     * Метод открытия сокета
     */
    public void openConnection() throws Exception
    {
        // Если сокет уже открыт, то он закрывается
        try {
            // Создание сокета
            mSocket = new Socket(mHost, mPort);
            output = new PrintWriter(mSocket.getOutputStream());
            input = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
        } catch (IOException e) {
            throw new Exception("Невозможно создать сокет: "+e.getMessage());
        }

    }

    @SuppressLint("ResourceType")
    public void getData(Context context, Handler handler) throws Exception {

        try {
            // Определение входного потока
            InputStream is = mSocket.getInputStream();
            // Буфер для чтения информации
            byte[] data = new byte[1024*4];

            Intent mIntent = new Intent(context, Splash.class);

            while(mSocket.isConnected()){
                // Получение информации : count - количество полученных байт
                int count = is.read(data,0, data.length);

                // Никогда не следует повторно использовать объект Message. Каждый раз нужно создавать новый
                Message msgGetData = handler.obtainMessage();
                Bundle bundle = new Bundle();

                if (count > 0) {
                    String msg = new String(data, 0, count);

                    // Вывод в консоль сообщения
                    Log.e(LOG_TAG, "Сообщение: " + msg);

                    if(msg.indexOf(":") > 0 && msg.indexOf(":") < msg.length()){

                        String[] strings = msg.split(":");

                        if(strings[0].equals("msg")){ // msg:rdsn>MOSKVA
                            bundle.putString("MSG", strings[1]);
                            msgGetData.setData(bundle);
                            handler.sendMessage(msgGetData);
                        }

                        if(strings[0].equals("turn") && strings[1].trim().equals("on")) {
                            context.stopService(mIntent);
                            mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mIntent.putExtra("power", "on");

                            context.startActivity(mIntent);
                        }

                        if(strings[0].equals("turn") && strings[1].trim().equals("off")) {
                            context.stopService(mIntent);
                            mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mIntent.putExtra("power", "off");

                            context.startActivity(mIntent);
                        }

                        if(strings[0].equals("turn") && strings[1].trim().equals("son")) {
                            context.stopService(mIntent);
                            mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mIntent.putExtra("screen", "on");

                            context.startActivity(mIntent);
                        }

                        if(strings[0].equals("turn") && strings[1].trim().equals("soff")) {
                            context.stopService(mIntent);
                            mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mIntent.putExtra("screen", "off");

                            context.startActivity(mIntent);
                        }
                    }
                } else if (count == -1 ) {
                    // Если count=-1, то поток прерван
                    System.out.println("socket is closed");
                    closeConnection();
                    break;
                }
            }
        } catch (IOException e){
            throw new Exception("Ошибка getData(): " + e.getMessage());
        }
    }
    /**
     * Метод закрытия сокета
     */
    public void closeConnection()
    {
        /* Проверяем сокет. Если он не зарыт, то закрываем его и освобдождаем соединение.*/
        if (mSocket != null && !mSocket.isClosed()) {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Невозможно закрыть сокет: " + e.getMessage());
            } finally {
                mSocket = null;
            }
        }
        mSocket = null;
    }
    /**
     * Метод отправки данных
     */
    public void sendData(String data) throws Exception {

        // Проверка открытия сокета
        if (mSocket == null || mSocket.isClosed()) {
            throw new Exception("Невозможно отправить данные. Сокет не создан или закрыт");
        }
        // Отправка данных
        output.println(data);
        output.flush();
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        closeConnection();
    }
}
