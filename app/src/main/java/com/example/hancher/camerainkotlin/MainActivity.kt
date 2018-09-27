package com.example.hancher.camerainkotlin

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.hardware.Camera
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.ImageView
import java.io.File
import java.io.FileOutputStream

/**
 * 用Kotlin练习编写的一个简单的相机app
 */
@SuppressWarnings("deprecation")
class MainActivity : AppCompatActivity(), SurfaceHolder.Callback, View.OnClickListener {

    /**
     * 伴生对象，一般作为静态使用
     */
    companion object {
        val TAG = "CameraTest"
        val REQUEST_PERMISSION_CODE = 0
    }

    /**
     * 预览SurfaceView
     */
    private var mSurfacePreView: SurfaceView? = null  // ?标识变量可为空
    /**
     * 拍照按钮
     */
    private var mTakePictureView: ImageView? = null
    /**
     * 照片预览View
     */
    private var mShowPictureView: ImageView? = null
    /**
     * 摄像头旧API
     */
    private var mCamera: Camera? = null

    private var mSurfaceHolder: SurfaceHolder? = null

    /**
     * 手势监听器
     */
    private var mGestureDetector: GestureDetector? = null
    /**
     * 手势回调，实现匿名内部类 object:类或者接口
     */
    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            Log.d(TAG, "onGestureFling,返回拍摄")
            mShowPictureView?.visibility = View.GONE
            mSurfacePreView?.visibility = View.VISIBLE
            mTakePictureView?.visibility = View.VISIBLE
            return true
        }

        override fun onDown(e: MotionEvent?): Boolean {
            Log.d(TAG, "onGestureDown")
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            Log.d(TAG, "onGestureScroll")
            return true
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mSurfacePreView = findViewById(R.id.sv_camera_preview)
        mShowPictureView = findViewById(R.id.iv_camera_picture_taken)
        mTakePictureView = findViewById(R.id.iv_camera_take_picture)

        mTakePictureView?.setOnClickListener(this)   //?.标识符表示安全访问，防止空指针
        mSurfacePreView?.setOnClickListener(this)

        requestPermission()
        initGestureDetector()
        initSurface()
    }

    private fun initGestureDetector() {
        mGestureDetector = GestureDetector(this, mGestureListener)
        //高阶函数 & Lambda表达式，实现直接传递函数
        mShowPictureView?.setOnTouchListener { v, event ->
            mGestureDetector?.onTouchEvent(event)
            true
        }
        //上面等效于以下
        // 1.常规的匿名内部类写法
//        mShowPictureView?.setOnTouchListener(object:View.OnTouchListener{
//            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
//                mGestureDetector?.onTouchEvent(event)
//                return true
//            }
//        })
        //2.高阶函数写法，直接传递匿名函数实例
//        mShowPictureView?.setOnTouchListener(fun(v, event): Boolean {
//            mGestureDetector?.onTouchEvent(event)
//            return true
//        })
    }

    private fun initSurface() {
        mSurfaceHolder = mSurfacePreView?.holder
        mSurfaceHolder?.addCallback(this)
        mSurfaceHolder?.setFormat(PixelFormat.RGBA_8888)
    }


    private fun initCamera() {
        //打开后置摄像头(默认)：0是前置，1是后置，可以通过CameraInfo查询
        mCamera = Camera.open(1)
        val param: Camera.Parameters = mCamera!!.parameters  // !!.非空断言，强制转换为非空
        val supportedPreviewSize: List<android.hardware.Camera.Size> = param.supportedPreviewSizes
        //Kotlin for循环
        for (item in supportedPreviewSize) {
            Log.d(TAG, "支持的预览尺寸：${item.width},${item.height}")  //Kotlin字符串模板
        }
        val supportedPictureSize = param.supportedPictureSizes
        for (item in supportedPictureSize) {
            Log.d(TAG, "支持的照片尺寸：${item.width},${item.height}")
        }
        val previewSize = param.preferredPreviewSizeForVideo
        Log.d(TAG, "最佳预览尺寸：${previewSize.width},${previewSize.height}")
        val pictureSize = supportedPictureSize[0]
        Log.d(TAG, "设置的照片尺寸：${pictureSize.width},${pictureSize.height}")
        param.setPreviewSize(previewSize.width, previewSize.height)
        param.setPictureSize(pictureSize.width, pictureSize.height)
        param.setRotation(270)  //设置这个照片才能正常展示
        param.pictureFormat = ImageFormat.JPEG
        mCamera?.setDisplayOrientation(90)  //设置这个预览才正常展示
        mCamera?.parameters = param
        mCamera?.setPreviewDisplay(mSurfaceHolder)
        mCamera?.startPreview()
    }

    override fun onClick(v: View?) {
        if (v == mTakePictureView) {
            takePicture()
        } else if (v == mSurfacePreView) {
            //do nothing
        }
    }

    /**
     * 拍照
     */
    private fun takePicture() {
        mCamera?.takePicture(Camera.ShutterCallback {
            Log.d(TAG, "On Shutter")  //拍照瞬间回调
        }, null, Camera.PictureCallback { data, camera ->
            mSurfacePreView?.visibility = View.GONE
            mTakePictureView?.visibility = View.GONE
            mShowPictureView?.visibility = View.VISIBLE
            //从data数组中解压出Bitmap
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            mShowPictureView?.setImageBitmap(bitmap)
            val photo = File("/sdcard/DCIM/${System.currentTimeMillis()}.jpg")
            if (photo.exists()) {
                photo.delete()
            }
            photo.createNewFile()
            val fos = FileOutputStream(photo)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            Log.d(TAG, "照片已拍摄保存")
        })
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        Log.d(TAG, "surface created")
        initCamera()
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        Log.d(TAG, "surface changed width:$width ,height$height")

    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        Log.d(TAG, "surface destroyed")
        mCamera?.stopPreview()
        mCamera?.release()
    }

    /**
     * 动态权限申请
     */
    private fun requestPermission() {
        //生成数组：size：数组长度 默认初始化值""
        val requestPermissionArray = Array(2, { "" })
        var index = 0
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "当前没有拍照权限，申请权限")
            requestPermissionArray[index] = Manifest.permission.CAMERA
            index++
        } else {
            Log.d(TAG, "当前已经有拍照权限")
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "当前没有SD卡权限，申请权限")
            requestPermissionArray[index] = Manifest.permission.WRITE_EXTERNAL_STORAGE
        } else {
            Log.d(TAG, "当前已有SD卡权限")
        }
        if (!TextUtils.isEmpty(requestPermissionArray[0])) {
            Log.d(TAG, "真正开始权限申请")
            ActivityCompat.requestPermissions(this, requestPermissionArray, REQUEST_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "相机权限申请成功")
                } else {
                    Log.d(TAG, "相机权限申请不成功")
                }
                if (grantResults.size > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "SD卡权限申请成功")
                } else {
                    Log.d(TAG, "SD卡权限申请不成功")
                }
            }
            else -> return
        }
    }
}
