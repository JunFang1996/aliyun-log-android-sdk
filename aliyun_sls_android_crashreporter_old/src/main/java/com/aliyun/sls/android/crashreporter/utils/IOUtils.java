package com.aliyun.sls.android.crashreporter.utils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import android.content.Context;
import android.text.TextUtils;

/**
 * @author gordon
 * @date 2021/10/15
 */
public class IOUtils {
    private IOUtils() {
        //no instance
    }

    public static String getUUID(Context context) {
        final File uniqueFile = new File(context.getFilesDir(), "sls_crash_reporter/itrace/tags/unique");
        return IOUtils.readLineFromFile(uniqueFile);
    }

    public static String readLineFromFile(File file) {
        if (!file.exists()) {
            return null;
        }

        BufferedReader bufferedReader = null;
        try {
            final FileReader fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);

            String line;
            while (null != (line = bufferedReader.readLine())) {
                if (TextUtils.isEmpty(line)) {
                    continue;
                }
                break;
            }
            bufferedReader.close();

            return line;
        } catch (Throwable e) {
            e.printStackTrace();
            IOUtils.close(bufferedReader);
            return null;
        }
    }

    public static void close(Closeable closeable) {
        if (null == closeable) {
            return;
        }

        try {
            closeable.close();
        } catch (IOException e) {
            // ignore
        }
    }
}
