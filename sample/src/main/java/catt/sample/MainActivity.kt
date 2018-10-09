package catt.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log.e
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.include_content_layout.*
import kotlinx.android.synthetic.main.include_menu_layout.*

class MainActivity : AppCompatActivity() {
    private val _TAG: String = MainActivity::class.java.simpleName
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        menuBtn.setOnClickListener {
            e(_TAG, "##################    onClick: menuBtn")

        }
        contentBtn.setOnClickListener {
            e(_TAG, "@@@@@@@@@@@@@@@@@@    onClick: contentBtn")
        }
    }

    override fun onDestroy() {
        this.clearFindViewByIdCache()
        super.onDestroy()
    }
}
