package projecteLearning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import projecteLearning.dto.*;
import projecteLearning.exception.AdminProtectedException;
import projecteLearning.exception.EmailAlreadyExistsException;
import projecteLearning.exception.InvalidRoleAssignmentException;
import projecteLearning.exception.UserNotFoundException;
import projecteLearning.models.Role;
import projecteLearning.models.User;
import projecteLearning.repository.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Sans I/O/0/1 pour éviter toute confusion visuelle dans le mot de passe généré
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "!@#$%^&*-_+=";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;
    private static final int PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AdminUserResponse createUser(CreateUserByAdminRequest request) {

        if (request.getRole() == Role.ADMIN) {
            throw new InvalidRoleAssignmentException("Admin accounts cannot be created through this endpoint");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("This email is already registered: " + request.getEmail());
        }

        String plainPassword = generateStrongPassword();
        LocalDateTime now = LocalDateTime.now();

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(plainPassword))
                .phone(request.getPhone())
                .role(request.getRole())
                .enabled(true)
                .mustChangePassword(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        User saved = userRepository.save(user);

        emailService.sendAdminCreatedCredentials(saved.getEmail(), saved.getFirstName(), plainPassword, saved.getRole().name());

        return toAdminUserResponse(saved);
    }

    public PageResponse<AdminUserResponse> listStudents(int page, int size, String search) {
        return searchByRole(Role.ETUDIANT, page, size, search);
    }

    public PageResponse<AdminUserResponse> listTrainers(int page, int size, String search) {
        return searchByRole(Role.FORMATEUR, page, size, search);
    }

    public UserCountsResponse getUserCounts() {
        long students = userRepository.countByRole(Role.ETUDIANT);
        long trainers = userRepository.countByRole(Role.FORMATEUR);
        long admins = userRepository.countByRole(Role.ADMIN);
        long total = students + trainers + admins;
        return new UserCountsResponse(students, trainers, admins, total);
    }

    private PageResponse<AdminUserResponse> searchByRole(Role role, int page, int size, String search) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100); // borne entre 1 et 100

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        String escapedSearch = escapeRegex(search);

        Page<User> result = userRepository.searchByRole(role, escapedSearch, pageable);

        List<AdminUserResponse> content = result.getContent().stream()
                .map(this::toAdminUserResponse)
                .toList();

        return PageResponse.<AdminUserResponse>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    /** Échappe les caractères spéciaux regex avant de les injecter dans une requête MongoDB $regex. */
    private String escapeRegex(String input) {
        if (input == null) return "";
        return input.replaceAll("([.*+?^${}()|\\[\\]\\\\])", "\\\\$1");
    }

    public AdminUserResponse updateUser(String id, AdminUpdateUserRequest request) {

        User user = resolveManageableUser(id);

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getProfileImage() != null) user.setProfileImage(request.getProfileImage());

        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        return toAdminUserResponse(saved);
    }

    public void deleteUser(String id) {
        User user = resolveManageableUser(id);
        userRepository.delete(user);
    }

    /** Récupère l'utilisateur et vérifie qu'il n'est pas ADMIN — vérification commune à update/delete. */
    private User resolveManageableUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("No user found with this id"));

        if (user.getRole() == Role.ADMIN) {
            throw new AdminProtectedException("Admin accounts cannot be managed through this endpoint");
        }

        return user;
    }

    private String generateStrongPassword() {
        StringBuilder password = new StringBuilder();
        password.append(UPPER.charAt(SECURE_RANDOM.nextInt(UPPER.length())));
        password.append(LOWER.charAt(SECURE_RANDOM.nextInt(LOWER.length())));
        password.append(DIGITS.charAt(SECURE_RANDOM.nextInt(DIGITS.length())));
        password.append(SPECIAL.charAt(SECURE_RANDOM.nextInt(SPECIAL.length())));

        for (int i = password.length(); i < PASSWORD_LENGTH; i++) {
            password.append(ALL.charAt(SECURE_RANDOM.nextInt(ALL.length())));
        }

        List<Character> chars = new ArrayList<>();
        password.chars().forEach(c -> chars.add((char) c));
        Collections.shuffle(chars, SECURE_RANDOM);

        StringBuilder shuffled = new StringBuilder(chars.size());
        chars.forEach(shuffled::append);
        return shuffled.toString();
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .bio(user.getBio())
                .profileImage(user.getProfileImage())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}