package org.tensorflow.lite.examples.detection.facerecognition

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.kbyai.facesdk.FaceBox
import com.kbyai.facesdk.FaceDetectionParam
import com.kbyai.facesdk.FaceSDK
import org.tensorflow.lite.examples.detection.R
import java.lang.Exception
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    companion object {
        private val SELECT_PHOTO_REQUEST_CODE = 1
    }

    private lateinit var dbManager: DBManager
    private lateinit var textWarning: TextView
    private lateinit var personAdapter: PersonAdapter

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main12)

        textWarning = findViewById<TextView>(R.id.textWarning)

        var ret = FaceSDK.setActivation(
            " BsTr9o4f4R/rM3TxbCWVb/hrOJuOIdz8ArQ/t2IgQFFUQzGHOLNNaMJiK/fUfr5zo005zoTA/cm6 \n" +
                    " VoZ6iGl+/hZGA3R5T/VWwhxekbw8JVz9sNesU6rMG5+1cNSN75trH2tpzdCPZ28ZDnZlttmiuUoC \n" +
                    " 9QazRe1xKi5tUXa+xgIxzL0vE6UW2dLKWaEXjn3fSJfLxXWw0q+UZP0hQAXb5Y9Yl/NVi7y3d0xT \n" +
                    " Vq6/weuMQkgLcNdLqFRvQXup0M9W/pvuhaubySAxHCKVY8wToygN2iM78cOkyyAbGVwZeGQP0Jfd \n" +
                    " 46VZo+w+KCNw355j3osVVMghrOcVZnfbp1dNyg== \n"
        )

        if (ret == FaceSDK.SDK_SUCCESS) {
            ret = FaceSDK.init(assets)
        }

        if (ret != FaceSDK.SDK_SUCCESS) {
            textWarning.visibility = View.VISIBLE
            if (ret == FaceSDK.SDK_LICENSE_KEY_ERROR) {
                textWarning.setText("Invalid license!")
            } else if (ret == FaceSDK.SDK_LICENSE_APPID_ERROR) {
                textWarning.setText("Invalid error!")
            } else if (ret == FaceSDK.SDK_LICENSE_EXPIRED) {
                textWarning.setText("License expired!")
            } else if (ret == FaceSDK.SDK_NO_ACTIVATED) {
                textWarning.setText("No activated!")
            } else if (ret == FaceSDK.SDK_INIT_ERROR) {
                textWarning.setText("Init error!")
            }
        }

        dbManager = DBManager(this)
        dbManager.loadPerson()

        personAdapter = PersonAdapter(this, DBManager.personList)
        val listView: ListView = findViewById<View>(R.id.listPerson) as ListView
        listView.setAdapter(personAdapter)

        findViewById<Button>(R.id.buttonEnroll).setOnClickListener {
            val intent = Intent()
            intent.setType("image/*")
            intent.setAction(Intent.ACTION_PICK)
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), SELECT_PHOTO_REQUEST_CODE)

        }

        findViewById<Button>(R.id.buttonIdentify).setOnClickListener {
            startActivity(Intent(this, CameraActivityKt::class.java))
        }

        findViewById<Button>(R.id.buttonSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.buttonAbout).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        personAdapter.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                var bitmap: Bitmap = Utils.getCorrectlyOrientedImage(this, data?.data!!)

                val faceDetectionParam = FaceDetectionParam()
                faceDetectionParam.check_liveness = true
                faceDetectionParam.check_liveness_level = SettingsActivity.getLivenessLevel(this)
                var faceBoxes: List<FaceBox>? = FaceSDK.faceDetection(bitmap, faceDetectionParam)

                if(faceBoxes.isNullOrEmpty()) {
                    Toast.makeText(this, getString(R.string.no_face_detected), Toast.LENGTH_SHORT).show()
                } else if (faceBoxes.size > 1) {
                    Toast.makeText(this, getString(R.string.multiple_face_detected), Toast.LENGTH_SHORT).show()
                } else {
                    val faceImage = Utils.cropFace(bitmap, faceBoxes[0])
                    val templates = FaceSDK.templateExtraction(bitmap, faceBoxes[0])

                    dbManager.insertPerson("Person" + Random.nextInt(10000, 20000), faceImage, templates)
                    personAdapter.notifyDataSetChanged()
                    Toast.makeText(this, getString(R.string.person_enrolled), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                //handle exception
                e.printStackTrace()
            }
        }
    }
}