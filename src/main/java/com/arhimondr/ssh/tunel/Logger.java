package com.arhimondr.ssh.tunel;


import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    private static final ThreadLocal<SimpleDateFormat> sdf = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        }
    };


    public static void log(String message) {
        System.out.println(sdf.get().format(new Date()) + " --- " + message);
    }
}
