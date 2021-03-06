package ca.jason.morphimage;

import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by jason on 2017-04-08.
 */

public class ImageExport implements Runnable {
    private int     i;
    private String  fileName, baseName;
    private Bitmap bmp;
    private boolean complete;

    public ImageExport(int i, Bitmap bmp, String baseName) {
        this.i = i;
        this.bmp = bmp;
        this.baseName = baseName;
        complete = false;
    }

    public void run() {
        fileName = exportImageToFile(baseName + "_" + i, bmp);
        bmp = null;
        complete = true;
    }

    public String getFileName() {
        return fileName;
    }

    public int getI() {
        return i;
    }

    public boolean isComplete() {
        return complete;
    }

    /**
     * Exports given bitmap to a PNG file in the application directory on external storage.
     *
     * @param fn Filename to save to
     * @param bmp Bitmap to save
     * @return String Absolute path to saved file
     */
    private String exportImageToFile(String fn, Bitmap bmp) {
        File f = new File(Environment.getExternalStorageDirectory() + MainActivity.APP_DIRECTORY
                + File.separator + fn + ".png");
        try {
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fo);
            fo.close();
        } catch (IOException e) {
            System.out.println("Couldn't create file: " + f.getAbsolutePath());
        }
        return f.getAbsolutePath();
    }
}
