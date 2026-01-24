package com.example.fastfood.controller;

import com.example.fastfood.entity.Contact;
import com.example.fastfood.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
@CrossOrigin(origins = "*") // Cho phép React gọi API
public class ContactController {

    @Autowired
    private ContactRepository contactRepository;

    // API nhận tin nhắn từ khách hàng
    @PostMapping
    public Contact createContact(@RequestBody Contact contact) {
        return contactRepository.save(contact);
    }

    // API để Admin xem danh sách liên hệ (nếu cần sau này)
    @GetMapping
    public List<Contact> getAllContacts() {
        return contactRepository.findAll();
    }
}