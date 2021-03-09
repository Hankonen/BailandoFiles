package com.hankonen.BailandoFiles;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.hierynomus.smbj.auth.AuthenticationContext;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ch.swaechter.smbjwrapper.SmbConnection;
import ch.swaechter.smbjwrapper.SmbDirectory;
import ch.swaechter.smbjwrapper.SmbFile;

//import org.apache.commons.io.IOUtils;

public class MainActivity extends AppCompatActivity {

    private boolean isFileManagerInitialized = false;

    private boolean[] selection;

    private File[] files;

    private List<String> filesList;

    private File dir;

    private TextView pathOutput;

    private String currentPath;
    private String newPath;

    private int selectedIndex;

    private String copySrcPath;

    private String pathSmb;
    private String copiedFileName;
    private String smbServerName;
    private String smbUsername;
    private String smbPasswd;
    private String smbDomain;
    private String smbShareName;
    private List<String> filesListSmb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout1);
    }



//SMBJWRAPPER CLASS
    class SmbClass {

        private AuthenticationContext authenticationContext = new AuthenticationContext(smbUsername, smbPasswd.toCharArray(), smbDomain);
        private File src;
        public void startSmbThread(){
            smbjThread.start();
        }

        public void pasteToSmbShare(File src){
            this.src = src;
            smbjPasteThread.start();
        }

        final Thread smbjPasteThread = new Thread() {
            @Override
            public void run() {
                try (SmbConnection smbConnection = new SmbConnection(smbServerName, smbShareName, authenticationContext)) {
                    SmbFile smbFile = new SmbFile(smbConnection, pathSmb + "/" + copiedFileName);
                    InputStream inputStream = new FileInputStream(src);

                    OutputStream outputStream = smbFile.getOutputStream();

                    IOUtils.copy(inputStream, outputStream);
                    inputStream.close();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        final Thread smbjThread = new Thread(){
            @Override
            public void run() {
                final ListView listView2 = findViewById(R.id.listView2);
                final TextAdapter textAdapter2 = new TextAdapter();
                final TextView smbPathOutPut = findViewById(R.id.pathOutput2);
                filesListSmb = new ArrayList<>();

                try (SmbConnection smbConnection = new SmbConnection(smbServerName, smbShareName, authenticationContext)) {
                    Log.d("polku", "lolkonnekted");
                    SmbDirectory rootDirectory = new SmbDirectory(smbConnection);
                    if(pathSmb != null) {
                        rootDirectory = new SmbDirectory(smbConnection, pathSmb);
                    }
                    for (SmbDirectory smbDirectory : rootDirectory.getDirectories()) {
                        Log.d("polku", smbDirectory.getPath());
                        filesListSmb.add(smbDirectory.getPath());
                    }
                    for (SmbFile smbFile : rootDirectory.getFiles()) {
                        Log.d("polku", smbFile.getName());
                        Log.d("polku", smbFile.getPath());
                        filesListSmb.add(smbFile.getPath());
                    }

                    //LISTVIEW2 AND STUFF TO PUT INSIDE IT
                    runOnUiThread(() -> {
                        listView2.setAdapter(textAdapter2);
                        textAdapter2.setData(filesListSmb);
                        smbPathOutPut.setText("smb::/" + pathSmb);
                    });

                    /*UPDATE PATH FOR REFRESH BUTTON TO LOAD NEW DIRECTORY. THIS SHOULD BE DONE DIFFERENTLY*/
                    listView2.setOnItemClickListener((parent, view, position, id) -> {
                        pathSmb = textAdapter2.getItem(position);
                        Log.d("polku", "pathSmbklikattuna: " + pathSmb);
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    /*MARSHMALLOW OR ABOVE NEEDS EXTERNAL STORAGE PERMISSIONS TO BE ASKED ON RUNTIME*/
    private static final int REQUEST_PERMISSIONS = 1234;

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET
    };

    private boolean permissionsDenied(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){       /*TÄSSÄKIN VANHA PERMISSION*/
            int p = 0;
            while (p<PERMISSIONS.length){
                if(checkSelfPermission(PERMISSIONS[p]) != PackageManager.PERMISSION_GRANTED){
                    return true;
                }
                p++;
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {

        super.onResume();

        /*ASKING FOR PERMISSIONS*/
        if(permissionsDenied()){
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }

        if(!isFileManagerInitialized) {
            final Button pasteBtn = findViewById(R.id.pasteBtn);
            final ListView listView1 = findViewById(R.id.listView1);
            final TextAdapter textAdapter1 = new TextAdapter();
            final Button refreshBtn = findViewById(R.id.refreshBtn);
            final Button cdBackBtn = findViewById(R.id.cdBckBtn);
            final Button smbLoadBtn = findViewById(R.id.loadSmbShareBtn);
            final Button refreshBtnSmb = findViewById(R.id.refreshBtnSmb);
            final Button backBtnSmb = findViewById(R.id.backBtnSmb);
            final Button pasteBtnSmb = findViewById(R.id.pasteBtnSmb);
            final Button deleteBtnPhone = findViewById(R.id.deleteBtn);
            final Button renameBtn = findViewById(R.id.btn2);
            final Button copyBtn = findViewById(R.id.copyBtn);
            final Button createNewFolder = findViewById(R.id.newFolderBtn);
            refreshBtnSmb.setEnabled(true);
            backBtnSmb.setEnabled(true);
            final String rootPath = "/storage/self/primary/Download";//String.valueOf(Environment.getExternalStorageDirectory()) + "/Download/";
            Log.d("polku", rootPath);
            final String jotain = Environment.getExternalStorageState();
            Log.d("polku", jotain);
            for (String permission : PERMISSIONS) {
                Log.d("polku", permission);
            }
            currentPath = rootPath;
            dir = new File(rootPath);
            files = dir.listFiles();
            pathOutput = findViewById(R.id.pathOutput);
            pathOutput.setText("/" + rootPath.substring(rootPath.lastIndexOf('/') + 1));


            listView1.setAdapter(textAdapter1);

            filesList = new ArrayList<>();

            /*REFRESHBUTTON*/
            //This is called all the time to reload the upper ListView with phones current directory's list of files
            refreshBtn.setOnClickListener(v -> {
                files = dir.listFiles();
                filesList.clear();
                if(files != null){
                    for (File file : files) {
                        filesList.add(file.getName());
                    }
                }
                if (files == null) throw new AssertionError();
                selection = new boolean[files.length];
                textAdapter1.setSelection(selection);
                textAdapter1.setData(filesList);
            });

            //This loads the above mentioned ListView with stuff on first load
            if(files != null){
                for (File file : files) {
                    filesList.add(file.getName());
                }
            }
            textAdapter1.setData(filesList);
            if(files != null){
                selection = new boolean[files.length];
            }

            /*GO BACK ONE DIRECTORY - BUTTON*/
            cdBackBtn.setOnClickListener(v -> {
                newPath = currentPath.substring(0,currentPath.lastIndexOf('/'));
                dir = new File(newPath);
                

                String[] splitted = newPath.split("/");
                Log.d("polku", "backnappi splitted length ennen while looppia: " + splitted.length);
                while(splitted.length > 2 && !dir.canRead()){
                    newPath = newPath.substring(0,newPath.lastIndexOf('/'));
                    splitted = newPath.split("/");
                    dir = new File(newPath);
                    Log.d("polku", "backnappi: " + newPath);
                    Log.d("polku", "backnappi splitted length loopissa: " + splitted.length);
                }
                if(dir.canRead()){
                    currentPath = newPath;
                    Log.d("polku", "dircanread TRUE: " + currentPath);
                }

                dir = new File(currentPath);
                Log.d("polku", "backnappi dir: " + dir.canRead());
                pathOutput.setText("/" + currentPath.substring(currentPath.lastIndexOf('/') + 1));
                refreshBtn.callOnClick();
                selection = new boolean[files.length];
                textAdapter1.setSelection(selection);
            });

            /*OPENING DIRECTORIES BY CLICKING THEM ON THE ListView*/
            listView1.setOnItemClickListener((parent, view, position, id) -> {
                newPath = files[position].getAbsolutePath();
                Log.d("polku", "ETEENPÄIN: " + newPath);
                if(newPath.equals("/storage/self")){
                    newPath = "/storage/self/primary";
                }

                dir = new File(newPath);
                if(dir.isDirectory() && dir.canRead()){
                    currentPath = newPath;
                    pathOutput.setText("/" + currentPath.substring(currentPath.lastIndexOf('/') + 1));
                    refreshBtn.callOnClick();
                    selection = new boolean[files.length];
                    textAdapter1.setSelection(selection);
                }

            });

            //SAMBA LOADLOGIN INFOINPUT BUTTON
            smbLoadBtn.setOnClickListener(v -> {
                final AlertDialog.Builder renameDialog = new AlertDialog.Builder(MainActivity.this);
                renameDialog.setTitle("Fill SMB Share information");
                Context context = renameDialog.getContext();
                final LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.VERTICAL);
                final EditText smbServerNameET = new EditText(context);
                smbServerNameET.setHint("Servername eg. '10.0.0.0'");
                layout.addView(smbServerNameET);
                final EditText smbUserNameET = new EditText(context);
                smbUserNameET.setHint("Username");
                layout.addView(smbUserNameET);
                final EditText smbPasswdET = new EditText(context);
                smbPasswdET.setHint("Password");
                layout.addView(smbPasswdET);
                smbPasswdET.setTransformationMethod(PasswordTransformationMethod.getInstance());
                final EditText smbDomainET = new EditText(context);
                smbDomainET.setHint("Domain name. Null if left empty");
                layout.addView(smbDomainET);
                final EditText smbShareNameET = new EditText(context);
                smbShareNameET.setHint("Share");
                layout.addView(smbShareNameET);

                smbServerNameET.setInputType(InputType.TYPE_CLASS_TEXT);
                renameDialog.setView(layout);
                renameDialog.setPositiveButton("Ok", (dialog, which) -> {
                    smbServerName = smbServerNameET.getText().toString();
                    smbDomain = smbDomainET.getText().toString();
                    smbPasswd = smbPasswdET.getText().toString();
                    smbUsername = smbUserNameET.getText().toString();
                    smbShareName = smbShareNameET.getText().toString();
                    SmbClass newSmbClass = new SmbClass();
                    newSmbClass.startSmbThread();
                });
                renameDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                renameDialog.show();
            });

            //SAMBA DIRECTORY CHANGE BY RECONNECTING AND LOADING THINGS WITH NEW PATH
            refreshBtnSmb.setOnClickListener(v -> {
                SmbClass newSmbClass = new SmbClass();
                newSmbClass.startSmbThread();
            });

            //SAMBA BACK BUTTON
            backBtnSmb.setOnClickListener(v -> {
                pathSmb = null;
                SmbClass newSmbClass = new SmbClass();
                newSmbClass.startSmbThread();
            });

            //SAMBA PASTE BUTTON
            pasteBtnSmb.setOnClickListener(v -> {
                pasteBtnSmb.setEnabled(false);
                SmbClass newSmbClass = new SmbClass();
                newSmbClass.pasteToSmbShare(new File(copySrcPath));
            });

            /*SELECTING FILE BY LONGCLICKING IT ON ListView*/
            listView1.setOnItemLongClickListener((parent, view, position, id) -> {

                selection[position] = !selection[position];
                textAdapter1.setSelection(selection);
                int counter = 0;
                for (boolean Selection : selection) {
                    if(Selection){
                        counter++;
                    }
                }
                if (counter > 0) {
                    findViewById(R.id.deleteBtn).setEnabled(true);
                } else {
                    findViewById(R.id.deleteBtn).setEnabled(false);
                }
                if(counter == 1){
                    selectedIndex = position;
                    findViewById(R.id.btn2).setEnabled(true);
                    findViewById(R.id.copyBtn).setEnabled(true);
                }else if(counter != 1){
                    findViewById(R.id.btn2).setEnabled(false);
                }
                return true;
            });

            /*DELETE FILE FROM PHONES MEMORY - BUTTON*/
            deleteBtnPhone.setOnClickListener(v -> {
                final AlertDialog.Builder copyDialog = new AlertDialog.Builder(MainActivity.this);
                deleteBtnPhone.setEnabled(false);
                copyDialog.setTitle("Delete selected");
                copyDialog.setMessage("Are you sure?");
                copyDialog.setPositiveButton("Yes", (dialog, which) -> {
                    for (int i = 0; i < files.length; i++) {
                        if (selection[i]) {
                            deleteFileOrFolder(files[i]);
                            selection[i] = false;
                        }
                    }
                    selection = new boolean[files.length];
                    textAdapter1.setSelection(selection);
                    refreshBtn.callOnClick();
                });
                copyDialog.setNegativeButton("No", (dialog, which) -> dialog.cancel());
                copyDialog.show();
            });

            /*RENAME BUTTON METHODS ETC.*/
            renameBtn.setOnClickListener(v -> {
                final AlertDialog.Builder renameDialog = new AlertDialog.Builder(MainActivity.this);
                renameDialog.setTitle("Rename file or directory");
                EditText input = new EditText(MainActivity.this);
                input.setText(files[selectedIndex].getName());
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                renameDialog.setView(input);
                renameDialog.setPositiveButton("Ok", (dialog, which) -> {
                    final File newFile = new File(currentPath+"/"+input.getText().toString());
                    if(!newFile.exists()){
                        files[selectedIndex].renameTo(newFile);
                        refreshBtn.callOnClick();
                        selection = new boolean[files.length];
                        textAdapter1.setSelection(selection);
                    }
                });
                renameDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                renameDialog.show();
            });

            /*COPY BUTTON METHODS ETC. THESE FILES ARE ALSO USED TO COPY THINGS OVER SAMBA*/
            copyBtn.setOnClickListener(v -> {
                copySrcPath = files[selectedIndex].getAbsolutePath();
                pasteBtnSmb.setEnabled(true);
                pasteBtn.setEnabled(true);

                copiedFileName = files[selectedIndex].getName();
                Log.d("testin", copySrcPath);
                selection = new boolean[files.length];
                textAdapter1.setSelection(selection);
                findViewById(R.id.pasteBtn).setEnabled(true);
                refreshBtn.callOnClick();
            });

            /*PASTE BUTTON FOR PASTING FILES FROM PHONES MEMORY TO PHONES MEMORY ONLY.  NOT TO SAMBA SHARE*/
            pasteBtn.setOnClickListener(v -> {
                pasteBtn.setEnabled(false);
                String pasteDstPath = currentPath + copySrcPath.substring(copySrcPath.lastIndexOf('/'));
                Log.d("testin", pasteDstPath);

                try {
                    InputStream in = new FileInputStream(new File(copySrcPath)); // Your input stream
                    OutputStream out = new FileOutputStream(new File(pasteDstPath)); //Outputstream
                    IOUtils.copy(in, out);
                    in.close();
                    out.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //selection = null;
                refreshBtn.callOnClick();
            });

            /*NEW FOLDER TO PHONES MEMORY - BUTTON*/
            createNewFolder.setOnClickListener(v -> {
                final AlertDialog.Builder newFolderDialog = new AlertDialog.Builder(MainActivity.this);
                newFolderDialog.setTitle("Create new directory");
                EditText input = new EditText(MainActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                newFolderDialog.setView(input);
                newFolderDialog.setPositiveButton("Ok", (dialog, which) -> {
                    final File newFolder = new File(currentPath+"/"+input.getText().toString());
                    if(!newFolder.exists()){
                        newFolder.mkdir();
                        refreshBtn.callOnClick();
                        selection = new boolean[files.length];
                        textAdapter1.setSelection(selection);
                    }
                });
                newFolderDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                newFolderDialog.show();
            });

            isFileManagerInitialized = true;
        }
    }


/*DELETING FILES AND FOLDERS AND STUFF IN FOLDERS METHOD*/
    private void deleteFileOrFolder(File fileOrFolder){
        if(fileOrFolder.isDirectory()){
            if(Objects.requireNonNull(fileOrFolder.list()).length==0){
                fileOrFolder.delete();
            }else{
                String files[] = fileOrFolder.list();
                for(String temp:files){
                    File fileToDelete = new File(fileOrFolder, temp);
                    deleteFileOrFolder(fileToDelete);
                }
                if(Objects.requireNonNull(fileOrFolder.list()).length==0) {
                    fileOrFolder.delete();
                }
            }
        }else if(fileOrFolder.isFile()){
            fileOrFolder.delete();
        }
    }

    /*This fill "override" the "Dont ask again" checker in the Permission asking box thing*/
    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==REQUEST_PERMISSIONS && grantResults.length>0){
            if(permissionsDenied()){
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            }else {
                onResume();
            }
        }
    }
}