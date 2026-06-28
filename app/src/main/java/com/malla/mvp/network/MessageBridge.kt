package com.malla.mvp.network

import android.content.Context
import com.malla.mvp.core.data.IConversationRepository
import com.malla.mvp.core.data.IMessageRepository
import com.malla.mvp.core.network.INetworkService
import com.malla.mvp.core.notification.INotificationHelper
import com.malla.mvp.core.util.ILogger

class MessageBridge(
    private val context: Context,
    private val networkService: INetworkService,
    private val messageRepository: IMessageRepository,
    private val conversationRepository: IConversationRepository,
    private val logger: ILogger,
    private val notificationHelper: INotificationHelper
) {
    fun start() {}
    fun stop() {}
}
