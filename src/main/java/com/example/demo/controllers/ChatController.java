package com.example.demo.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.ChatService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> request,
                                   HttpSession session) {

        String message = request.get("message");

        // ===== HISTORY (GIỮ LẠI CHO ĐẸP) =====
        List<String> history = (List<String>) session.getAttribute("chatHistory");
        if (history == null) {
            history = new ArrayList<>();
        }

        // 🔥 GỌI AI FAKE (CHỈ 1 PARAM)
        String reply = chatService.chat(message);

        // ===== LƯU HISTORY =====
        history.add("USER: " + message);
        history.add("AI: " + reply);
        session.setAttribute("chatHistory", history);

        // ===== RETURN =====
        Map<String, String> res = new HashMap<>();
        res.put("reply", reply);

        return res;
    }
}