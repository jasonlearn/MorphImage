package ca.jason.morphimage;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public final static String PHOTO_PATH = "ca.jason.morphimage.FILE_PATHS";
    public final static String APP_DIRECTORY = File.separator + "MorthImage";

    private final static int CAMERA_IMG_REQUEST = 1;
    private final static int GALLERY_IMG_REQUEST = 2;
    private final static int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    private Bitmap[] warpComplete, crossComplete;
    private ImageExport[] imageExports;
    private ImageView selectedView;
    private Pair<Integer, Integer> selectedLine;
    private ProgressBar progressBar;
    private TextView progressText;
    private String[]                filePaths;
    private String                  baseName;
    private ArrayList<Thread>       exportThreads;
    private int                     progress;
    private int                     currFrame;
    private int                     frames;
    private boolean                 isDrawingLine = false;
    private boolean                 isMorphing = false;
    private char                    state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        ImageViewEdit image = (ImageViewEdit) findViewById(R.id.image_view_left);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setOnTouchListener(imgTouchListener);
        image = (ImageViewEdit) findViewById(R.id.image_view_right);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setOnTouchListener(imgTouchListener);
        NumberPicker np = (NumberPicker) findViewById(R.id.number_picker_frames);
        np.setMinValue(1);
        np.setMaxValue(30);
        np.setValue(1);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressText = (TextView) findViewById(R.id.text_view_progress);
        state = 'a';
        File folder = new File(Environment.getExternalStorageDirectory() + APP_DIRECTORY);
        if (!folder.exists())
            folder.mkdir();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        //int id = item.getItemId();

        //if (id == R.id.nav_camera) {
            // Handle the camera action
        //} else if (id == R.id.nav_gallery) {

        //} else if (id == R.id.nav_slideshow) {

        //} else if (id == R.id.nav_manage) {

        //} else if (id == R.id.nav_share) {

        //} else if (id == R.id.nav_send) {

        //}

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    View.OnTouchListener imgTouchListener = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent e) {
            ImageViewEdit viewEdit = (ImageViewEdit) v;
            if (e.getAction() == MotionEvent.ACTION_DOWN)
                getGalleryImg(viewEdit);
            return true;
        }
    };

    public void getGalleryImg(View v) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            selectedView = (ImageView) v;
            startActivityForResult(pickPhoto, GALLERY_IMG_REQUEST);
        }
    }

    View.OnTouchListener editTouchListener = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent e) {
            ImageViewEdit dv = (ImageViewEdit) v;
            ImageViewEdit opposite;
            opposite = (ImageViewEdit) (dv.getId() == R.id.image_view_left ?
                    findViewById(R.id.image_view_right) : findViewById(R.id.image_view_left));
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (selectedLine == null) {
                        selectedLine = dv.getClosestPointIndex((int) e.getX(), (int) e.getY());
                        dv.setSelectedLine(selectedLine.first);
                        opposite.setSelectedLine(selectedLine.first);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (selectedLine != null) {
                        dv.getLine(selectedLine.first).setPoint(selectedLine.second, new Point((int) e.getX(), (int) e.getY()));
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    selectedLine = null;
                    break;
            }
            dv.invalidate();
            return true;
        }
    };

    View.OnTouchListener lineTouchListener = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent e) {
            ImageViewEdit dv = (ImageViewEdit) v;
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isDrawingLine = true;
                    dv.addLine(new Point((int) e.getX(), (int) e.getY()), new Point((int) e.getX(), (int) e.getY()));
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (isDrawingLine)
                        dv.getLine(dv.getLastLine()).setEnd(new Point((int) e.getX(), (int) e.getY()));
                    break;
                case MotionEvent.ACTION_UP:
                    if (isDrawingLine) {
                        isDrawingLine = false;
                        ImageViewEdit opposite;
                        opposite = (ImageViewEdit) (dv.getId() == R.id.image_view_left ?
                                findViewById(R.id.image_view_right) : findViewById(R.id.image_view_left));
                        LineController last = dv.getLine(dv.getLastLine());
                        opposite.addLine(new LineController(last));
                        opposite.invalidate();
                    }
                    break;
            }
            dv.invalidate();
            return true;
        }
    };

    public void startEditMode(View v) {
        ImageViewEdit dvLeft = (ImageViewEdit) findViewById(R.id.image_view_left);
        ImageViewEdit dvRight = (ImageViewEdit) findViewById(R.id.image_view_right);
        if (state != 'e') {
            if (dvLeft.getLastLine() > -1) {
                dvLeft.setOnTouchListener(editTouchListener);
                dvRight.setOnTouchListener(editTouchListener);
                dvLeft.setBackgroundResource(R.drawable.iv_edit);
                dvRight.setBackgroundResource(R.drawable.iv_edit);
            }
            Toast.makeText(getApplicationContext(),
                    "Edit Mode ENABLED", Toast.LENGTH_SHORT).show();
            state = 'e';
        } else {
            dvLeft.setOnTouchListener(lineTouchListener);
            dvRight.setOnTouchListener(lineTouchListener);
            dvLeft.setBackgroundResource(0);
            dvRight.setBackgroundResource(0);
            Toast.makeText(getApplicationContext(),
                    "Edit Mode DISABLED", Toast.LENGTH_SHORT).show();
            state = 'a';
        }
    }

    public void resetViews(View v) {
        ImageViewEdit image = (ImageViewEdit) findViewById(R.id.image_view_left);
        image.resetView();
        image.setOnTouchListener(imgTouchListener);
        image = (ImageViewEdit) findViewById(R.id.image_view_right);
        image.resetView();
        image.setOnTouchListener(imgTouchListener);
        Toast.makeText(getApplicationContext(),
                "Images Reset", Toast.LENGTH_SHORT).show();
    }

    public void toggleLines(View v) {
        ImageViewEdit lView = (ImageViewEdit) findViewById(R.id.image_view_left),
                rView = (ImageViewEdit) findViewById(R.id.image_view_right);
        lView.toggleLines();
        rView.toggleLines();
    }

    public void deleteLine(View v) {
        ImageViewEdit lView = (ImageViewEdit) findViewById(R.id.image_view_left),
                rView = (ImageViewEdit) findViewById(R.id.image_view_right);
        lView.deleteSelected();
        rView.deleteSelected();
        Toast.makeText(getApplicationContext(),
                "Selected Line Deleted", Toast.LENGTH_SHORT).show();
    }

    public void startMorph(View v) {
        if (!isMorphing) {
            // Initializing all variables required for morph
            isMorphing = true;
            NumberPicker np = (NumberPicker) findViewById(R.id.number_picker_frames);
            frames = np.getValue();
            filePaths = new String[frames + 2];
            exportThreads = new ArrayList<>();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd-hhmmss");
            baseName = simpleDateFormat.format(new Date());

            // Exports source and destination images
            Thread eStartEnd = new Thread(new Runnable() {
                public void run() {
                    ImageViewEdit lView = (ImageViewEdit) findViewById(R.id.image_view_left),
                            rView = (ImageViewEdit) findViewById(R.id.image_view_right);
                    filePaths[0] = exportImageToFile(baseName + "_0", lView.getBitmap());
                    filePaths[frames + 1] = exportImageToFile(baseName + "_" + (frames + 1), rView.getBitmap());
                }
            });
            eStartEnd.start();
            exportThreads.add(eStartEnd);

            // Initializes progress bar
            imageExports = new ImageExport[frames];
            progress = 0;
            progressBar.setProgress(progress);
            progressBar.setMax(frames + 1);
            progressBar.setVisibility(View.VISIBLE);
            progressText.setText("Preparing for Warp...");
            progressText.setVisibility(View.VISIBLE);

            // Creates new running thread for computation
            new Thread(new Runnable() {
                public void run() {
                    ImageViewEdit lView = (ImageViewEdit) findViewById(R.id.image_view_left),
                            rView = (ImageViewEdit) findViewById(R.id.image_view_right);
                    LineController[] lLines = lView.getLineArray(),
                            rLines = rView.getLineArray();

                    // Interpolating intermediate line arrays for each frame
                    LineController[][] midLines = new LineController[frames][rLines.length];
                    for (int i = 0; i < rLines.length; i++) {
                        double stepX1 = (rLines[i].getStart().x - lLines[i].getStart().x) / (frames + 1),
                                stepY1 = (rLines[i].getStart().y - lLines[i].getStart().y) / (frames + 1),
                                stepX2 = (rLines[i].getEnd().x - lLines[i].getEnd().x) / (frames + 1),
                                stepY2 = (rLines[i].getEnd().y - lLines[i].getEnd().y) / (frames + 1),
                                x1 = lLines[i].getStart().x,
                                y1 = lLines[i].getStart().y,
                                x2 = lLines[i].getEnd().x,
                                y2 = lLines[i].getEnd().y;
                        for (int j = 0; j < frames; j++) {
                            x1 += stepX1;
                            y1 += stepY1;
                            x2 += stepX2;
                            y2 += stepY2;
                            midLines[j][i] = new LineController(new Point((int) x1, (int) y1), new Point((int) x2, (int) y2));
                        }
                    }

                    // Initializes arrays to hold completed bitmaps
                    warpComplete = new Bitmap[frames * 2];
                    crossComplete = new Bitmap[frames];
                    progressBar.post(new Runnable() {
                        public void run() {
                            progressBar.setMax(4);
                        }
                    });

                    // Frame processing loop
                    for (currFrame = 0; currFrame < frames; currFrame++) {
                        progress = 0;
                        updateProgress();
                        progressText.post(new Runnable() {
                            public void run() {
                                progressText.setText("Processing Frame " + (currFrame + 1) + "/" + frames);
                            }
                        });

                        // Warp source to intermediate frame
                        warpSingleFrame(lLines, midLines[currFrame], lView, currFrame * 2);
                        incrementProgress();
                        // Warp destination to intermediate frame
                        warpSingleFrame(rLines, midLines[currFrame], rView, currFrame * 2 + 1);
                        incrementProgress();

                        // Cross-dissolve the results of the warp
                        crossFrame(warpComplete[currFrame * 2], warpComplete[currFrame * 2 + 1], currFrame);
                        incrementProgress();
                        // Clear result of warp to allow Garbage Collector to free up memory
                        warpComplete[currFrame * 2] = null;
                        warpComplete[currFrame * 2 + 1] = null;

                        // Start export to PNG process on cross-dissolved result
                        imageExports[currFrame] = new ImageExport(currFrame + 1, crossComplete[currFrame], baseName);
                        crossComplete[currFrame] = null; // Cleaning up memory
                        Thread eFrame = new Thread(imageExports[currFrame]);
                        eFrame.start();
                        exportThreads.add(eFrame);
                    }

                    // Setting up progress bar for export phase
                    progressText.post(new Runnable() {
                        public void run() {
                            progressText.setText("Finishing Exports...");
                        }
                    });
                    progress = 0;
                    updateProgress();
                    progressBar.post(new Runnable() {
                        public void run() {
                            progressBar.setMax(frames);
                        }
                    });

                    // Wait for Export Workers to finish exporting to PNG
                    for (ImageExport ie : imageExports) {
                        while (!ie.isComplete()) ;
                        filePaths[ie.getI()] = ie.getFileName();
                        incrementProgress();
                    }

                    // Open FrameDisplayActivity to show final results
                    openFrameDisplay();

                    // Hide ProgressBar once complete
                    progressBar.post(new Runnable() {
                        public void run() {
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    });
                    progressText.post(new Runnable() {
                        public void run() {
                            progressText.setVisibility(View.INVISIBLE);
                        }
                    });
                    isMorphing = false;
                }
            }).start();
        } else
            Toast.makeText(getApplicationContext(),
                    "Already Morphing. Please Wait...", Toast.LENGTH_SHORT);
    }

    private void warpSingleFrame(LineController[] l, LineController[] r, ImageViewEdit dv, int frameID) {
        int numLines = l.length;
        int arrSize = numLines * 2;
        LineController[] sLines = l;
        LineController[] dLines = r;
        float[] pP = new float[arrSize]; // Point P
        float[] pQ = new float[arrSize]; // Point Q
        float[] pP2 = new float[arrSize]; // Point P'
        float[] pQ2 = new float[arrSize]; // Point Q'
        float[] vPQ = new float[arrSize]; // Vector PQ
        float[] vN = new float[arrSize]; // Vector Normal to PQ
        float[] vPQ2 = new float[arrSize]; // Vector P'Q'
        float[] vN2 = new float[arrSize]; // Vector Normal to P'Q'

        for (int i = 0; i < numLines; i++) {
            int x = i * 2, y = i * 2 + 1;
            pP[x] = dLines[i].getStart().x;
            pP[y] = dLines[i].getStart().y;
            pQ[x] = dLines[i].getEnd().x;
            pQ[y] = dLines[i].getEnd().y;
            pP2[x] = sLines[i].getStart().x;
            pP2[y] = sLines[i].getStart().y;
            pQ2[x] = sLines[i].getEnd().x;
            pQ2[y] = sLines[i].getEnd().y;

            vPQ[x] = pQ[x] - pP[x];
            vPQ[y] = pQ[y] - pP[y];
            vN[x] = vPQ[y] * -1;
            vN[y] = vPQ[x];
            vPQ2[x] = pQ2[x] - pP2[x];
            vPQ2[y] = pQ2[y] - pP2[y];
            vN2[x] = vPQ2[y] * -1;
            vN2[y] = vPQ2[x];
        }

        Bitmap inBmp = dv.getBitmap();
        Bitmap outBmp = Bitmap.createBitmap(inBmp.getWidth(), inBmp.getHeight(), Bitmap.Config.ARGB_8888);

        // Initialize RenderScript object
        //RenderScript rs = RenderScript.create(this);
        //ScriptC_warp warpScript = new ScriptC_warp(rs);

        // Allocate non-movable memory for Bitmaps
        //Allocation inAlloc = Allocation.createFromBitmap(rs, inBmp);
        //Allocation outAlloc = Allocation.createFromBitmap(rs, outBmp);

        // Allocate non-movable memory for float arrays
        //Allocation apP = Allocation.createSized(rs, Element.F32(rs), arrSize);
        //apP.copyFrom(pP);
        //Allocation apQ = Allocation.createSized(rs, Element.F32(rs), arrSize);
        //apQ.copyFrom(pQ);
        //Allocation apP2 = Allocation.createSized(rs, Element.F32(rs), arrSize);
        //apP2.copyFrom(pP2);
        //Allocation apQ2 = Allocation.createSized(rs, Element.F32(rs), arrSize);
        //apQ2.copyFrom(pQ2);
        //Allocation avPQ = Allocation.createSized(rs, Element.F32(rs), arrSize);
        //avPQ.copyFrom(vPQ);
        //Allocation avN = Allocation.createSized(rs, Element.F32(rs), arrSize);
        //avN.copyFrom(vN);
        //Allocation avPQ2 = Allocation.createSized(rs, Element.F32(rs), arrSize);
        //avPQ2.copyFrom(vPQ2);
        //Allocation avN2 = Allocation.createSized(rs, Element.F32(rs), arrSize);
        //avN2.copyFrom(vN2);

        // Assign variables
        //warpScript.set_apP(apP);
        //warpScript.set_apQ(apQ);
        //warpScript.set_apP2(apP2);
        //warpScript.set_apQ2(apQ2);
        //warpScript.set_avPQ(avPQ);
        //warpScript.set_avN(avN);
        //warpScript.set_avPQ2(avPQ2);
        //warpScript.set_avN2(avN2);
        //warpScript.set_inImage(inAlloc);
        //warpScript.set_height(inBmp.getHeight());
        //warpScript.set_width(inBmp.getWidth());
        //warpScript.set_xOff(dv.leftBound);
        //warpScript.set_yOff(dv.topBound);
        //warpScript.set_num_lines(numLines);

        // Iterate per pixel on Bitmap
        //warpScript.forEach_root(outAlloc, outAlloc);
        // Point outBmp to allocated output
        //outAlloc.copyTo(outBmp);

        // Place in completed frame array
        warpComplete[frameID] = outBmp;
    }

    private void crossFrame(Bitmap img1, Bitmap img2, int frameID) {
        int w1 = img1.getWidth(),
                h1 = img1.getHeight(),
                w2 = img2.getWidth(),
                h2 = img2.getHeight();

        int width = w1 > w2 ? w1 : w2;
        int height = h1 > h2 ? h1 : h2;

        // Create new bitmaps that will fit both source images
        Bitmap fullBmp1 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Bitmap fullBmp2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas c1 = new Canvas(fullBmp1);
        c1.drawColor(Color.WHITE);
        c1.drawBitmap(img1, (width - w1) / 2, (height - h1) / 2, null);

        Canvas c2 = new Canvas(fullBmp2);
        c2.drawColor(Color.WHITE);
        c2.drawBitmap(img2, (width - w2) / 2, (height - h2) / 2, null);

        // Calculate fade weighting
        float cross2 = (float) (frameID + 1) / (frames + 1);
        float cross = 1 - cross2;

        Bitmap crossBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Initialize RenderScript object
        //RenderScript rs = RenderScript.create(this);
        //ScriptC_crossDissolve crossScript = new ScriptC_crossDissolve(rs);

        // Allocate non-movable memory for both inputs and output bitmap
        //Allocation inAlloc1 = Allocation.createFromBitmap(rs, fullBmp1);
        //Allocation inAlloc2 = Allocation.createFromBitmap(rs, fullBmp2);
        //Allocation outAlloc = Allocation.createFromBitmap(rs, crossBitmap);

        // Set variables for RenderScript
        //crossScript.set_lWeight(cross);
        //crossScript.set_rWeight(cross2);
        //crossScript.set_right_image(inAlloc2);

        // Iterate pixel by pixel
        //crossScript.forEach_root(inAlloc1, outAlloc);
        // Point crossBitmap to the allocated output Bitmap
        //outAlloc.copyTo(crossBitmap);

        // Add finished bitmap to output array
        crossComplete[frameID] = crossBitmap;
    }

    private String exportImageToFile(String fn, Bitmap bmp) {
        File f = new File(Environment.getExternalStorageDirectory() + APP_DIRECTORY
                + File.separator + fn + ".png");
        try {
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fo);
            fo.close();
        } catch (IOException e) {
            System.err.println("Couldn't create file: " + f.getAbsolutePath());
        }
        return f.getAbsolutePath();
    }

    private void openFrameDisplay() {
        Intent intent = new Intent(this, MorphDisplay.class);
        intent.putExtra(PHOTO_PATH, filePaths);
        startActivity(intent);
    }

    /**
     * Posts an asynchronous update to the progress bar.
     */
    private void updateProgress() {
        progressBar.post(new Runnable() {
            public void run() {
                progressBar.setProgress(progress);
            }
        });
    }

    /**
     * Increments progress variable then asynchronously updates the progress bar.
     */
    private void incrementProgress() {
        progress++;
        updateProgress();
    }

    /**
     * Gets the absolute path to an image from its URI
     *
     * @param source URI to get path of
     * @return Absolute path to the file
     */
    private String getRealPath(Uri source) {
        String result;
        Cursor cursor = getContentResolver().query(source, null, null, null, null);
        if (cursor == null) {
            result = source.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    /**
     * Override to handle External Storage permissions request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickPhoto, GALLERY_IMG_REQUEST);
                }
                return;
            }
        }
    }

    /**
     * Override to handle the returning URI from Gallery Images
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CAMERA_IMG_REQUEST:
            case GALLERY_IMG_REQUEST:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = data.getData();

                    selectedView.setImageBitmap(BitmapFactory.decodeFile(getRealPath(selectedImage)));
                    selectedView.setBackgroundResource(0);
                    selectedView.setOnTouchListener(lineTouchListener);
                }
                break;
        }
    }
}
