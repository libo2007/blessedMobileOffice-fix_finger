package com.jiaying.workstation.activity.sensor;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jiaying.workstation.R;
import com.jiaying.workstation.activity.BaseActivity;
import com.jiaying.workstation.activity.physicalexamination.PhysicalExamActivity;
import com.jiaying.workstation.activity.physicalexamination.PhysicalExamResultActivity;
import com.jiaying.workstation.activity.plasmacollection.SelectPlasmaMachineActivity;
import com.jiaying.workstation.activity.plasmacollection.SelectPlasmaMachineResultActivity;
import com.jiaying.workstation.constant.IntentExtra;
import com.jiaying.workstation.constant.TypeConstant;
import com.jiaying.workstation.engine.LdFingerprintReader;
import com.jiaying.workstation.engine.ProxyFingerprintReader;
import com.jiaying.workstation.interfaces.IfingerprintReader;
import com.jiaying.workstation.utils.CountDownTimerUtil;
import com.jiaying.workstation.utils.SetTopView;
import com.jiaying.workstation.utils.ZA_finger;
import com.za.android060;

import java.io.DataOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

/**
 * 基于龙盾sdk原始的demo
 */
public class FingerprintOriginalActivity extends BaseActivity {
    private static final String TAG = "FingerprintOriginal";
    private Handler mHandler = new Handler();
    private Runnable mRunnable = null;



    private TextView result_txt;
    private TextView state_txt;
    private ImageView photo_image;

    private TextView nameTextView = null;
    private TextView idCardNoTextView = null;
    private ImageView avaterImageView = null;
    private String donorName = null;
    private Bitmap avatarBitmap = null;
    private String idCardNO = null;
    private int source;

    private CountDownTimerUtil countDownTimerUtil;



    private boolean fpflag=false;
    private boolean fpcharflag = false;
    private boolean fpmatchflag = false;
    private boolean fperoll = false;
    private boolean fpsearch = false;
    private boolean isfpon  = false;

    private int testcount = 0;

    private int fpcharbuf = 0;
    long ssart = System.currentTimeMillis();
    long ssend = System.currentTimeMillis();
    private Handler objHandler_fp;
    private HandlerThread thread;

    private int IMG_SIZE = 0;//同参数：（0:256x288 1:256x360）



    android060 a6 = new android060();
    int DEV_ADDR = 0xffffffff;
    private Handler objHandler_3 ;
    String sdCardRoot = Environment.getExternalStorageDirectory()
            .getAbsolutePath();


    private int def_iCom=13;
    private int def_iBaud=6;
    private int usborcomtype=1 ;///0 noroot  1root
    private int defDeviceType;
    private int defiCom;
    private int defiBaud;

    private int iPageID = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public void initVariables() {

        Intent donorInfoIntent = getIntent();
        source = donorInfoIntent.getIntExtra("source", 0);
        switch (source) {
            case TypeConstant.TYPE_LOG:
                break;
            case TypeConstant.TYPE_REG:
                donorName = donorInfoIntent.getStringExtra("donorName");
                Bitmap tempBitmap = donorInfoIntent.getParcelableExtra("avatar");
                Matrix matrix = new Matrix();
                matrix.postScale(1.0f, 1.0f);
                avatarBitmap = Bitmap.createBitmap(tempBitmap, 0, 0, tempBitmap.getWidth(),
                        tempBitmap.getHeight(), matrix, true);
                idCardNO = donorInfoIntent.getStringExtra("idCardNO");
                break;
            case TypeConstant.TYPE_SELECT_MACHINE:

                break;

        }



        //指纹打开

        usborcomtype = 1;
        defDeviceType = 2;
        defiCom = 3;
        defiBaud = 12;
        thread = new HandlerThread("MyHandlerThread");
        thread.start();
        objHandler_fp = new Handler();//

        char[] pPassword = new char[4];
        ZA_finger fppower = new ZA_finger();
        fppower.finger_power_on();
        fppower.card_power_on();

        try {
            thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        int status = 0;
        if(1==usborcomtype){
            LongDunD8800_CheckEuq();
            //status = a6.ZAZOpenDeviceEx(-1, 5, 3, 12, 0, 0);
            status = a6.ZAZOpenDeviceEx(-1, defDeviceType, defiCom, defiBaud, 0, 0);
            if(status == 1 && a6.ZAZVfyPwd(DEV_ADDR, pPassword) == 0){
                status =1;
            }
            else {
                status = 0;
            }

            a6.ZAZSetImageSize(IMG_SIZE);
        }else {
            int fd=getrwusbdevices();
            Log.e(TAG, "zhw === open fd: " + fd);

            //try {
            //	Thread.sleep(1000);
            //} catch (InterruptedException e) {
            // TODO Auto-generated catch block
            //	e.printStackTrace();
            //}
            status = a6.ZAZOpenDeviceEx(fd, defDeviceType, defiCom, defiBaud, 0, 0);
            //status = a6.ZAZOpenDeviceEx(fd, defDeviceType, defiCom, defiBaud, 0, 0);
            if(status == 1 && a6.ZAZVfyPwd(DEV_ADDR, pPassword) == 0){
                status =1;
            }
            else {
                status = 0;
            }
            a6.ZAZSetImageSize(IMG_SIZE);
            //a6.ZAZCloseDeviceEx();

            //fd=getrwusbdevices();
            //Log.e(TAG, "zhw === open fd: " + fd);
            //status = a6.ZAZOpenDeviceEx(fd, 5, 3, 12, 0, 0);


        }
        Log.e(TAG, " open status: " + status);
        //offLine(true);

        if(status==1){
            Toast.makeText(FingerprintOriginalActivity.this, "打开设备成功",
                    Toast.LENGTH_SHORT).show();
            String temp ="打开设备成功";
            readFinger();
        }else{
            Toast.makeText(FingerprintOriginalActivity.this, "打开设备失败",
                    Toast.LENGTH_SHORT).show();
            String temp ="打开设备失败";
            int close_status = a6.ZAZCloseDeviceEx();
            Log.e(TAG, "close_status" + status);
            finish();
        }



    }
    //开始读取指纹
    private void readFinger(){

        setflag(true);
        try {
            thread.sleep(500);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        fpflag = false;
        objHandler_fp.removeCallbacks(fpTasks);

        readsfpimg();
    }
    public void readsfpimg()
    {
        ssart = System.currentTimeMillis();
        ssend = System.currentTimeMillis();
        testcount = 0;
        objHandler_fp.postDelayed(fpTasks, 0);
    }
    private Runnable fpTasks = new Runnable() {
        public void run()// 运行该服务执行此函数
        {
            String temp="";
            long timecount=0;
            ssend = System.currentTimeMillis();
            timecount = (ssend - ssart);

            if (timecount >10000)
            {
                temp ="读指纹等待超时"+"\r\n";
                state_txt.setText(temp);
                return;
            }
            if(fpflag){
                temp ="获取图像主动停止"+"\r\n";
                state_txt.setText(temp);
                return;
            }
            int nRet = 0;
            nRet = a6.ZAZGetImage(DEV_ADDR);
            if(nRet  == 0)
            {
                testcount = 0;
                int[] len = { 0, 0 };
                char[] Image = new char[256 * 360];
                a6.ZAZUpImage(DEV_ADDR, Image, len);
                String str = "/mnt/sdcard/test.bmp";
                a6.ZAZImgData2BMP(Image, str);
                temp ="获取图像成功";
                state_txt.setText(temp);

                Bitmap bmpDefaultPic;
                bmpDefaultPic = BitmapFactory.decodeFile(str,null);
                photo_image.setImageBitmap(bmpDefaultPic);


                //认证通过后跳到
            mRunnable = new runnable();
            mHandler.postDelayed(mRunnable, 0);
            }
            else if(nRet==a6.PS_NO_FINGER){
                temp = "正在读取指纹中   剩余时间:"+((10000-(ssend - ssart)))/1000 +"s";
                state_txt.setText(temp);
                objHandler_fp.postDelayed(fpTasks, 100);
            }
            else if(nRet==a6.PS_GET_IMG_ERR){
                temp ="图像获取中";
                Log.d(TAG, temp+"2: "+nRet);
                objHandler_fp.postDelayed(fpTasks, 100);
                state_txt.setText(temp);
                return;
            }else if(nRet == -2)
            {
                testcount ++;
                if(testcount <3){
                    temp = "正在读取指纹中   剩余时间:"+((10000-(ssend - ssart)))/1000 +"s";
                    isfpon = false;
                    state_txt.setText(temp);
                    objHandler_fp.postDelayed(fpmatchTasks, 10);
                }
                else{
                    temp ="通讯异常";
                    Log.d(TAG, temp+": "+nRet);
                    state_txt.setText(temp);

                    return;
                }
            }
            else
            {
                temp ="通讯异常";
                Log.d(TAG, temp+"2: "+nRet);
                state_txt.setText(temp);
                return;
            }

        }
    };

    private void setflag(boolean value)
    {
        fpflag = value;
        fpcharflag = value;
        fpmatchflag= value;
        fperoll = value;
        fpsearch = value;


    }


    @Override
    public void initView() {
        setContentView(R.layout.activity_fingerprint);
        new SetTopView(this, R.string.title_activity_fingerprint, true);
        if (source == TypeConstant.TYPE_SELECT_MACHINE) {
            new SetTopView(this, R.string.read_worker_fp, true);
        }


        result_txt = (TextView) findViewById(R.id.result_txt);
        state_txt = (TextView) findViewById(R.id.state_txt);
        photo_image = (ImageView) findViewById(R.id.photo_image);
        //开始倒计时
//        countDownTimerUtil = CountDownTimerUtil.getInstance(result_txt, this);
//        countDownTimerUtil.start();


        switch (source) {

            case TypeConstant.TYPE_LOG:
                break;
            case TypeConstant.TYPE_REG:
                nameTextView = (TextView) this.findViewById(R.id.name_txt);
                nameTextView.setText(donorName);
                avaterImageView = (ImageView) this.findViewById(R.id.head_image);
                avaterImageView.setImageBitmap(avatarBitmap);
                idCardNoTextView = (TextView) this.findViewById(R.id.id_txt);
                idCardNoTextView.setText(idCardNO);
                break;
        }
    }

    @Override
    public void loadData() {

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

//    @Override
//    public void onFingerPrintInfo(final Bitmap bitmap) {
//        //指纹识别结果
//        if (bitmap != null) {
////            countDownTimerUtil.cancel();
//            FingerprintOriginalActivity.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    countDownTimerUtil.cancel();
//                    photo_image.setImageBitmap(convert(bitmap));
//                }
//            });
//
//
//            //认证通过后跳到
//            mRunnable = new runnable();
//            mHandler.postDelayed(mRunnable, 1000);
//        } else {
//            FingerprintOriginalActivity.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(FingerprintOriginalActivity.this, "指纹设备异常",
//                            Toast.LENGTH_SHORT).show();
//                }
//            });
//        }
//    }

//    @Override
//    public void onFingerPrintOpenInfo(int status) {
//        showOpenResult(status);
//        if (1 != status) {
//            proxyFingerprintReader.close();
//            this.finish();
//        } else {
//            proxyFingerprintReader.read();
//        }
//    }

    private Bitmap convert(Bitmap a) {

        int w = a.getWidth();
        int h = a.getHeight();
        Bitmap newb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);// 创建一个新的和SRC长度宽度一样的位图
        Canvas cv = new Canvas(newb);
        Matrix m = new Matrix();
//        m.postScale(1, -1);   //镜像垂直翻转
        m.postScale(-1, 1);   //镜像水平翻转
//        m.postRotate(-90);  //旋转-90度
        Bitmap new2 = Bitmap.createBitmap(a, 0, 0, w, h, m, true);
        cv.drawBitmap(new2, new Rect(0, 0, new2.getWidth(), new2.getHeight()), new Rect(0, 0, w, h), null);

//        return newb;
//        Bitmap roundBitmap = BitmapUtils.makeRoundCorner(newb);
//        MyLog.e("ERROR",roundBitmap.getWidth() + ",height:" + roundBitmap.getHeight());
        return newb;

    }



    private class runnable implements Runnable {
        @Override
        public void run() {
            Intent it = null;
            int type = getIntent().getIntExtra("source", 0);
            if (type == TypeConstant.TYPE_REG) {
                //登记的话就到采集人脸
//                it = new Intent(FingerprintActivity.this, FaceCollectionActivity.class);
                it = new Intent(FingerprintOriginalActivity.this, FaceCollectionActivity.class);
            } else if (type == TypeConstant.TYPE_BLOODPLASMACOLLECTION) {
                //献浆的，去选择浆机
                it = new Intent(FingerprintOriginalActivity.this, SelectPlasmaMachineActivity.class);
            } else if (type == TypeConstant.TYPE_PHYSICAL_EXAM) {
                //体检，去体检
                it = new Intent(FingerprintOriginalActivity.this, PhysicalExamActivity.class);
            } else if (type == TypeConstant.TYPE_PHYSICAL_EXAM_SUBMIT_XJ) {
                //体检完成后提交体检，献浆员打指纹-》医生打指纹

                new SetTopView(FingerprintOriginalActivity.this, R.string.title_activity_fingerprint_xj, false);
                it = new Intent(FingerprintOriginalActivity.this, FingerprintOriginalActivity.class);
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                it.putExtra(IntentExtra.EXTRA_TYPE, TypeConstant.TYPE_PHYSICAL_EXAM_SUBMIT_DOC);
            } else if (type == TypeConstant.TYPE_PHYSICAL_EXAM_SUBMIT_DOC) {
                //体检完成后提交体检，医生打指纹后，显示体检结果
                new SetTopView(FingerprintOriginalActivity.this, R.string.title_activity_fingerprint_doc, false);
                it = new Intent(FingerprintOriginalActivity.this, PhysicalExamResultActivity.class);
            } else if (type == TypeConstant.TYPE_SELECT_MACHINE) {
                it = new Intent(FingerprintOriginalActivity.this, SelectPlasmaMachineResultActivity.class);
                it.putExtra(IntentExtra.EXTRA_PLASMAMACHINE, getIntent().getSerializableExtra(IntentExtra.EXTRA_PLASMAMACHINE));
            } else if (type == TypeConstant.TYPE_SELECT_MACHINE) {
                it = new Intent(FingerprintOriginalActivity.this, SelectPlasmaMachineResultActivity.class);
                it.putExtra(IntentExtra.EXTRA_PLASMAMACHINE, getIntent().getExtras());
                startActivity(it);
            } else {
                //其他的情况
                it = new Intent(FingerprintOriginalActivity.this, FingerprintOriginalActivity.class);
            }
            if (it != null) {
                startActivity(it);
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();


    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    public int LongDunD8800_CheckEuq()
    {
        Process process = null;
        DataOutputStream os = null;

        // for (int i = 0; i < 10; i++)
        // {
        String path = "/dev/bus/usb/00*/*";
        String path1 = "/dev/bus/usb/00*/*";
        File fpath = new File(path);
        Log.d("*** LongDun D8800 ***", " check path:" + path);
        // if (fpath.exists())
        // {
        String command = "chmod 777 " + path;
        String command1 = "chmod 777 " + path1;
        Log.d("*** LongDun D8800 ***", " exec command:" + command);
        try
        {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command+"\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            return 1;
        }
        catch (Exception e)
        {
            Log.d("*** DEBUG ***", "Unexpected error - Here is what I know: "+e.getMessage());
        }
        //  }
        //  }
        return 0;
    }
    public int getrwusbdevices() {

        // get FileDescriptor by Android USB Host API
        UsbManager mUsbManager = (UsbManager) this
                .getSystemService(Context.USB_SERVICE);

        final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        BroadcastReceiver mUsbReceiver = null;
        this.registerReceiver(mUsbReceiver, filter);
        Log.i(TAG,"zhw 060");
        int fd = -1;
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            Log.i(TAG,
                    device.getDeviceName() + " "
                            + Integer.toHexString(device.getVendorId()) + " "
                            + Integer.toHexString(device.getProductId()));

            if ((device.getVendorId() == 0x2109)
                    && (0x7638 == device.getProductId())) {
                Log.d(TAG, " get FileDescriptor ");
                mUsbManager.requestPermission(device, mPermissionIntent);
                while(!mUsbManager.hasPermission(device)){

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                if (mUsbManager.hasPermission(device)) {
                    if (mUsbManager
                            .openDevice(device) != null) {
                        fd = mUsbManager
                                .openDevice(device).getFileDescriptor();
                        Log.d(TAG, " get FileDescriptor fd " + fd);
                        return fd;
                    } else
                        Log.e(TAG, "UsbManager openDevice failed");

                    mUsbManager
                            .openDevice(device).close();
                }
                break;
            }

        }

        return 0;
    }
    private Runnable fpmatchTasks = new Runnable() {
        public void run()// 运行该服务执行此函数
        {
            String temp="";
            long timecount=0;
            ssend = System.currentTimeMillis();
            timecount = (ssend - ssart);

            if (timecount >10000)
            {
                temp ="读指纹等待超时"+"\r\n";
                state_txt.setText(temp);
                return;
            }
            if(fpmatchflag){
                temp ="比对主动停止"+"\r\n";
                state_txt.setText(temp);
                return;
            }
            int nRet = 0;
            nRet = a6.ZAZGetImage(DEV_ADDR);
            if(nRet ==0){
                testcount = 0;
                try {
                    thread.sleep(200);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                nRet = a6.ZAZGetImage(DEV_ADDR);
            }
            if(nRet  == 0)
            {
                if(isfpon){
                    temp ="请拿起手指";
                    state_txt.setText(temp);
                    ssart = System.currentTimeMillis();
                    objHandler_fp.postDelayed(fpmatchTasks, 100);
                    return;
                }
//		 		{
//			 		int[] len = { 0, 0 };
//					char[] Image = new char[256 * 360];
//					a6.ZAZUpImage(DEV_ADDR, Image, len);
//					String str = "/mnt/sdcard/test.bmp";
//					a6.ZAZImgData2BMP(Image, str);
//					temp ="获取图像成功";
//			 		state_txt.setText(temp);
//
//					Bitmap bmpDefaultPic;
//					bmpDefaultPic = BitmapFactory.decodeFile(str,null);
//					mFingerprintIv.setImageBitmap(bmpDefaultPic);
//		 		}
                nRet= a6.ZAZGenChar(DEV_ADDR, fpcharbuf);// != PS_OK) {
                if(nRet ==a6.PS_OK  )
                {
                    if(fpcharbuf!=0)
                    {	int[] iScore = { 0, 0 };
                        nRet= a6.ZAZMatch(DEV_ADDR,  iScore);
                        if(nRet ==a6.PS_OK  )
                        {
                            temp ="比对成功   得分:"+iScore[0];
                            state_txt.setText(temp);
                        }
                        else
                        {
                            temp ="比对失败   得分:"+iScore[0];
                            state_txt.setText(temp);
                        }
                        return;
                    }
                    fpcharbuf=1;
                    isfpon = true;
                    temp ="请按指纹需要比对的指纹";
                    state_txt.setText(temp);
                    ssart = System.currentTimeMillis();
                    objHandler_fp.postDelayed(fpmatchTasks, 100);
                }
                else
                {	temp ="特征太差，请重新录入";
                    state_txt.setText(temp);
                    ssart = System.currentTimeMillis();
                    objHandler_fp.postDelayed(fpmatchTasks, 1000);

                }

            }
            else if(nRet==a6.PS_NO_FINGER){
                temp = "正在读取指纹中   剩余时间:"+((10000-(ssend - ssart)))/1000 +"s";
                isfpon = false;
                state_txt.setText(temp);
                objHandler_fp.postDelayed(fpmatchTasks, 10);
            }else if(nRet==a6.PS_GET_IMG_ERR){
                temp ="图像获取中";
                Log.d(TAG, temp+": "+nRet);
                state_txt.setText(temp);
                objHandler_fp.postDelayed(fpmatchTasks, 10);
                //state_txt.setText(temp);
                return;
            }else if(nRet == -2)
            {
                testcount ++;
                if(testcount <3){
                    temp = "正在读取指纹中   剩余时间:"+((10000-(ssend - ssart)))/1000 +"s";
                    isfpon = false;
                    state_txt.setText(temp);
                    objHandler_fp.postDelayed(fpmatchTasks, 10);
                }
                else{
                    temp ="通讯异常";
                    Log.d(TAG, temp+": "+nRet);
                    state_txt.setText(temp);

                    return;
                }
            }

            else
            {
                temp ="通讯异常";
                Log.d(TAG, temp+": "+nRet);
                state_txt.setText(temp);

                return;
            }
        }
    };

}

