package com.priot.util;

import java.util.Date;

public abstract class Stdio {

    public static void err(String s) {
        System.err.println(new Date().toString() + " - Error: " + s);
        System.exit(1);
    }

    public static void pout(String s) {
        System.out.println(s);
    }

    public static void dot() {
        System.out.print('.');
    }

    public static void println() {
        System.out.print('\n');
    }

}
