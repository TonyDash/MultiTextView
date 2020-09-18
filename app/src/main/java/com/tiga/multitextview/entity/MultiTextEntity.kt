package com.tiga.multitextview.entity

class MultiTextEntity(
    var content: String,
    var topicList: List<Topic>? = null,
    var atList: List<AtUserModel>? = null,
    var isShowAll: Boolean = false
)