package com.example.scapp_scouting

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*
import javax.annotation.Nullable
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class CreateMarker : AppCompatActivity() {


    var filePath: Uri? = null
    var GALLERY_REQ_CODE = 1
    var storage: FirebaseStorage? = FirebaseStorage.getInstance();
    var storageReference: StorageReference? = storage?.getReference();
    private val ImageList = ArrayList<Uri>()
    var imgnames: MutableList<String> = ArrayList()

    //lateinit var ImgView: ImageView

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_marker);

        //ImgView = findViewById(R.id.imageView) as ImageView
        val imgButton= findViewById(R.id.ImgButton) as Button
        imgButton?.setOnClickListener {

            SelectImage()

        }
        //val btn_click_me = findViewById(R.id.popup_window_button) as Button
        // set on-click listener
        val uploadButton= findViewById(R.id.popup_window_button) as Button
        uploadButton?.setOnClickListener {

            upload()
        }
    }


    private fun SelectImage() {
        // Defining Implicit Intent to mobile gallery
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, GALLERY_REQ_CODE)
    }
    @Override
    protected override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
                if (data != null) {
                    if (data.clipData != null) {
                        //ImgView.setImageURI(data.data)
                        //filePath = data.data
                        val count: Int? = data.clipData?.itemCount

                        var CurrentImageSelect = 0

                        while (CurrentImageSelect < count!!) {
                            val imageuri: Uri? = data.clipData?.getItemAt(CurrentImageSelect)?.getUri()
                            if (imageuri != null) {
                                ImageList.add(imageuri)
                            }

                            CurrentImageSelect = CurrentImageSelect + 1
                        }
                        //textView.setVisibility(View.VISIBLE)
                        //textView.setText("You Have Selected " + ImageList.size + " Pictures")
                        //choose.setVisibility(View.GONE)


                        /*try {
                            val bitmap = MediaStore.Images.Media
                                .getBitmap(
                                    contentResolver,
                                    filePath
                                )
                            //val ImgView = findViewById(R.id.imageView) as ImageView
                            //ImgView!!.setImageBitmap(bitmap)
                        }catch (ioe: IOException){
                            println(ioe.message)
                        }*/

                    }

        }
    }
    fun upload(){
        //add referencees
        val intent = getIntent()
        val Post: MutableMap<String, Any> = HashMap()

        val ImageFolder = FirebaseStorage.getInstance().reference.child("ImageFolder")

            var uploads = 0
            while (uploads < ImageList.size) {
                val Image = ImageList[uploads]
                val uuid = UUID.randomUUID().toString()
                imgnames.add("gs://scapp-scouting.appspot.com/images/" + uuid)
                val imagename: StorageReference =
                    ImageFolder.child("image/" + uuid)
                imagename.putFile(ImageList[uploads])

                uploads++
            }









        /*if (filePath != null){
            // Defining the child of storageReference
            val uuid = UUID.randomUUID().toString()

            val ref = storageReference?.child(

                "images/" + uuid
            )
            if (ref != null) {
                Post.put("Img", "gs://scapp-scouting.appspot.com/images/" + uuid)
            };

            if (ref != null) {
                ref.putFile(filePath!!)
            }

        }*/



        Post.put("Img", imgnames)


        //val img = intent1
        //Post.put("Img", img)

        val latetude = intent.getStringExtra("latetude")
        val longetude = intent.getStringExtra("longetude")
        val latlong = latetude+","+longetude;
        val position = latlong.split(",").toTypedArray()
        val latitude = position[0].toDouble()
        val longitude = position[1].toDouble()
        val location = LatLng(latitude, longitude)
        Post.put("Coordinates", location);


        var text   = findViewById<EditText>(R.id.popup_window_text);
        val description = text.text.toString()
        Post.put("Description", description);





        text   = findViewById<EditText>(R.id.popup_window_title);
        val title = text.text.toString()
        Post.put("Title", title)
        Post.put("UserId", 2);

        val db: FirebaseFirestore = FirebaseFirestore.getInstance()

        db.collection("Posts")
            .add(Post)
            .addOnSuccessListener { documentReference -> }
            .addOnFailureListener { e -> Log.w( "Error adding document", e) }
        finish()
    }

}



