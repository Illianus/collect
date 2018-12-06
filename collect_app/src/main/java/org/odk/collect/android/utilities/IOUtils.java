package org.odk.collect.android.utilities;

import android.annotation.TargetApi;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.Channel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class IOUtils {

    public static void closeQuietly(OutputStream stream) {
        try {
            stream.close();
        } catch (IOException e) {
            //ignore
        }
    }

    public static void closeQuietly(OutputStreamWriter writer) {
        try {
            writer.close();
        } catch (IOException e) {
            //ignore
        }
    }

    public static void forceDelete(File dbFile) throws IOException {
        boolean result = dbFile.delete();
        if (!result) throw new IOException();
    }

    public static void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (IOException e) {
            //ignore
        }
    }

    public static void closeQuietly(Channel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            //ignore
        }
    }

    public static void moveFileToDirectory(File mediaFile, File formMediaPath, boolean b) throws FileNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            moveFileToDirectoryO(mediaFile, formMediaPath);
        } else {
            if (!formMediaPath.exists()) formMediaPath.mkdirs();
            if (!mediaFile.exists()) throw new FileNotFoundException("File not found!");
            File destination = new File(formMediaPath.getPath() + mediaFile.getName());
            if (destination.exists()) destination.delete();
            try {
                try (FileInputStream reader = new FileInputStream(mediaFile)) {
                    try (FileOutputStream writer = new FileOutputStream(destination)) {
                        byte[] buffer = new byte[1024 * 1024];
                        int read;
                        while ((read = reader.read(buffer, 0, 1024 * 1024)) > 0) {
                            writer.write(buffer, 0, read);
                        }
                        reader.close();
                    }
                }
            } catch (IOException e) {
                //ignore
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static void moveFileToDirectoryO(File mediaFile, File formMediaPath) {
        try {
            Files.move(mediaFile.toPath(), formMediaPath.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            //ignore
        }
    }

    public static void copy(InputStream stream, OutputStream outputStream) {
        try {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = stream.read(buffer, 0, 1024 * 1024)) > 0) {
                outputStream.write(buffer, 0, read);
            }
            stream.close();
            outputStream.close();
        } catch (IOException e) {
            //ignore
        }
    }
}
