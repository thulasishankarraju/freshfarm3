package com.example.freshfarm3.service;

import com.example.freshfarm3.dto.request.AgentLoginRequest;
import com.example.freshfarm3.dto.request.AgentRegisterRequest;
import com.example.freshfarm3.dto.request.LoginRequest;
import com.example.freshfarm3.dto.request.RegisterRequest;
import com.example.freshfarm3.dto.request.ResetPasswordRequest;
import com.example.freshfarm3.dto.request.SendOtpRequest;
import com.example.freshfarm3.dto.response.AuthResponse;
import com.example.freshfarm3.entity.Buyer;
import com.example.freshfarm3.entity.DeliveryAgent;
import com.example.freshfarm3.entity.Farmer;
import com.example.freshfarm3.entity.User;
import com.example.freshfarm3.enums.Role;
import com.example.freshfarm3.repository.BuyerRepository;
import com.example.freshfarm3.repository.DeliveryAgentRepository;
import com.example.freshfarm3.repository.FarmerRepository;
import com.example.freshfarm3.repository.UserRepository;
import com.example.freshfarm3.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository          userRepository;
    private final FarmerRepository        farmerRepository;
    private final BuyerRepository         buyerRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final PasswordEncoder         passwordEncoder;
    private final JwtUtil                 jwtUtil;
    private final OtpService              otpService;

    // ── BUYER REGISTER ──────────────────────────────────────────
    @Transactional
    public AuthResponse registerBuyer(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already registered: " + req.getEmail());
        }
        requireRegistrationOtpVerified(req.getEmail(), req.getPhone());
        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(Role.BUYER)
                .build();
        User saved = userRepository.save(user);

        Buyer buyer = new Buyer();
        buyer.setUser(saved);
        buyerRepository.save(buyer);

        String token = jwtUtil.generateToken(saved.getEmail(), saved.getRole().name());
        log.info("Buyer registered: {}", saved.getEmail());
        return buildAuthResponse(token, saved);
    }

    // ── FARMER REGISTER (maps farmer-specific fields) ───────────
    @Transactional
    public AuthResponse registerFarmer(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already registered: " + req.getEmail());
        }
        requireRegistrationOtpVerified(req.getEmail(), req.getPhone());
        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(Role.FARMER)
                .build();
        User saved = userRepository.save(user);

        Farmer farmer = new Farmer();
        farmer.setUser(saved);
        farmer.setFarmName(req.getFarmName());
        farmer.setVillage(req.getVillage());
        farmer.setDistrict(req.getDistrict());
        farmer.setState(req.getState());
        farmer.setPincode(req.getPincode());
        farmer.setAadhaarNumber(req.getAadhaarNumber());
        farmer.setBankAccountNumber(req.getBankAccountNumber());
        farmer.setIfscCode(req.getIfscCode());
        farmer.setBio(req.getBio());
        farmerRepository.save(farmer);

        String token = jwtUtil.generateToken(saved.getEmail(), saved.getRole().name());
        log.info("Farmer registered: {}", saved.getEmail());
        return buildAuthResponse(token, saved);
    }

    // ── BUYER / FARMER / ADMIN LOGIN ─────────────────────────────
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        log.info("User logged in: {} as {}", user.getEmail(), user.getRole());
        return buildAuthResponse(token, user);
    }

    // ── AGENT REGISTER ───────────────────────────────────────────
    @Transactional
    public AuthResponse registerAgent(AgentRegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already registered: " + req.getEmail());
        }
        requireRegistrationOtpVerified(req.getEmail(), req.getPhone());

        User user = User.builder()
                .fullName(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(Role.AGENT)
                .build();
        User saved = userRepository.save(user);

        DeliveryAgent agent = DeliveryAgent.builder()
                .user(saved)
                .vehicleNumber(req.getVehicleNumber())
                .vehicleType(req.getVehicleType())
                .phone(req.getPhone())
                .isAvailable(true)
                .build();
        deliveryAgentRepository.save(agent);

        String token = jwtUtil.generateToken(saved.getEmail(), saved.getRole().name());
        log.info("Delivery agent registered: {}", saved.getEmail());
        return buildAuthResponse(token, saved);
    }

    // ── AGENT LOGIN ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public AuthResponse loginAgent(AgentLoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (user.getRole() != Role.AGENT) {
            throw new RuntimeException("This account is not registered as a delivery agent");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        log.info("Agent logged in: {}", user.getEmail());
        return buildAuthResponse(token, user);
    }

    // ── REGISTRATION OTP ─────────────────────────────────────────
    // Step 1: send an OTP to an email/phone that isn't registered yet.
    public void initiateRegistrationOtp(SendOtpRequest req) {
        boolean alreadyExists = req.getChannel().equalsIgnoreCase("EMAIL")
                ? userRepository.existsByEmail(req.getRecipient())
                : userRepository.existsByPhone(toStoredPhone(req.getRecipient()));
        if (alreadyExists) {
            throw new RuntimeException("An account already exists for " + req.getRecipient());
        }
        otpService.sendOtp(req.getRecipient(), req.getChannel(), "REGISTRATION", req.getRole());
    }

    // Step 2 (verification itself) happens directly via OtpService.verifyOtp,
    // called from AuthController — no extra business rule needed there.

    // Guard used by registerBuyer/registerFarmer/registerAgent: registration
    // is only allowed once the OTP flow above has been completed for either
    // the email or the phone number given at registration.
    //
    // NOTE: OtpService requires phone recipients in "+91XXXXXXXXXX" format
    // (it validates/sends via Twilio that way), but User.phone and every
    // RegisterRequest.phone in this app store/validate the bare 10-digit
    // form ("9876543210"). So the OTP row's `recipient` was saved as
    // "+91XXXXXXXXXX" while `phone` here is the bare form — we must check
    // both forms or phone-based verification would never match.
    private void requireRegistrationOtpVerified(String email, String phone) {
        boolean verified = otpService.isOtpVerified(email, "REGISTRATION")
                || (phone != null && otpService.isOtpVerified(toIndiaOtpFormat(phone), "REGISTRATION"))
                || (phone != null && otpService.isOtpVerified(phone, "REGISTRATION"));
        if (!verified) {
            throw new RuntimeException(
                    "Please verify your email or phone with the OTP sent to you before registering.");
        }
    }

    // ── FORGOT PASSWORD ───────────────────────────────────────────
    // Step 1: send an OTP to an email/phone that IS already registered.
    public void initiatePasswordResetOtp(SendOtpRequest req) {
        User user = req.getChannel().equalsIgnoreCase("EMAIL")
                ? userRepository.findByEmail(req.getRecipient())
                .orElseThrow(() -> new RuntimeException("No account found for " + req.getRecipient()))
                : userRepository.findByPhone(toStoredPhone(req.getRecipient()))
                .orElseThrow(() -> new RuntimeException("No account found for " + req.getRecipient()));
        otpService.sendOtp(req.getRecipient(), req.getChannel(), "FORGOT_PASSWORD", user.getRole().name());
    }

    // Step 2: verify the OTP and set the new password in one call.
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        otpService.verifyOtp(req.getRecipient(), req.getOtpCode(), "FORGOT_PASSWORD");

        User user = userRepository.findByEmail(req.getRecipient())
                .or(() -> userRepository.findByPhone(toStoredPhone(req.getRecipient())))
                .orElseThrow(() -> new RuntimeException("No account found for " + req.getRecipient()));

        if (!user.getRole().name().equalsIgnoreCase(req.getRole())) {
            throw new RuntimeException("Role does not match the account found for " + req.getRecipient());
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        log.info("Password reset for {}", req.getRecipient());
    }

    // "+919876543210" → "9876543210" (strips a leading +91, leaves email/other input untouched)
    private String toStoredPhone(String recipient) {
        return recipient.startsWith("+91") ? recipient.substring(3) : recipient;
    }

    // "9876543210" → "+919876543210" (only used to also check the OTP-service's required format)
    private String toIndiaOtpFormat(String phone) {
        return phone.startsWith("+") ? phone : "+91" + phone;
    }

    // ── shared response builder ──────────────────────────────────
    // Replaces the old `new AuthResponse(token, role, fullName, id)` calls,
    // which silently produced an all-null response because AuthResponse's
    // matching 4-arg constructor had an empty body.
    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .expiresIn(jwtUtil.getExpiration())
                .build();
    }
}
