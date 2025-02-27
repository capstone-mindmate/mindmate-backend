package com.mindmate.mindmate_server.global.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class FCMService {
//    private final FirebaseMessaging firebaseMessaging;
//
//    public void sendNotification(String token, String title, String body, Map<String, String> data) {
//        try {
//            Message message = Message.builder()
//                    .setToken(token)
//                    .setNotification(Notification.builder()
//                            .setTitle(title)
//                            .setBody(body)
//                            .build())
//                    .putAllData(data)
//                    .build();
//
//            String response = firebaseMessaging.send(message);
//            log.info("FCM notification sent successfully: {}", response);
//        } catch (FirebaseMessagingException e) {
//            log.error("Failed to send FCM notification", e);
//        }
//    }
//}
