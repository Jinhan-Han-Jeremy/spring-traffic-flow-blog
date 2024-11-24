package com.jinhan.TrafficBlog.controller;


import com.jinhan.TrafficBlog.dto.WriteDeviceDto;
import com.jinhan.TrafficBlog.entity.Device;
import com.jinhan.TrafficBlog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final UserService userService;

    @Autowired
    public DeviceController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("")
    public ResponseEntity<List<Device>> getDevices() {
        return ResponseEntity.ok(userService.getDevices());
    }

    @PostMapping("")
    public ResponseEntity<Device> addDevice(@RequestBody WriteDeviceDto writeDeviceDto) {
        return ResponseEntity.ok(userService.addDevice(writeDeviceDto));
    }
}