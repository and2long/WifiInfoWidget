package tech.and2long.wifiinfowidget

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.text_title).text = "WiFi 信息小部件"
        findViewById<TextView>(R.id.text_description).text =
            "这个小部件可以显示当前 WiFi 的 IP 地址和网关信息。\n\n长按桌面空白处，选择\"小部件\"，然后找到\"WiFi 信息\"即可添加。"
    }
}