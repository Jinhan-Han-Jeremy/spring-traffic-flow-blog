package com.jinhan.TrafficBlog.service;

import com.jinhan.TrafficBlog.dto.SignUpUser;
import com.jinhan.TrafficBlog.dto.WriteDeviceDto;
import com.jinhan.TrafficBlog.entity.Device;
import com.jinhan.TrafficBlog.entity.User;
import com.jinhan.TrafficBlog.repository.jpa.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(SignUpUser signUpUser) {
        User user = new User();
        user.setUsername(signUpUser.getUsername());
        user.setPassword(passwordEncoder.encode(signUpUser.getPassword()));
        user.setEmail(signUpUser.getEmail());
        return userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }

    public List<Device> getDevices() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Optional<User> user = userRepository.findByUsername(userDetails.getUsername());
        if (user.isPresent()) {
            return user.get().getDeviceList();
        } else {
            return new ArrayList<>();
        }
    }

    public Device addDevice(WriteDeviceDto writeDeviceDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Optional<User> user = userRepository.findByUsername(userDetails.getUsername());
        if (user.isPresent()) {
            Device device = new Device();
            device.setDeviceName(writeDeviceDto.getDeviceName());
            device.setToken(writeDeviceDto.getToken());
            user.get().getDeviceList().add(device);
            userRepository.save(user.get());
            return device;
        }
        return null;
    }
}