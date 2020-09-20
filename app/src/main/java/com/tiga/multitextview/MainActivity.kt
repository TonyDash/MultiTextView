package com.tiga.multitextview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.tiga.multitextview.entity.AtUserModel
import com.tiga.multitextview.entity.MultiTextEntity
import com.tiga.multitextview.entity.Topic
import com.tiga.multitextview.widget.MultiTextView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initData()
    }

    private fun initData() {
        val contentText = "奥特曼平成三杰@TIGA @DYNA @GAIA，谢谢你，泰罗。#TIGA# #DYNA# #GAIA# " +
                "我要填满三行我要填满三行我要填满三行我要填满三行我要填满三行我要填满三行我要填满三行我要填满三行" +
                "我要填满三行我要填满三行我要填满三行我要填满三行我要填满三行我要填满三行我要填满三行我要填满三行" +
                "我要填满三行我要填满三行我要填满三行我要填满三行我要填满三行我要填满三行我要填满三行我要填满三行"
        val topicList = mutableListOf(
            Topic(1, "TIGA", 1),
            Topic(2, "DYNA", 2),
            Topic(3, "GAIA", 3)
        )
        val atList = mutableListOf(
            AtUserModel(1, "@TIGA"),
            AtUserModel(2, "@DYNA"),
            AtUserModel(3, "@GAIA")
        )
        val multiTextEntity = MultiTextEntity(contentText, topicList, atList, false)
        mtvContent.setText(
            contentText,
            topicList,
            atList,
            multiTextEntity.isShowAll,
            object : MultiTextView.Callback {
                override fun onExpand() {
                    tvShowAll.visibility = View.VISIBLE
                    tvShowAll.text = resources.getText(R.string.hideAllText)
                }

                override fun onCollapse() {
                    tvShowAll.visibility = View.VISIBLE
                    tvShowAll.text = resources.getText(R.string.showAllText)
                }

                override fun onLoss() {
                    showToast("")
                    tvShowAll.visibility = View.GONE
                }

            })
        tvShowAll.setOnClickListener {
            multiTextEntity.isShowAll = !multiTextEntity.isShowAll
            mtvContent.setChanged(multiTextEntity.isShowAll)
        }
        mtvContent.onTextClickListener = object :MultiTextView.OnTextClickListener{
            override fun onTopicClick(topic: Topic) {
                showToast("topic title is ${topic.title}")
            }

            override fun onAtClick(name: String, id: Long) {
                showToast("name is $name")
            }

        }
    }

    fun showToast(tips: String) {
        Toast.makeText(this, tips, Toast.LENGTH_SHORT).show()
    }
}